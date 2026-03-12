package com.org.linkedin.user.repository;

import com.org.linkedin.domain.user.TUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<TUser, UUID> {

  List<TUser> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);

  Optional<TUser> findByKeycloakUserId(UUID userId);

  Page<TUser> findAllByIsDeletedFalse(Pageable pageable);

  Optional<TUser> findByIdAndIsDeletedFalse(UUID id);

  boolean existsByEmail(String userName);

  //  List<TUser> findByRoleName(String roleName);

  Optional<TUser> findByEmail(String username);

  TUser findByEmailAndIsDeletedFalse(String email);
}
