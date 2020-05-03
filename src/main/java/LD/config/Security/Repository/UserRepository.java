package LD.config.Security.Repository;

import LD.config.Security.model.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>
{
	User findByUsername(String username);
}
