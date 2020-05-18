package LD.config.Security.model.User;

import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO_in
{
	private String username;

	private String password;

	private STATUS_X isAccountExpired;

	private STATUS_X isCredentialsExpired;

	private STATUS_X isLocked;

	private STATUS_X isEnabled;

	private String roles;

}
