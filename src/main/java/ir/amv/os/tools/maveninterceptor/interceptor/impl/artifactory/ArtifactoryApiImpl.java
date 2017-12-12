package ir.amv.os.tools.maveninterceptor.interceptor.impl.artifactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util.PreemptiveAuth;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Amir
 */
@Component
public class ArtifactoryApiImpl {

    @Value("${amir.remote.maven}")
    private String artifactoryServerSaasUrl;

    @PostConstruct
    public void test() throws IOException, ParseException {
        Date releaseDateFor = getReleaseDateFor("");
        System.out.println("releaseDateFor = " + releaseDateFor);
    }

    public HttpResponse executeHttpGetRequest(HttpGet req) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        return httpClient.execute(req);
    }

    public Date getReleaseDateFor(String artifactPath) throws IOException, ParseException {
        HttpResponse response = executeHttpGetRequest(new HttpGet(getRestBaseUrl() + artifactPath));
        HttpEntity entity = response.getEntity();
        try (InputStream content = entity.getContent()){
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.reader().readTree(content);
            JsonNode created = jsonNode.get("created");
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(created.asText());
        }
    }

    private String getRestBaseUrl() {
        return artifactoryServerSaasUrl.replaceFirst("/saas", "/artifactory/api/storage/saas");
    }

    public static void main(String[] args) {
        Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl("").build();
        ItemHandle saas = artifactory.repository("saas").folder("");
        Item info = saas.info();
        Map<String, Date> releaseMap = new HashMap<>();
        if (info instanceof File) {
            File file = (File) info;
            System.out.println("file.getCreated() = " + file.getCreated());
        } else if (info instanceof Folder) {
            Folder folder = (Folder) info;
            System.out.println("folder.getCreated() = " + folder.getCreated());
            List<Item> children = folder.getChildren();
            for (Item child : children) {
                if (child.getName().matches("\\d.*")) {
                    Folder childFolder = artifactory.repository("saas").folder
                            ("" + child.getUri()).info();
                    List<Item> grandChildren = childFolder.getChildren();
                    for (Item grandChild : grandChildren) {
                        if (grandChild.getName().endsWith("pom")) {
                            File grandChildFile = artifactory.repository("saas").file
                                    ("" + child.getUri() + grandChild.getUri()).info();
                            releaseMap.put(childFolder.getName(), grandChildFile.getCreated());
                        }
                    }
                }
            }
        }
        releaseMap.entrySet().forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
    }
}
