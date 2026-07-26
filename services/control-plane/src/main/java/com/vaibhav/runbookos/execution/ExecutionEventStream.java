package com.vaibhav.runbookos.execution;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class ExecutionEventStream {
  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(UUID executionId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitters
        .computeIfAbsent(executionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(emitter);
    Runnable cleanup = () -> emitters.getOrDefault(executionId, List.of()).remove(emitter);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(e -> cleanup.run());
    return emitter;
  }

  public void publish(UUID executionId, Object event) {
    for (SseEmitter emitter : emitters.getOrDefault(executionId, List.of()))
      try {
        emitter.send(SseEmitter.event().name("execution-event").data(event));
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
  }
}
