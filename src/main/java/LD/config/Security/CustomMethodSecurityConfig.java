package LD.config.Security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

//@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
//public class CustomMethodSecurityConfig extends GlobalMethodSecurityConfiguration
//{
//	@Override
//	protected MethodSecurityExpressionHandler createExpressionHandler()
//	{
//		return super.createExpressionHandler();
//	}
//}
