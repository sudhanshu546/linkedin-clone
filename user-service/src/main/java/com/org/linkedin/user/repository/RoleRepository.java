package com.org.linkedin.user.repository;

import com.org.linkedin.domain.user.Role;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

  boolean existsByName(String roleName);

  boolean existsByNameOrCode(String name, String code);
}
