package com.vaibhav.runbookos.workflow;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  DELIVERED,
  DEAD
}
