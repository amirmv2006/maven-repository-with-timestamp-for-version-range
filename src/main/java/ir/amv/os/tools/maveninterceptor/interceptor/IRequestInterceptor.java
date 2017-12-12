package ir.amv.os.tools.maveninterceptor.interceptor;

import ir.amv.os.tools.maveninterceptor.interceptor.exc.RequestInterceptException;

public interface IRequestInterceptor {

    default int rank() {
        return Integer.MAX_VALUE;
    }


    void intercept(final RequestInterceptContext context) throws RequestInterceptException;
}
