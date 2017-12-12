package ir.amv.os.tools.maveninterceptor.threshold.resolver.impl;

import ir.amv.os.tools.maveninterceptor.threshold.resolver.IThresholdResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author Amir
 */
@Component
public class ApplicationPropertiesThresholdResolverImpl
        implements IThresholdResolver {
    @Value("${amir.version.range.up.to.date}")
    private String thresholdDate = "";
    @Value("${amir.version.range.up.to.date.format}")
    private String thresholdDateFormat = "";
    @Override
    public Date resolveThreshold(final Map<String, String[]> reqParamMap) {
        try {
            return new SimpleDateFormat(thresholdDateFormat).parse(thresholdDate);
        } catch (ParseException e) {
            return null;
        }
    }
}
