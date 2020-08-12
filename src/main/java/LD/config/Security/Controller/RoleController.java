package LD.config.Security.Controller;

import LD.config.Security.Service.RoleService;
import LD.config.Security.model.Role.Role;
import LD.config.Security.model.Role.RoleDTO_in;
import LD.config.Security.model.Role.RoleDTO_out;
import LD.config.Security.model.Role.RoleTransform;
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
@Api(value = "Контроллер для работы с ролями")
@Log4j2
@RequestMapping("/roles")
public class RoleController
{
	@Autowired
	RoleService roleService;
	@Autowired
	RoleTransform roleTransform;

	@GetMapping
	@ApiOperation(value = "Получение всех ролей", response = ResponseEntity.class)
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ROLE_READER)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Все роли возвращаются в ответе."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	public List<RoleDTO_out> getRoles()
	{
		return roleService.getRoles();
	}

	@GetMapping("{id}")
	@ApiOperation(value = "Получение роли с определённым id", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Роль существует, возвращается в ответе."),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Такой роли не существует")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ROLE_READER)")
	public ResponseEntity getRole(@PathVariable Long id)
	{
		Role role = roleService.findById(id);
		log.info("(getRole): role was taken: " + role);
		return new ResponseEntity(roleTransform.Role_to_RoleDTO_out(role), HttpStatus.OK);
	}

	@PostMapping
	@ApiOperation(value = "Сохранение новой роли", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Новая роль была сохранена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ROLE_ADDER)")
	public ResponseEntity saveNewRole(@RequestBody RoleDTO_in roleDTO_in)
	{
		Role newRoleToSave = roleTransform.RoleDTO_in_to_Role(roleDTO_in);
		roleService.saveNewRole(newRoleToSave);
		return new ResponseEntity(roleTransform.Role_to_RoleDTO_out(newRoleToSave), HttpStatus.OK);
	}

	@PutMapping("{id}")
	@ApiOperation(value = "Изменение значений роли", response = ResponseEntity.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Роль была изменена."),
			@ApiResponse(code = 404, message = "Доступ запрещён")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ROLE_EDITOR)")
	public ResponseEntity update(@PathVariable Long id, @RequestBody RoleDTO_in roleDTO_in)
	{
		log.info("(update): Поступил объект roleDTO_in", roleDTO_in);

		Role role = roleTransform.RoleDTO_in_to_Role(roleDTO_in);
		Role updatedRole = roleService.updateRole(id, role);
		return new ResponseEntity(roleTransform.Role_to_RoleDTO_out(updatedRole), HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	@ApiOperation(value = "Удаление значения")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Роль была успешно удалена"),
			@ApiResponse(code = 403, message = "Доступ запрещён"),
			@ApiResponse(code = 404, message = "Роль не была обнаружена")
	})
	@PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ROLE_DELETER)")
	public boolean deleteRole(@RequestParam Long roleId)
	{
		Role roleToDelete = roleService.findById(roleId);

		if(roleToDelete != null)
		{
			roleService.delete(roleToDelete);
		}

		return false;
	}
}
