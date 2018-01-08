package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util.JenkinsBuildResult;

import java.io.IOException;
import java.util.Date;

public interface IJenkinsApi {
    Long getLastBuild() throws IOException;

    JenkinsBuildResult getBuildStats(Long buildNo) throws IOException;

    Date getBuildFinishDate(Long buildNo) throws IOException;

    Date findSuccessfulBuildFinishDateForCommitId(String commitId) throws IOException;
}
