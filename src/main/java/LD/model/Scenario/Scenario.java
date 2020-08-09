package LD.model.Scenario;

import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "Scenario")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "storno_status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private ScenarioStornoStatus status;

    @Column(name = "isBlocked")
    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    private STATUS_X isBlocked;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private User user;

    @Column(name = "DateTime_lastChange", nullable = false)
    private ZonedDateTime lastChange;
}
