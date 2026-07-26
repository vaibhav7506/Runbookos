package com.vaibhav.runbookos.execution;

public enum ExecutionStatus {
  QUEUED,
  RUNNING,
  PAUSED_FOR_APPROVAL,
  SUCCEEDED,
  PARTIALLY_SUCCEEDED,
  FAILED,
  TIMED_OUT,
  CANCELLED
}
