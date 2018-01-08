package ir.amv.os.tools.maveninterceptor.interceptor.impl.artifactory;

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

    InputStream download(String artifactPath);
}
