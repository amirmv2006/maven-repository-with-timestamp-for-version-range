package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.util;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IGitApi;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IJenkinsApi;
import org.eclipse.jgit.api.CloneCommand;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
    @Autowired
    private IJenkinsApi jenkinsApi;

    @Value("${git.clone.url}")
    private String gitCloneUrl;
    @Value("${git.local.repo}")
    private String gitRepo;
    @Value("${git.local.username}")
    private String username;
    @Value("${git.local.password}")
    private String password;

    private Set<String> liveBranches = new HashSet<>();
    private static final int PAGE_SIZE = 10;

    @PostConstruct
    public void initialize() throws GitAPIException {
        CredentialsProvider.setDefault(new UsernamePasswordCredentialsProvider(username, password));
        File gitFolder = new File(getDotGitPath());
        if (!gitFolder.exists()) {
            LOGGER.info("Git folder '{}' doesn't exist, checking out '{}'", getDotGitPath(), gitCloneUrl);
            checkout(gitFolder);
            LOGGER.info("Git checkout successful");
        }
    }

    private String getDotGitPath() {
        if (!gitRepo.endsWith(".git")) {
            gitRepo += gitRepo + File.separatorChar + ".git";
        }
        return gitRepo;
    }

    @Scheduled(initialDelay = 10_000, fixedRate = 1 * 60 * 60_000)
    public synchronized void houseKeepLiveBranchesCaches() throws IOException, GitAPIException {
        try (Repository repository = new FileRepository(getDotGitPath())) {
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
                List<String> lastMergedCommitId = selfProxiedInstance.getLastMergedCommitId(branchRefName);
                Date finishDate = jenkinsApi.findSuccessfulBuildFinishDateForCommitId(lastMergedCommitId);
                LOGGER.debug("last merged commit to master for branch '{}' is {} and the finish date for successful " +
                                "build after this commit is {}", branchRefName, lastMergedCommitId, finishDate);
            }
            liveBranches.removeAll(currentLiveBranches);
            LOGGER.debug("invalidating cahce for obsolete branches {}", liveBranches);
            liveBranches.forEach(selfProxiedInstance::invalidateBranchCache);
            liveBranches = currentLiveBranches;
        }
    }

    private void checkout(final File gitFolder) throws GitAPIException{
        CloneCommand cc = new CloneCommand().setDirectory(gitFolder.getParentFile()).setURI(gitCloneUrl);
        cc.call();
    }

    @Override
    @Cacheable(value = "branchCommits", key = "#branchName")
    public synchronized List<String> getLastMergedCommitId(final String branchName) throws IOException,
            GitAPIException {
        LOGGER.info("getLastMergedCommitId for {}", branchName);
        liveBranches.add(branchName);
        try (Repository repository = new FileRepository(getDotGitPath())) {
            Git git = new Git(repository);
            ObjectId master = repository.parseCommit(repository.resolve("origin/master"));
            int skip = 0;
            while (skip < MAX_COMMIT_CHECK_COUNT) {
                Iterable<RevCommit> revCommits = git.log().add(repository.resolve("origin/" + branchName))
                        .setSkip(skip).setMaxCount(PAGE_SIZE).call();
                RevWalk walk = new RevWalk(repository);
                for (RevCommit revCommit : revCommits) {
                    if (walk.isMergedInto(walk.parseCommit(revCommit.getId()), walk.parseCommit(master))) {
                        List<String> result = new ArrayList<>();
                        result.add(revCommit.getName());
                        RevCommit[] parents = revCommit.getParents();
                        if (parents != null) {
                            for (RevCommit parent : parents) {
                                result.add(parent.getName());
                            }
                        }
                        return result;
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
