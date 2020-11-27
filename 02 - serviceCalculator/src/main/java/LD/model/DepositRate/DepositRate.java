package LD.model.DepositRate;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "DepositRate")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class DepositRate extends AbstractModelClass {

    @EmbeddedId
    DepositRateID depositRateID;

    @Column(name = "RATE", nullable = false)
    private BigDecimal RATE;
}
