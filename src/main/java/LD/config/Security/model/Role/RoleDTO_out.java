package LD.config.Security.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO_out
{
	private Long id;

	private String name;

	private StringBuilder authorities;

	private String user_changed;

	private String lastChange;
}
