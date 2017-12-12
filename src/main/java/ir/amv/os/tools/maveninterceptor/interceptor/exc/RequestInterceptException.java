package ir.amv.os.tools.maveninterceptor.interceptor.exc;

/**
 * @author Amir
 */
public class RequestInterceptException extends Exception {

    public RequestInterceptException() {
    }

    public RequestInterceptException(final String message) {
        super(message);
    }

    public RequestInterceptException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
