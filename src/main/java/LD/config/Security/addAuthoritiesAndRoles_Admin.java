package LD.config.Security;

import LD.config.Security.Repository.CustomAuthorityRepository;
import LD.config.Security.Repository.RoleRepository;
import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.Authority.CustomAuthority;
import LD.config.Security.model.Role.Role;
import LD.config.Security.model.User.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

@Log4j2
@Component
public class addAuthoritiesAndRoles_Admin implements ApplicationListener<ContextRefreshedEvent>
{
	boolean alreadySetup = false;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private CustomAuthorityRepository customAuthorityRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		log.info("Запущена инициализация данных БД. alreadySetup = {}", alreadySetup);

		if (alreadySetup)
			return;

		Arrays.stream(CustomAuthority.customAuthorityPredefinedList)
				.forEach(ca -> createCustomAuthorityIfNotFound(ca));

		Set<CustomAuthority> superAdminCustomAuthorities = Set.copyOf(customAuthorityRepository.findAll());

		Role adminRole = createRoleIfNotFound("ROLE_ADMIN", superAdminCustomAuthorities);

		User adminUser = User.builder()
			.isEnabled(true)
			.isExpired(false)
			.isLocked(false)
			.username("a")
			.password(passwordEncoder.encode("a"))
			.roles(Set.of(adminRole))
			.build();

		userRepository.save(adminUser);

		alreadySetup = true;
	}

	@Transactional
	private CustomAuthority createCustomAuthorityIfNotFound(CustomAuthority customAuthority)
	{
		log.info("Запущен метод createCustomAuthorityIfNotFound. Проверка customAuthority = {}", customAuthority);

		CustomAuthority newCustomAuthority = customAuthorityRepository.findByName(customAuthority.getAuthority());

		log.info("Запущен метод createCustomAuthorityIfNotFound. newCustomAuthority = {}", newCustomAuthority);

		if (newCustomAuthority == null)
		{
			newCustomAuthority = customAuthorityRepository.save(customAuthority);
		}

		log.info("Запущен метод createCustomAuthorityIfNotFound. " +
				"Выдается значение newCustomAuthority = {}", newCustomAuthority);

		return newCustomAuthority;
	}

	@Transactional
	private Role createRoleIfNotFound(
			String name, Set<CustomAuthority> CustomAuthorities)
	{
		Role role = roleRepository.findByName(name);

		if (role == null)
		{
			role = Role.builder()
					.name(name)
					.authorities(CustomAuthorities)
					.build();

			roleRepository.save(role);
		}

		return role;
	}

}
