package LD.model;

import LD.config.Security.model.User.User;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractModelClass {

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    protected User user;

    @Column(name = "DateTime_lastChange", nullable = false)
    protected ZonedDateTime lastChange;

//    @Column(name = "enabledforusing")
//    private boolean enabledForUsing;
}
