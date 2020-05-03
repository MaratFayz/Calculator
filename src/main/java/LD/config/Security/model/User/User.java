package LD.config.Security.model.User;

import LD.config.Security.model.Role.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

@Entity
@Table(name = "usr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	private String password;

	@Transient
	private String password2;

	@ManyToMany(fetch = FetchType.EAGER)
	private Set<Role> roles;

//	private boolean canView;
//	private boolean canAdd;
//	private boolean canEdit;
//	private boolean canCalculate;
//	private boolean canAdministrate;
//	private boolean canDelete;

	private boolean isExpired;
	private boolean isLocked;
	private boolean isEnabled;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		return roles;
	}

	@Override
	public String getPassword()
	{
		return password;
	}

	@Override
	public String getUsername()
	{
		return username;
	}

	@Override
	public boolean isAccountNonExpired()
	{
		return true; //!isExpired;
	}

	@Override
	public boolean isAccountNonLocked()
	{
		return true; //!isLocked;
	}

	@Override
	public boolean isCredentialsNonExpired()
	{
		return true; //!isExpired;
	}

	@Override
	public boolean isEnabled()
	{
		return true;//isEnabled;
	}
}
