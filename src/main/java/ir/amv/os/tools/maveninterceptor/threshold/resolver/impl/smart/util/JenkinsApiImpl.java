package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IJenkinsApi;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Amir
 */
@Component
@Profile("smartThreshold")
public class JenkinsApiImpl
        implements IJenkinsApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsApiImpl.class);
    private static final long MAX_BUILD_COUNT_CHECK = 500;
    private static final String API_JSON = "/api/json?pretty=true";

    @Value("${jenkins.server.master.job.address}")
    private String serverAddress;

    @Value("${jenkins.server.username}")
    private String username;

    @Value("${jenkins.server.password}")
    private String password;

    @Value("${jenkins.server.cache.build.count}")
    private Integer cacheBuildCount;

    @Autowired
    private IJenkinsApi selfProxiedInstance;

    public InputStream executeHttpGetRequest(final String getUrl, final AtomicReference<Closeable> closeableReference) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        closeableReference.set(httpClient);
        // Then provide the right credentials
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password));

        // Generate BASIC scheme object and stick it to the execution context
        BasicScheme basicAuth = new BasicScheme();
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", basicAuth);

        // Add as the first (because of the zero) request interceptor
        // It will first intercept the request and preemptively initialize the authentication scheme if there is not
        httpClient.addRequestInterceptor(new PreemptiveAuth(), 0);
        CloseableHttpResponse execute = httpClient.execute(new HttpGet(getUrl), context);
        HttpEntity entity = execute.getEntity();
        return entity.getContent();
    }

    // need to wait for cache to be initialized:
    // org.springframework.cache.interceptor.CacheAspectSupport.afterSingletonsInstantiated() will be called
    // after PostConstructs...
    @Scheduled(initialDelay = 5_000, fixedRate = 4 * 60 * 60_000)
    public void cacheLastBuilds() throws IOException {
        Long lastBuild = getLastBuild();
        Long targetBuild = lastBuild - cacheBuildCount;
        while (lastBuild > targetBuild) {
            JenkinsBuildResult buildStats = selfProxiedInstance.getBuildStats(lastBuild);
            LOGGER.info("Build Stats: Build No {}, stats: {}", lastBuild, buildStats);
            if (buildStats.isBuildSuccessful()) {
                Date buildFinishDate = selfProxiedInstance.getBuildFinishDate(lastBuild);
                LOGGER.info("Build no {}, finish date {}", lastBuild, buildFinishDate);
            }
            lastBuild--;
        }
    }

    @Override
    public Long getLastBuild() throws IOException {
        String getUrl = getJenkinsUrlFor(null, "lastBuild[number]");
        AtomicReference<Closeable> closeableReference = new AtomicReference<>();
        try (InputStream content = executeHttpGetRequest(getUrl, closeableReference)) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode lastBuild = jsonNode.get("lastBuild");
            JsonNode number = lastBuild.get("number");
            return number.asLong();
        } finally {
            Closeable closeable = closeableReference.get();
            if (closeable != null) {
                closeable.close();
            }
        }
    }

    @Override
    @Cacheable(value = "buildCommits", key = "#buildNo")
    public JenkinsBuildResult getBuildStats(Long buildNo) throws IOException {
        LOGGER.info("Getting Build Stats for {}", buildNo);
        List<String> result = new ArrayList<>();
        String getUrl = getJenkinsUrlFor(buildNo, "changeSets[items[commitId]]", "result");
        AtomicReference<Closeable> closeableReference = new AtomicReference<>();
        try (InputStream content = executeHttpGetRequest(getUrl, closeableReference)) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode changeSets = jsonNode.get("changeSets");
            for (JsonNode changeSet : changeSets) {
                JsonNode items = changeSet.get("items");
                for (JsonNode gitCommit : items) {
                    result.add(gitCommit.get("commitId").asText());
                }
            }
            boolean buildSuccessful = jsonNode.get("result").asText().equals("SUCCESS");
            return new JenkinsBuildResult(buildSuccessful, result);
        } finally {
            Closeable closeable = closeableReference.get();
            if (closeable != null) {
                closeable.close();
            }
        }
    }

    private String getJenkinsUrlFor(final Long buildNo, String... neededObjects) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverAddress);
        if (buildNo != null) {
            sb.append("/");
            sb.append(buildNo);
        }
        sb.append(API_JSON);
        if (neededObjects != null) {
            sb.append("&tree=");
            for (int i = 0; i < neededObjects.length; i++) {
                sb.append(neededObjects[i]);
                if (i < neededObjects.length - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }

    @Override
    @Cacheable(value = "buildFinishDate", key = "#buildNo")
    public Date getBuildFinishDate(Long buildNo) throws IOException {
        LOGGER.info("Getting build finish date for {}", buildNo);
        String getUrl = getJenkinsUrlFor(buildNo, "timestamp", "duration");
        AtomicReference<Closeable> closeableReference = new AtomicReference<>();
        try (InputStream content = executeHttpGetRequest(getUrl, closeableReference)) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode startTimestamp = jsonNode.get("timestamp");
            JsonNode duration = jsonNode.get("duration");
            return new Date(startTimestamp.asLong() + duration.asLong());
        } finally {
            Closeable closeable = closeableReference.get();
            if (closeable != null) {
                closeable.close();
            }
        }
    }

    @Override
    @Cacheable("commitBuildFinishDate")
    public Date findSuccessfulBuildFinishDateForCommitId(List<String> commits) throws IOException {
        Long lastBuild = getLastBuild();
        Long buildCounter = lastBuild;
        Long lastSuccessfulBuild = null;
        while (lastBuild - buildCounter < MAX_BUILD_COUNT_CHECK) {
            JenkinsBuildResult buildStats = selfProxiedInstance.getBuildStats(buildCounter);
            if (buildStats.isBuildSuccessful()) {
                lastSuccessfulBuild = buildCounter;
            }
            for (String commit : commits) {
                if (buildStats.getCommitIds().contains(commit)) {
                    return selfProxiedInstance.getBuildFinishDate(lastSuccessfulBuild);
                }
            }
            buildCounter--;
        }
        return null;
    }
}
