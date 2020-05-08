package LD.config.Security;

import LD.config.Security.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
	@Autowired
	private UserService userService;

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		http
				.csrf().disable()
				.authorizeRequests()
					.antMatchers("/admin").hasRole("ADMIN")
					.antMatchers("/").authenticated()
					.anyRequest().authenticated()
				.and()
					.formLogin()
					//.loginPage("/login")
					.permitAll()
				.and()
					.logout()
					.permitAll();
	}

	@Autowired
	protected void configureGlobal(AuthenticationManagerBuilder auth) throws Exception
	{
		auth.userDetailsService(userService);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}


}
