package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

public interface IGitApi {
    String getLastMergedCommitId(String branchName) throws IOException, GitAPIException;

    void invalidateBranchCache(String branchName);
}
