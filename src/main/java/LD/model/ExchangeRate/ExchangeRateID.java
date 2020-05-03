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
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ExchangeRateID implements Serializable
{
	static final Long serialVersionUID = 4L;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Currency currency;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Scenario scenario;

	private ZonedDateTime date;
}
