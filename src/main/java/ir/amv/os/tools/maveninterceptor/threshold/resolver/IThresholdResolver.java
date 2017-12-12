package ir.amv.os.tools.maveninterceptor.threshold.resolver;

import java.util.Date;
import java.util.Map;

public interface IThresholdResolver {

    default int rank() {
        return Integer.MAX_VALUE;
    }

    Date resolveThreshold(Map<String, String[]> reqParamMap);
}
