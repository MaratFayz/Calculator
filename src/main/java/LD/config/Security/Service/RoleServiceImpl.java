package LD.config.Security.Service;

import LD.config.Security.Repository.RoleRepository;
import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.Role.Role;
import LD.config.Security.model.Role.RoleDTO_out;
import LD.config.Security.model.Role.RoleTransform;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class RoleServiceImpl implements RoleService
{
	@Autowired
	RoleRepository roleRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleTransform roleTransform;

	@Override
	public Role saveNewRole(Role role)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		role.setUser_changed(userRepository.findByUsername(username));

		role.setLastChange(ZonedDateTime.now());

		log.info("Роль для сохранения = {}", role);

		return roleRepository.save(role);
	}

	@Override
	public List<RoleDTO_out> getRoles()
	{
		List<Role> resultFormDB = roleRepository.findAll();
		List<RoleDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new RoleDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> roleTransform.Role_to_RoleDTO_out(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public Role findById(Long id)
	{
		return roleRepository.findById(id).orElse(null);
	}

	@Override
	public boolean delete(Role role)
	{
		try
		{
			roleRepository.delete(role);
		}
		catch (Exception e)
		{
			log.info("Не получилось удалить пользователя: {}", e);
			return false;
		}

		return true;
	}

	@Override
	public Role updateRole(Long id, Role role)
	{
		role.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		role.setUser_changed(userRepository.findByUsername(username));

		role.setLastChange(ZonedDateTime.now());

		Role roleToUpdate = findById(id);

		BeanUtils.copyProperties(role, roleToUpdate);

		roleRepository.saveAndFlush(roleToUpdate);

		return roleToUpdate;
	}
}
