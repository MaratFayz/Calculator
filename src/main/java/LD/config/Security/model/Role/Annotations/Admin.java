package LD.config.Security.model.Role.Annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@PreAuthorize("hasRole('ROLE_ADMIN')")
public @interface Admin
{

}
