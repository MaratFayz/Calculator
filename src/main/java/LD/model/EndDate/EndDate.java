package LD.model.EndDate;

import LD.model.AbstractModelClass;
import LD.model.LeasingDeposit.LeasingDeposit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "end_date")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EndDate extends AbstractModelClass {

    @EmbeddedId
    EndDateID endDateID;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToOne
    @MapsId(value = "leasingDeposit_id")
    @JsonIgnore
    private LeasingDeposit leasingDeposit;
}
