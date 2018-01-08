package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IGitApi;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Amir
 */
@Component
public class GitApiImpl
        implements IGitApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitApiImpl.class);
    public static final int MAX_COMMIT_CHECK_COUNT = 500;

    @Autowired
    private IGitApi selfProxiedInstance;

    @Value("${git.local.repo}")
    private String gitRepo;
    @Value("${git.local.username}")
    private String username;
    @Value("${git.local.password}")
    private String password;

    private Set<String> liveBranches = new HashSet<>();
    private static final int PAGE_SIZE = 10;

    @PostConstruct
    public void initialize() {
        CredentialsProvider.setDefault(new UsernamePasswordCredentialsProvider(username, password));
    }

    @Scheduled(initialDelay = 60_000, fixedRate = 6 * 60 * 60_000)
    public synchronized void houseKeepLiveBranchesCaches() throws IOException, GitAPIException {
        try (Repository repository = new FileRepository(gitRepo)) {
            Git git = new Git(repository);
            List<Ref> remoteBranchListCall = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            Set<String> currentLiveBranches = new HashSet<>();
            for (Ref branchRef : remoteBranchListCall) {
                String branchRefName = branchRef.getName();
                if (branchRefName.startsWith("refs/remotes/origin/")) {
                    branchRefName = branchRefName.substring("refs/remotes/origin/".length());
                }
                LOGGER.debug("found remote branch: {}", branchRefName);
                currentLiveBranches.add(branchRefName);
                String lastMergedCommitId = selfProxiedInstance.getLastMergedCommitId(branchRefName);
                LOGGER.debug("last merged commit to master for branch '{}' is {}", branchRefName, lastMergedCommitId);
            }
            liveBranches.removeAll(currentLiveBranches);
            LOGGER.debug("invalidating cahce for obsolete branches {}", liveBranches);
            liveBranches.forEach(selfProxiedInstance::invalidateBranchCache);
            liveBranches = currentLiveBranches;
        }
    }

    @Override
    @Cacheable(value = "branchCommits", key = "#branchName")
    public synchronized String getLastMergedCommitId(final String branchName) throws IOException, GitAPIException {
        LOGGER.info("getLastMergedCommitId for {}", branchName);
        liveBranches.add(branchName);
        try (Repository repository = new FileRepository(gitRepo)) {
            Git git = new Git(repository);
            ObjectId master = repository.parseCommit(repository.resolve("origin/master"));
            int skip = 0;
            while (skip < MAX_COMMIT_CHECK_COUNT) {
                Iterable<RevCommit> revCommits = git.log().add(repository.resolve("origin/" + branchName))
                        .setSkip(skip).setMaxCount(PAGE_SIZE).call();
                RevWalk walk = new RevWalk(repository);
                for (RevCommit revCommit : revCommits) {
                    if (walk.isMergedInto(walk.parseCommit(revCommit.getId()), walk.parseCommit(master))) {
                        return revCommit.name();
                    }
                }
                skip += PAGE_SIZE;
            }
        }
        return null;
    }

    @Override
    @CacheEvict(value = "branchCommits", key = "#branchName")
    public synchronized void invalidateBranchCache(final String branchName) {
        LOGGER.debug("invalidating cache for {}", branchName);
    }
}
