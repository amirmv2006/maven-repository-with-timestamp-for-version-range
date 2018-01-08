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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public Map<String, Date> getReleaseDatesFor(String artifactPath) {
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
                if (child.getName().matches("\\d.*")) {
                    Folder childFolder = releaseRepo.folder(artifactPath + child.getUri()).info();
                    List<Item> grandChildren = childFolder.getChildren();
                    for (Item grandChild : grandChildren) {
                        if (grandChild.getName().endsWith("pom")) {
                            File grandChildFile = releaseRepo.file(artifactPath + child.getUri() + grandChild
                                    .getUri()).info();
                            releaseMap.put(childFolder.getName(), grandChildFile.getCreated());
                        }
                    }
                }
            }
        }
        return releaseMap;
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
