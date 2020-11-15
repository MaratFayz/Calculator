package LD.model.DepositRate;

import LD.model.Company.Company;
import LD.model.Currency.Currency;
import LD.model.Duration.Duration;
import LD.model.Scenario.Scenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Embeddable
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositRateID implements Serializable
{
	static final Long serialVersionUID = 7L;

	@ManyToOne
	@JoinColumn(name = "company_id", nullable = false)
	private Company company;

	@Column(name = "START_PERIOD", nullable = false, columnDefinition = "DATE")
	private LocalDate START_PERIOD;

	@Column(name = "END_PERIOD", nullable = false, columnDefinition = "DATE")
	private LocalDate END_PERIOD;

	@ManyToOne
	@JoinColumn(name = "currency_id", nullable = false)
	private Currency currency;

	@ManyToOne
	@JoinColumn(name = "duration_id", nullable = false)
	private Duration duration;

	@ManyToOne
	@JoinColumn(name = "scenario_id", nullable = false)
	private Scenario scenario;
}
