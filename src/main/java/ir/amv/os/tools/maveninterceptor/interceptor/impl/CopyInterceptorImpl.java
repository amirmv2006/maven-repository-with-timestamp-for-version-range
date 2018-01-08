package ir.amv.os.tools.maveninterceptor.interceptor.impl;

import ir.amv.os.tools.maveninterceptor.interceptor.IRequestInterceptor;
import ir.amv.os.tools.maveninterceptor.interceptor.RequestInterceptContext;
import ir.amv.os.tools.maveninterceptor.interceptor.exc.RequestInterceptException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Amir
 */
@Component
public class CopyInterceptorImpl
        implements IRequestInterceptor {
    @Override
    public void intercept(final RequestInterceptContext context) throws RequestInterceptException {
        try {
            IOUtils.copy(context.getInputStream().apply(null), context.getOutputStream());
        } catch (IOException e) {
            throw new RequestInterceptException("Exc", e);
        }
    }
}
