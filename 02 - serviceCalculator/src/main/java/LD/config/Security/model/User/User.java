package LD.config.Security.model.User;

import LD.config.PostgreSQLEnumType;
import LD.config.Security.model.Role.Role;
import LD.model.AbstractModelClass;
import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Log4j2
public class User extends AbstractModelClass implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X isAccountExpired;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X isCredentialsExpired;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X isLocked;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X isEnabled;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(roles);

        log.info("roles = {}", roles);

        for (Role role : this.roles) {
            role.getAuthorities().stream()
                    .forEach(authorities::add);
        }

        log.info("authorities = {}", authorities);

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountExpired == null;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isLocked == null;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isCredentialsExpired == null;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled != null;
    }
}
