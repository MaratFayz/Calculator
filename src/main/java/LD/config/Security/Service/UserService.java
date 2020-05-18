package LD.config.Security.Service;

import LD.config.Security.model.User.User;
import LD.config.Security.model.User.UserDTO_out;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public interface UserService extends UserDetailsService
{
	@Override
	UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

	User saveNewUser(User user);

	List<UserDTO_out> getUsers();

	User findById(Long id);

	boolean delete(User user);

	User updateUser(Long id, User user);
}
