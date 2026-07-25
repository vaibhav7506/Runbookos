package com.vaibhav.runbookos.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmailNormalized(String emailNormalized);

  boolean existsByEmailNormalized(String emailNormalized);
}
