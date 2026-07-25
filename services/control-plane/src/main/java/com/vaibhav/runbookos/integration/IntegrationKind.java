package com.vaibhav.runbookos.integration;

/** External system an integration connects to. */
public enum IntegrationKind {
  GITHUB,
  SENTRY,
  SLACK,
  EMAIL,
  JIRA,
  CUSTOM_WEBHOOK,
  HTTP_HEALTHCHECK
}
