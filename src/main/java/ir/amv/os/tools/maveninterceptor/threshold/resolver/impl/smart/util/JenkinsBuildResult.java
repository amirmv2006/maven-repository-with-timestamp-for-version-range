package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import java.io.Serializable;
import java.util.List;

/**
 * @author Amir
 */
public class JenkinsBuildResult implements Serializable {

    private boolean buildSuccessful;
    private List<String> commitIds;

    public JenkinsBuildResult() {
    }

    public JenkinsBuildResult(final boolean buildSuccessful, final List<String> commitIds) {
        this.commitIds = commitIds;
        this.buildSuccessful = buildSuccessful;
    }

    public List<String> getCommitIds() {
        return commitIds;
    }

    public void setCommitIds(final List<String> commitIds) {
        this.commitIds = commitIds;
    }

    public boolean isBuildSuccessful() {
        return buildSuccessful;
    }

    public void setBuildSuccessful(final boolean buildSuccessful) {
        this.buildSuccessful = buildSuccessful;
    }

    @Override
    public String toString() {
        return "JenkinsBuildResult{" +
                "buildSuccessful=" + buildSuccessful +
                ", commitIds=" + commitIds +
                '}';
    }
}
