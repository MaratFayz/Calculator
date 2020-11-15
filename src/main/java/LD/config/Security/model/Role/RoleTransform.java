package LD.config.Security.model.Role;

import LD.config.DateFormat;
import LD.config.Security.Repository.CustomAuthorityRepository;
import LD.config.Security.model.Authority.CustomAuthority;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@Log4j2
public class RoleTransform
{
	@Autowired
	CustomAuthorityRepository customAuthorityRepository;

	public Role RoleDTO_in_to_Role(RoleDTO_in roleDTO_in)
	{
		String[] roleAuthorities = roleDTO_in.getAuthorities().split(",\\s*");

		log.info("Из DTO in {} получены полномочия: {}", roleDTO_in, roleAuthorities);

		Set<CustomAuthority> authoritiesInRoleDTO = new HashSet<>();

		Arrays.stream(roleAuthorities).forEach(r -> {
			CustomAuthority customAuthority = customAuthorityRepository.findByName(r);

			if(customAuthority != null) authoritiesInRoleDTO.add(customAuthority);
		});

		log.info("Итого из базы данных получены полномочия: {}", authoritiesInRoleDTO);

		return Role.builder()
				.name(roleDTO_in.getName())
				.authorities(authoritiesInRoleDTO)
				.build();
	}

	public RoleDTO_out Role_to_RoleDTO_out(Role role)
	{
		StringBuilder RoleAuthorities = role.getAuthorities().stream().reduce(new StringBuilder(),
				(result, authority) -> result.append(", ").append(authority.getAuthority()),
				(res1, res2) -> res1.append(", ").append(res2));

		RoleAuthorities = RoleAuthorities.delete(0, 2);

		return RoleDTO_out.builder()
				.id(role.getId())
				.name(role.getName())
				.authorities(RoleAuthorities)
				.user_changed(role.getUserLastChanged() == null ? null : role.getUserLastChanged().getUsername())
				.lastChange(DateFormat.formatDate(role.getLastChange()))
				.build();
	}
}
