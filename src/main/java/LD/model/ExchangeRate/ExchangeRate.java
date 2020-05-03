package LD.model.ExchangeRate;

import LD.model.Currency.Currency;
import LD.model.Scenario.Scenario;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Table(name = "ExchangeRate")
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class ExchangeRate
{
	@EmbeddedId
	private ExchangeRateID exchangeRateID;

	@Column(columnDefinition = "DECIMAL(31,12)")
	private BigDecimal rate_at_date;

	@Column(columnDefinition = "DECIMAL(31,12)")
	private BigDecimal average_rate_for_month;

}
