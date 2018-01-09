package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

public interface IGitApi {
    List<String> getLastMergedCommitId(String branchName) throws IOException, GitAPIException;

    void invalidateBranchCache(String branchName);
}
