package LD.model.ExchangeRate;

import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ExchangeRateID implements Serializable {

    static final Long serialVersionUID = 4L;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Currency currency;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Scenario scenario;

    private LocalDate date;
}
