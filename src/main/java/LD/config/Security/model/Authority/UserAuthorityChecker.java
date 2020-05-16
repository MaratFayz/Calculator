package LD.config.Security.model.Authority;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component("UAC")
@Log4j2
public class UserAuthorityChecker
{
	public boolean hasPermission(ALL_AUTHORITIES authority)
	{
		//return "foo".equals(foo);
		log.info("hasPermission стартует");
		return true;
	}
}
