/**
 * Copyright (c) Experian, 2009. All rights reserved.
 */
package ir.amv.os.tools.maveninterceptor.interceptor.impl;

import ir.amv.os.tools.maveninterceptor.interceptor.IRequestInterceptor;
import ir.amv.os.tools.maveninterceptor.interceptor.RequestInterceptContext;
import ir.amv.os.tools.maveninterceptor.interceptor.exc.RequestInterceptException;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.impl.smart.IGitApi;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Amir
 */
@Component
@Profile("smartThreshold")
public class BranchGitCommitCacheInvalidatorInterceptorImpl
        implements IRequestInterceptor {

    @Autowired
    private IGitApi gitApi;

    @Override
    public int rank() {
        return 0;
    }

    @Override
    public void intercept(final RequestInterceptContext context) throws RequestInterceptException {
        String requestURI = context.getRequestURI();
        if (requestURI.toLowerCase().endsWith("amiristhebestguyiknow")) {
            String branchName = context.getParamMap().get("branch")[0];
            gitApi.invalidateBranchCache(branchName);
            try {
                gitApi.getLastMergedCommitId(branchName);
                context.getOutputStream().write("Congrats on knowing Amir ;)".getBytes());
            } catch (Exception ignore) {
            }
            context.setFinished(true);
        }
    }
}
