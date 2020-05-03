package LD.config.Security.Repository;

import LD.config.Security.model.Role.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long>
{
}
