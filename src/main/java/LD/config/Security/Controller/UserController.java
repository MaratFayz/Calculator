package LD.config.Security.Controller;

import LD.config.Security.Service.UserService;
import LD.config.Security.model.User.User;
import LD.config.Security.model.User.UserDTO_in;
import LD.config.Security.model.User.UserDTO_out;
import LD.config.Security.model.User.UserTransform;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(value = "Контроллер для работы с пользователями")
@Log4j2
@RequestMapping("/users")
public class UserController
{
	@Autowired
	UserService userService;
	@Autowired
	UserTransform userTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех пользователей", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).USER_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все пользователи возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<UserDTO_out> getUsers()
	{
		return userService.getUsers();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение пользователя с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь существует, возвращается в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Таковй пользователь отсутствует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).USER_READER)")
	public ResponseEntity getUser(@PathVariable Long id)
	{
		User user = userService.findById(id);
		log.info("(getUser): user was taken: " + user);
		return new ResponseEntity(userTransform.User_to_UserDTO_out(user), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение нового пользователя", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новый пользователь был сохранен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).USER_ADDER)")
	public ResponseEntity saveNewUser(@RequestBody UserDTO_in userDTO_in)
	{
		User newUserToSave = userTransform.UserDTO_in_to_User(userDTO_in);
		userService.saveNewUser(newUserToSave);
		return new ResponseEntity(userTransform.User_to_UserDTO_out(newUserToSave), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений пользователя", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь был изменен."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).USER_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody UserDTO_in userDTO_in)
	{
		log.info("(update): Поступил объект userDTO_in", userDTO_in);

		User user = userTransform.UserDTO_in_to_User(userDTO_in);
		User updatedUser = userService.updateUser(id, user);
		return new ResponseEntity(userTransform.User_to_UserDTO_out(updatedUser), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь был успешно удален"),
			@ApiResponse(code = 404, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Пользователь не был обнаружен")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).USER_DELETER)")
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
