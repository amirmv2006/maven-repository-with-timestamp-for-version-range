package ir.amv.os.tools.maveninterceptor.interceptor;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Amir
 */
public class RequestInterceptContext {
    private Function<Void, InputStream> inputStream;
    private ServletOutputStream outputStream;
    private String requestURI;
    private Date threshold;
    private Boolean finished;
    private Map<String, String[]> paramMap;

    public void setInputStream(final Function<Void, InputStream> inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(final ServletOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setRequestURI(final String requestURI) {
        this.requestURI = requestURI;
    }

    public Function<Void, InputStream> getInputStream() {
        return inputStream;
    }

    public ServletOutputStream getOutputStream() {
        return outputStream;
    }

    public String getRequestURI() {
        return requestURI;
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
                ", threshold=" + threshold +
                '}';
    }

    public void setParamMap(final Map<String, String[]> paramMap) {
        this.paramMap = paramMap;
    }

    public Map<String, String[]> getParamMap() {
        return paramMap;
    }
}
