package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface IJenkinsApi {
    Long getLastBuild() throws IOException;

    List<String> getBuildCommitIds(Long buildNo) throws IOException;

    Date getBuildFinishDate(Long buildNo) throws IOException;
}
