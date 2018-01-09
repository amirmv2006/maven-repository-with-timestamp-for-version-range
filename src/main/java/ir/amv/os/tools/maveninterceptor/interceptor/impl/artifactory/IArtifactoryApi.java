package ir.amv.os.tools.maveninterceptor.interceptor.impl.artifactory;

import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.Item;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * @author Amir
 */
public interface IArtifactoryApi {
    Map<String, Date> getReleaseDatesFor(String artifactPath) throws IOException, ParseException;

    Map<String, Date> getChildReleaseDateMap(
            final String uri, String artifactPath,
            RepositoryHandle releaseRepo,
            Item child);

    InputStream download(String artifactPath);
}
