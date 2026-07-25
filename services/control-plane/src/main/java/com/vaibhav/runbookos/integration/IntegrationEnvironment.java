package com.vaibhav.runbookos.integration;

/**
 * Deployment environment an integration targets. The policy engine treats PRODUCTION as the
 * strictest tier when classifying action risk.
 */
public enum IntegrationEnvironment {
  DEVELOPMENT,
  STAGING,
  PRODUCTION
}
