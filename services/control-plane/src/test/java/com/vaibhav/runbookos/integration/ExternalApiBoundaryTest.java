package com.vaibhav.runbookos.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

class ExternalApiBoundaryTest {
  private WireMockServer externalApi;

  @BeforeEach
  void startServer() {
    externalApi = new WireMockServer(0);
    externalApi.start();
  }

  @AfterEach
  void stopServer() {
    externalApi.stop();
  }

  @Test
  void handlesAProviderResponseThroughAnIsolatedHttpBoundary() {
    externalApi.stubFor(
        get(urlEqualTo("/repository"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .willReturn(okJson("{\"full_name\":\"acme/checkout\"}")));

    String body =
        RestClient.builder()
            .baseUrl(externalApi.baseUrl())
            .build()
            .get()
            .uri("/repository")
            .header("Authorization", "Bearer test-token")
            .retrieve()
            .body(String.class);

    assertThat(body).contains("acme/checkout");
    externalApi.verify(1, getRequestedFor(urlEqualTo("/repository")));
  }
}
