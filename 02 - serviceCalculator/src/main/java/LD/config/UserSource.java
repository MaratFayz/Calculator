package LD.config;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
public class UserSource {

    @Autowired
    private UserRepository userRepository;
    private User user;

    public User getAuthenticatedUser() {
        if (isNull(user)) {
            synchronized (this) {
                Authentication authentication = getAuthentication();

                if (nonNull(authentication)) {
                    String userName = authentication.getName();
                    user = userRepository.findByUsername(userName);
                } else {
                    user = userRepository.getOne(1L);
                }
            }
        }

        return user;
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}