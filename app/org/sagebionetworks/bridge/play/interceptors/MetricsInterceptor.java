package org.sagebionetworks.bridge.play.interceptors;

import static org.apache.http.HttpHeaders.USER_AGENT;
import static org.sagebionetworks.bridge.BridgeConstants.METRICS_EXPIRE_SECONDS;
import static org.sagebionetworks.bridge.BridgeConstants.X_FORWARDED_FOR_HEADER;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.sagebionetworks.bridge.models.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import play.cache.Cache;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;

@Component("metricsInterceptor")
public class MetricsInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MetricsInterceptor.class);

    @Override
    public Object invoke(MethodInvocation method) throws Throwable {
        final Metrics metrics = initMetrics();
        Cache.set(metrics.getCacheKey(), metrics, METRICS_EXPIRE_SECONDS);
        try {
            final Result result = (Result)method.proceed();
            metrics.setStatus(result.toScala().header().status());
            return result;
        } finally {
            Cache.remove(metrics.getCacheKey());
            metrics.end();
            logger.info(metrics.toJsonString());
        }
    }

    Metrics initMetrics() {
        final Request request = Http.Context.current().request();
        final Metrics metrics = new Metrics(RequestUtils.getRequestId(request));
        metrics.setMethod(request.method());
        metrics.setUri(request.path());
        metrics.setProtocol(request.version());
        metrics.setRemoteAddress(RequestUtils.header(request, X_FORWARDED_FOR_HEADER, request.remoteAddress()));
        metrics.setUserAgent(RequestUtils.header(request, USER_AGENT, null));
        return metrics;
    }
}
