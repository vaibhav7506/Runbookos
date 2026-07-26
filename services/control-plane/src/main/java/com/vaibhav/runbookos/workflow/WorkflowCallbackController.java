package com.vaibhav.runbookos.workflow;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/workflows")
public class WorkflowCallbackController {
  private final WorkflowCallbackService service;

  public WorkflowCallbackController(WorkflowCallbackService service) {
    this.service = service;
  }

  @PostMapping(value = "/callbacks", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void callback(
      @RequestHeader("X-Execution-Token") String token,
      @RequestHeader("X-Internal-Timestamp") String timestamp,
      @RequestHeader("X-Internal-Nonce") String nonce,
      @RequestHeader("X-Internal-Signature") String signature,
      @RequestBody byte[] body) {
    service.accept(token, timestamp, nonce, signature, body);
  }
}
