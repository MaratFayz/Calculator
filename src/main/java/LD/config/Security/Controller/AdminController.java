package LD.config.Security.Controller;

import LD.config.Security.model.User.User;
import LD.config.Security.Service.UserService;
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
	UserService userService;

	@GetMapping
	public List<User> getUsers()
	{
		return userService.getUsers();
	}

	@PostMapping
	public boolean deleteUser(@RequestParam Long userId)
	{
		User userToDelete = userService.findById(userId);

		if(userToDelete != null)
		{
			userService.delete(userToDelete);
		}

		return false;
	}
}
