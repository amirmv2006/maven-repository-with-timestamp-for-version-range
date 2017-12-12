package ir.amv.os.tools.maveninterceptor.controller;

import ir.amv.os.tools.maveninterceptor.interceptor.IRequestInterceptor;
import ir.amv.os.tools.maveninterceptor.interceptor.RequestInterceptContext;
import ir.amv.os.tools.maveninterceptor.threshold.resolver.IThresholdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.apache.tomcat.util.http.fileupload.IOUtils.copy;

/**
 * @author Amir
 */
@Controller
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    @Value("${amir.remote.maven}")
    private String remoteServer;

    private List<IRequestInterceptor> requestInterceptors;
    private List<IThresholdResolver> thresholdResolvers;

    @RequestMapping(path = "/**", method = {
            RequestMethod.GET,
            RequestMethod.HEAD,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.PATCH,
            RequestMethod.TRACE
    })
    public void serviceGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        LOGGER.debug("Serving '{}'. request param map is {}", requestURI, request.getParameterMap());
        String newUrl = remoteServer + requestURI;
        URLConnection connection = new URL(newUrl).openConnection();
        InputStream inputStream = connection.getInputStream();
        ServletOutputStream outputStream = response.getOutputStream();

        RequestInterceptContext context = new RequestInterceptContext();
        context.setInputStream(inputStream);
        context.setOutputStream(outputStream);
        context.setRequestURI(requestURI);
        context.setNewUrl(newUrl);
        for (IThresholdResolver thresholdResolver : thresholdResolvers) {
            
            Date date = thresholdResolver.resolveThreshold(request.getParameterMap());
            if (date != null) {
                context.setThreshold(date);
                break;
            }
        }

        LOGGER.debug("Request context: '{}'", context);

        for (IRequestInterceptor interceptor : requestInterceptors) {
            try {
                interceptor.intercept(context);
                if (context.getFinished()) {
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        response.getOutputStream().flush();
    }

    @RequestMapping(path = "/**", method = {
            RequestMethod.POST,
            RequestMethod.PUT,
    })
    public void servicePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        String newUrl = remoteServer + requestURI;
        URLConnection connection = new URL(newUrl).openConnection();
        connection.setDoOutput(true);

        ServletInputStream requestInputStream = request.getInputStream();
        OutputStream connectionOutputStream = connection.getOutputStream();
        copy(requestInputStream, connectionOutputStream);

        InputStream connectionInputStream = connection.getInputStream();
        ServletOutputStream responseOutputStream = response.getOutputStream();
        copy(connectionInputStream, responseOutputStream);

        response.getOutputStream().flush();
    }

    @Autowired
    public void setRequestInterceptors(final List<IRequestInterceptor> requestInterceptors) {
        this.requestInterceptors = new ArrayList<>(requestInterceptors);
        this.requestInterceptors.sort(Comparator.comparingInt(IRequestInterceptor::rank));
        LOGGER.info("got request interceptors: {}", requestInterceptors);
    }

    @Autowired
    public void setThresholdResolvers(final List<IThresholdResolver> thresholdResolvers) {
        this.thresholdResolvers = new ArrayList<>(thresholdResolvers);
        this.thresholdResolvers.sort(Comparator.comparingInt(IThresholdResolver::rank));
        LOGGER.info("got threshold resolvers: {}", thresholdResolvers);
    }
}
