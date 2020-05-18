package LD.config.Security.model.Role;

import LD.config.Security.model.Authority.CustomAuthority;
import LD.config.Security.model.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements GrantedAuthority
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "role_authorities",
			   joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
			   inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id"))
	private Set<CustomAuthority> authorities;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private User user_changed;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;

	@Override
	public String getAuthority()
	{
		return name;
	}
}
