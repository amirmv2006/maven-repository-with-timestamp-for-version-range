package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IJenkinsApi;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Amir
 */
@Component
public class JenkinsServerApi
        implements IJenkinsApi {

    public static final String API_JSON = "/api/json?pretty=true";
    @Value("${jenkins.server.master.job.address}")
    private String serverAddress;

    @Value("${jenkins.server.username}")
    private String username;

    @Value("${jenkins.server.password}")
    private String password;

    public HttpResponse executeHttpGetRequest(HttpGet req) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
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
        return httpClient.execute(req, context);
    }

    @Override
    public Long getLastBuild() throws IOException {
        String getUrl = serverAddress + API_JSON;
        HttpGet get = new HttpGet(getUrl);
        HttpResponse response = executeHttpGetRequest(get);
        HttpEntity entity = response.getEntity();
        try (InputStream content = entity.getContent()){
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode lastBuild = jsonNode.get("lastBuild");
            JsonNode number = lastBuild.get("number");
            return number.asLong();
        }
    }

    @Override
    public List<String> getBuildCommitIds(Long buildNo) throws IOException {
        List<String> result = new ArrayList<>();

        String getUrl = serverAddress + "/" + buildNo + API_JSON;
        HttpGet get = new HttpGet(getUrl);
        HttpResponse response = executeHttpGetRequest(get);
        HttpEntity entity = response.getEntity();
        try (InputStream content = entity.getContent()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode changeSets = jsonNode.get("changeSets");
            for (JsonNode changeSet : changeSets) {
                JsonNode items = changeSet.get("items");
                for (JsonNode gitCommit : items) {
                    result.add(gitCommit.get("commitId").asText());
                }
            }
            return result;
        }
    }

    @Override
    public Date getBuildFinishDate(Long buildNo) throws IOException {
        String getUrl = serverAddress + "/" + buildNo + API_JSON;
        HttpGet get = new HttpGet(getUrl);
        HttpResponse response = executeHttpGetRequest(get);
        HttpEntity entity = response.getEntity();
        try (InputStream content = entity.getContent()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode startTimestamp = jsonNode.get("timestamp");
            JsonNode duration = jsonNode.get("duration");
            return new Date(startTimestamp.asLong() + duration.asLong());
        }
    }
}
