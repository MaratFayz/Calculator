package LD.model.ExchangeRate;

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

@Table(name = "ExchangeRate")
@Entity
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class ExchangeRate extends AbstractModelClass {

    @EmbeddedId
    private ExchangeRateID exchangeRateID;

    @Column(columnDefinition = "DECIMAL(31,12)", nullable = false)
    private BigDecimal rate_at_date;

    @Column(columnDefinition = "DECIMAL(31,12)")
    private BigDecimal average_rate_for_month;
}
