package LD.Formats_DTO_in;

import LD.config.Security.model.User.User;
import LD.config.Security.model.User.UserDTO_in;
import LD.config.Security.model.User.UserTransform;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@Disabled("Need implementation")
public class UserDTO_in_Format
{
	@Autowired
	UserTransform userTransform;

	@Test
	public void DTOin_null_notCreatingExceptions()
	{
		UserDTO_in inputDTO_in = UserDTO_in.builder()
				.isEnabled(null)
				.isLocked(null)
				.isAccountExpired(null)
				.isCredentialsExpired(null)
				.isEnabled(null)
				.password(null)
				.roles(null)
				.username(null)
				.build();

		Class<?> uD = UserDTO_in.class;
		Arrays.stream(uD.getDeclaredFields()).forEach(System.out::println);

		try
		{
			User user = userTransform.UserDTO_in_to_User(inputDTO_in);
		}
		catch(Exception e)
		{
			Assert.fail();
		}
	}
}
