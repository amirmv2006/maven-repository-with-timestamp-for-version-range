package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.IThresholdResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * @author Amir
 */
@Component
public class SmartThresholdResolver
        implements IThresholdResolver {

    @Autowired
    private IGitApi gitApi;

    @Autowired
    private IJenkinsApi jenkinsApi;

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public Date resolveThreshold(final Map<String, String[]> reqParamMap)
    {
        String[] branchNames = reqParamMap.get("branchName");
        if (branchNames == null) {
            return null;
        }
        String branchName = branchNames[0];
        try {
            String lastMergedCommitId = gitApi.getLastMergedCommitId(branchName);
            Date finishDate = jenkinsApi.findSuccessfulBuildFinishDateForCommitId(lastMergedCommitId);
            return finishDate == null ? new Date() : finishDate;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
