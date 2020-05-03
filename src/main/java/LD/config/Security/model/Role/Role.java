package LD.config.Security.model.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;

@Entity
@Table(name = "t_role")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Role implements GrantedAuthority
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String name;

	public Role(Long id)
	{
		this.id = id;
	}

	@Override
	public String getAuthority()
	{
		return "ROLE_" + name;
	}
}
