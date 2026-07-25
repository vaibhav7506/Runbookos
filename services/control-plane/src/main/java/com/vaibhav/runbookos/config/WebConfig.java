package com.vaibhav.runbookos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class WebConfig {

  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeClientInfo(false);
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(false);
    filter.setIncludeHeaders(false);
    filter.setMaxPayloadLength(1000);
    filter.setAfterMessagePrefix("REQUEST: ");
    return filter;
  }
}
