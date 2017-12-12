package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IGitApi;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Amir
 */
@Component
public class GitApi
        implements IGitApi {

    @Value("${git.local.repo}")
    private String gitRepo;
    @Value("${git.local.username}")
    private String username;
    @Value("${git.local.password}")
    private String password;

    @Override
    public String getLastMergedCommitId(final String branchName) throws IOException, GitAPIException {
        try (Repository repository = new FileRepository(gitRepo)) {
            Git git = new Git(repository);
            CredentialsProvider.setDefault(new UsernamePasswordCredentialsProvider(username, password));
            Iterable<RevCommit> revCommits = git.log().add(repository.resolve("origin/" + branchName)).setMaxCount
                    (10).call();
            RevWalk walk = new RevWalk(repository);
            ObjectId master = repository.parseCommit(repository.resolve("origin/master"));
            for (RevCommit revCommit : revCommits) {
                if (walk.isMergedInto(walk.parseCommit(revCommit.getId()), walk.parseCommit(master))) {
                    return revCommit.name();
                }
            }
        }
        return null;
    }
}
