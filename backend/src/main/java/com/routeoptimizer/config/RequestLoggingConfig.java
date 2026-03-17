package com.routeoptimizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    /**
     * Logs incoming HTTP request payloads to the application console/Render logs.
     * Activate in application.properties by setting:
     *   logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(5000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("[REQUEST] ");
        return filter;
    }
}
