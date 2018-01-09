package ir.amv.os.tools.maveninterceptor.interceptor.impl.artifactory;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Amir
 */
@Component
public class ArtifactoryApiImpl implements IArtifactoryApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryApiImpl.class);

    @Value("${artifactory.base.path}")
    private String artifactoryBasePath;
    @Value("${artifactory.release.repository}")
    private String artifactoryReleaseRepository;
    @Value("${artifactory.download.repository}")
    private String artifactoryDownloadRepository;
    @Value("${artifactory.release.date.check.createDate}")
    private Boolean checkCreateDate;

    @Autowired
    private IArtifactoryApi selfProxiedInstance;

    @Override
    public Map<String, Date> getReleaseDatesFor(String artifactPath) throws IOException, ParseException {
        if (checkCreateDate) {
            Artifactory artifactory = ArtifactoryClientBuilder.create()
                    .setUrl(artifactoryBasePath).build();
            RepositoryHandle releaseRepo = artifactory.repository(artifactoryReleaseRepository);
            ItemHandle saas = releaseRepo.folder(artifactPath);
            Item info = saas.info();
            Map<String, Date> releaseMap = new HashMap<>();
            if (info instanceof Folder) {
                Folder folder = (Folder) info;
                List<Item> children = folder.getChildren();
                for (Item child : children) {
                    Map<String, Date> childReleaseDateMap = selfProxiedInstance.getChildReleaseDateMap(
                            child.getUri(), artifactPath, releaseRepo, child);
                    releaseMap.putAll(childReleaseDateMap);
                }
            }
            return releaseMap;
        } else {
            URLConnection connection = new URL(artifactoryBasePath + "/" + artifactoryReleaseRepository + "/" +
                    artifactPath).openConnection();
            InputStream inputStream = connection.getInputStream();
            return parseVersionsHtml(inputStream);
        }
    }

    private Map<String, Date> parseVersionsHtml(InputStream is) throws IOException, ParseException {
        Map<String, Date> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<a href=")) {
                    StringTokenizer tokenizer = new StringTokenizer(line, " ");
                    if (tokenizer.countTokens() == 5) {
                        tokenizer.nextToken(); // <a
                        String href = tokenizer.nextToken();// href=...

                        int start = href.substring(0, href.length() - 1).lastIndexOf(">");
                        int end = href.lastIndexOf("<");
                        String version = href.substring(start + 1, end);
                        if (version.endsWith("/")) {
                            version = version.substring(0, version.length() - 1);
                        }
                        String date = tokenizer.nextToken();
                        String time = tokenizer.nextToken();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                        result.put(version, sdf.parse(date + " " + time));
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Cacheable(value = "artifactReleaseMap", key = "#artifactURI")
    public Map<String, Date> getChildReleaseDateMap(
            final String artifactURI,
            final String artifactPath,
            final RepositoryHandle releaseRepo,
            final Item child) {
        if (child.getName().matches("\\d.*")) {
            Folder childFolder = releaseRepo.folder(artifactPath + child.getUri()).info();
            List<Item> grandChildren = childFolder.getChildren();
            for (Item grandChild : grandChildren) {
                if (grandChild.getName().endsWith("pom")) {
                    File grandChildFile = releaseRepo.file(artifactPath + child.getUri() + grandChild
                            .getUri()).info();
                    Map<String, Date> releaseMap = new HashMap<>();
                    releaseMap.put(childFolder.getName(), grandChildFile.getCreated());
                    return releaseMap;
                }
            }
        }
        return new HashMap<>();
    }

    @Override
    public InputStream download(String artifactPath) {
        Artifactory artifactory = ArtifactoryClientBuilder.create()
                .setUrl(artifactoryBasePath).build();
        RepositoryHandle releaseRepo = artifactory.repository(artifactoryDownloadRepository);
        DownloadableArtifact download = releaseRepo.download(artifactPath);
        return download.doDownload();
    }
}
