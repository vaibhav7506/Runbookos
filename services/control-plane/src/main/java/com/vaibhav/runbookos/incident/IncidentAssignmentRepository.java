package com.vaibhav.runbookos.incident;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAssignmentRepository extends JpaRepository<IncidentAssignment, UUID> {}
