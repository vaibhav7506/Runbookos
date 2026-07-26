package com.vaibhav.runbookos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {
  @Test
  void organizationAdministrationIsLimitedToOwnerAndAdmin() {
    assertThat(Role.OWNER.canAdministerOrganization()).isTrue();
    assertThat(Role.ADMIN.canAdministerOrganization()).isTrue();
    assertThat(Role.RESPONDER.canAdministerOrganization()).isFalse();
    assertThat(Role.VIEWER.canAdministerOrganization()).isFalse();
    assertThat(Role.AUDITOR.canAdministerOrganization()).isFalse();
  }

  @Test
  void responderCapabilitiesDoNotLeakToReadOnlyRoles() {
    assertThat(Role.OWNER.canRespond()).isTrue();
    assertThat(Role.ADMIN.canRespond()).isTrue();
    assertThat(Role.RESPONDER.canRespond()).isTrue();
    assertThat(Role.VIEWER.canRespond()).isFalse();
    assertThat(Role.AUDITOR.canRespond()).isFalse();
  }

  @Test
  void auditAccessIsExplicit() {
    assertThat(Role.OWNER.canReadAudit()).isTrue();
    assertThat(Role.ADMIN.canReadAudit()).isTrue();
    assertThat(Role.AUDITOR.canReadAudit()).isTrue();
    assertThat(Role.RESPONDER.canReadAudit()).isFalse();
    assertThat(Role.VIEWER.canReadAudit()).isFalse();
  }
}
