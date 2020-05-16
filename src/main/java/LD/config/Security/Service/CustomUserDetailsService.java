package LD.config.Security.Service;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class CustomUserDetailsService implements UserDetailsService
{
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
	{
		log.info("Проверка юзера {} в базе данных", username);
		User user = userRepository.findByUsername(username);

		log.info(passwordEncoder.encode("a"));

		if(user == null)
		{
			log.info("В базе данных НЕ найден юзер {}", username);
			new UsernameNotFoundException("User not found");
		}

		return user;
	}

	public boolean saveNewUser(User user)
	{
		User userFromDB = userRepository.findByUsername(user.getUsername());

		if(userFromDB == null) return false;

		userRepository.save(user);

		return true;
	}

	public List<User> getUsers()
	{
		return userRepository.findAll();
	}

	public User findById(Long id)
	{
		return userRepository.findById(id).orElse(null);
	}

	public boolean delete(User user)
	{
		try
		{
			userRepository.delete(user);
		}
		catch (Exception e)
		{
			log.info("Не получилось удалить пользователя: {}", e);
			return false;
		}

		return true;
	}
}

