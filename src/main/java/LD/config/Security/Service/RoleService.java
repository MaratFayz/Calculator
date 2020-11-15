package LD.config.Security.Service;

import LD.config.Security.model.Role.Role;
import LD.config.Security.model.Role.RoleDTO_out;

import java.util.List;

public interface RoleService
{
	Role saveNewRole(Role role);

	List<RoleDTO_out> getRoles();

	Role findById(Long id);

	void delete(Role role);

	Role updateRole(Long id, Role role);
}
