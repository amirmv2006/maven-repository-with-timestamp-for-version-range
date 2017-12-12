package ir.amv.os.tools.maveninterceptor.interceptor;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.Date;

/**
 * @author Amir
 */
public class RequestInterceptContext {
    private InputStream inputStream;
    private ServletOutputStream outputStream;
    private String requestURI;
    private String newUrl;
    private Date threshold;
    private Boolean finished;

    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(final ServletOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    public void setNewUrl(final String newUrl) {
        this.newUrl = newUrl;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getNewUrl() {
        return newUrl;
    }

    public Date getThreshold() {
        return threshold;
    }

    public void setThreshold(final Date threshold) {
        this.threshold = threshold;
    }

    public void setFinished(final Boolean finished) {
        this.finished = finished;
    }

    public Boolean getFinished() {
        return finished;
    }

    @Override
    public String toString() {
        return "RequestInterceptContext{" +
                "inputStream=" + inputStream +
                ", outputStream=" + outputStream +
                ", requestURI='" + requestURI + '\'' +
                ", newUrl='" + newUrl + '\'' +
                ", threshold=" + threshold +
                '}';
    }
}
