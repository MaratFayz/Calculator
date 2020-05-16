package LD.config.Security.Controller;

import LD.config.Security.model.User.User;
import LD.config.Security.Service.CustomUserDetailsService;
import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "Контроллер для админа")
@Log4j2
@RequestMapping("/admin")
public class AdminController
{
	@Autowired
	CustomUserDetailsService customUserDetailsService;

	@GetMapping
	public List<User> getUsers()
	{
		return customUserDetailsService.getUsers();
	}

	@PostMapping
	public boolean deleteUser(@RequestParam Long userId)
	{
		User userToDelete = customUserDetailsService.findById(userId);

		if(userToDelete != null)
		{
			customUserDetailsService.delete(userToDelete);
		}

		return false;
	}
}
