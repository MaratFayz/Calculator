package LD.config.Security.model.User;

import LD.config.DateFormat;
import LD.config.Security.Repository.RoleRepository;
import LD.config.Security.model.Role.Role;
import LD.model.Enums.STATUS_X;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@Component
@Log4j2
public class UserTransform {

    @Autowired
    RoleRepository roleRepository;

    public User UserDTO_in_to_User(UserDTO_in userDTO_in) {
        Collection<Role> rolesInUserDTO = new ArrayList<>();
        log.info("DTO in: {}", userDTO_in);

        if (userDTO_in.getRoles() != null) {
            String[] userRoles = userDTO_in.getRoles()
                    .split(",\\s*");

            log.info("Из DTO in {} получены роли: {}", userDTO_in, userRoles);

            Arrays.stream(userRoles)
                    .forEach(r ->
                    {
                        Role role = roleRepository.findByName(r);

                        if (role != null) rolesInUserDTO.add(role);
                    });
        }

        log.info("Итого из базы данных получены роли: {}", rolesInUserDTO);

        return User.builder()
                .username(userDTO_in.getUsername())
                .password(userDTO_in.getPassword())
                .isAccountExpired(userDTO_in.getIsAccountExpired())
                .isCredentialsExpired(userDTO_in.getIsCredentialsExpired())
                .isLocked(userDTO_in.getIsLocked())
                .isEnabled(userDTO_in.getIsEnabled())
                .roles(rolesInUserDTO)
                .build();
    }

    public UserDTO_out User_to_UserDTO_out(User user) {
        StringBuilder userRoles = user.getAuthorities()
                .stream()
                .reduce(new StringBuilder(),
                        (result, authority) -> result.append(", ")
                                .append(authority.getAuthority()),
                        (res1, res2) -> res1.append(", ")
                                .append(res2));

        userRoles = userRoles.delete(0, 2);

        return UserDTO_out.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .isAccountExpired(user.getIsAccountExpired())
                .isCredentialsExpired(user.getIsCredentialsExpired())
                .isLocked(user.getIsLocked())
                .isEnabled(user.getIsEnabled())
                .roles(userRoles)
                .user_changed(user.getUsername())
                .lastChange(DateFormat.formatDate(user.getLastChange()))
                .build();
    }
}
