package LD.config.Security;

import LD.config.Security.Repository.CustomAuthorityRepository;
import LD.config.Security.Repository.RoleRepository;
import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.Authority.CustomAuthority;
import LD.config.Security.model.Role.Role;
import LD.config.Security.model.User.User;
import LD.model.Currency.Currency;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Scenario.Scenario;
import LD.repository.CurrencyRepository;
import LD.repository.ScenarioRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
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

	@Autowired
	private CurrencyRepository currencyRepository;
	@Autowired
	private ScenarioRepository scenarioRepository;

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

		Role adminRole = createRoleIfNotFound("ROLE_SUPERADMIN", superAdminCustomAuthorities);

		User adminUser = User.builder()
			.isEnabled(STATUS_X.X)
			.isAccountExpired(null)
			.isCredentialsExpired(null)
			.isLocked(null)
			.username("a")
			.password(passwordEncoder.encode("a"))
			.roles(Set.of(adminRole))
			.lastChange(ZonedDateTime.now())
			.build();

		adminUser = userRepository.save(adminUser);

		Currency RUB = Currency.builder()
				.name("Russian Ruble")
				.CBRCurrencyCode(null)
				.short_name("RUB")
				.lastChange(ZonedDateTime.now())
				.user(adminUser)
				.build();

		Currency USD = Currency.builder()
				.name("USA Dollar")
				.CBRCurrencyCode("R01235")
				.short_name("USD")
				.lastChange(ZonedDateTime.now())
				.user(adminUser)
				.build();

		currencyRepository.save(RUB);
		currencyRepository.save(USD);

		Scenario fact = Scenario.builder()
				.status(ScenarioStornoStatus.ADDITION).name("FACT")
				.lastChange(ZonedDateTime.now())
				.user(adminUser)
				.build();

		scenarioRepository.save(fact);

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
					.lastChange(ZonedDateTime.now())
					.build();

			roleRepository.save(role);
		}

		return role;
	}

}
