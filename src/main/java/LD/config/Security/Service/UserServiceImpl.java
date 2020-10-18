package LD.config.Security.Service;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.config.Security.model.User.UserDTO_out;
import LD.config.Security.model.User.UserTransform;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserTransform userTransform;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Проверка юзера {} в базе данных", username);
        User user = userRepository.findByUsername(username);

        if (user == null) {
            log.info("В базе данных НЕ найден юзер {}", username);
            throw new UsernameNotFoundException("User not found");
        }

        log.info("user authorities = {}", user.getAuthorities());

        return user;
    }

    @Override
    public User saveNewUser(User user) {
        User userFromDB = userRepository.findByUsername(user.getUsername());
        if (userFromDB != null) return null;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        user.setUserLastChanged(userRepository.findByUsername(username));

        user.setLastChange(ZonedDateTime.now());

        log.info("Пользователь для сохранения = {}", user);

        return userRepository.save(user);
    }

    @Override
    public List<UserDTO_out> getUsers() {
        List<User> resultFormDB = userRepository.findAll();
        List<UserDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new UserDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> userTransform.User_to_UserDTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public boolean delete(User user) {
        try {
            userRepository.delete(user);
        } catch (Exception e) {
            log.info("Не получилось удалить пользователя: {}", e);
            return false;
        }

        return true;
    }

    @Override
    public User updateUser(Long id, User user) {
        user.setId(id);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        user.setUserLastChanged(userRepository.findByUsername(username));

        user.setLastChange(ZonedDateTime.now());

        User userToUpdate = findById(id);

        BeanUtils.copyProperties(user, userToUpdate);

        userRepository.saveAndFlush(userToUpdate);

        return userToUpdate;
    }
}