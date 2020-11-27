package LD.config.Security.Repository;

import LD.config.Security.model.Authority.CustomAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomAuthorityRepository extends JpaRepository<CustomAuthority, Long>
{
	CustomAuthority findByName(String name);
}
