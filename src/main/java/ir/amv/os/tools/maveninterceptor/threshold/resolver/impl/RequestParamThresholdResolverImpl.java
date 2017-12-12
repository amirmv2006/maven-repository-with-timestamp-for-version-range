package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.IThresholdResolver;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * @author Amir
 */
@Component
public class RequestParamThresholdResolverImpl
        implements IThresholdResolver {
    @Override
    public int rank() {
        return 10;
    }

    @Override
    public Date resolveThreshold(final Map<String, String[]> reqParamMap) {
        String[] thresholds = reqParamMap.get("threshold");
        return (thresholds != null) ? new Date(Long.parseLong(thresholds[0])) : null;
    }
}
