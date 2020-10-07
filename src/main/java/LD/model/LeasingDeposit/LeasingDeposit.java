package LD.model.LeasingDeposit;

import LD.model.AbstractModelClass;
import LD.model.Company.Company;
import LD.model.Counterpartner.Counterpartner;
import LD.model.Currency.Currency;
import LD.model.EndDate.EndDate;
import LD.model.Entry.Entry;
import LD.model.Enums.STATUS_X;
import LD.model.Scenario.Scenario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "leasingDeposits")
@ToString(exclude = {"entries", "end_dates"})
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class LeasingDeposit extends AbstractModelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "counterpartner_id", nullable = false)
    private Counterpartner counterpartner;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @Column(name = "start_date", nullable = false, columnDefinition = "DATE")
    private LocalDate start_date;

    @Column(name = "deposit_sum_not_disc", nullable = false)
    private BigDecimal deposit_sum_not_disc;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statusX")
    private STATUS_X is_created;

    @Enumerated(value = EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(columnDefinition = "statusX")
    private STATUS_X is_deleted;

    @OneToMany(mappedBy = "leasingDeposit", fetch = FetchType.EAGER)
    @JsonIgnore
    private Set<Entry> entries;

    @OneToMany(mappedBy = "leasingDeposit", fetch = FetchType.EAGER)
    private Set<EndDate> end_dates;
}