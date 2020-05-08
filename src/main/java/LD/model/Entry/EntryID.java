package LD.model.Entry;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Data
@Builder(toBuilder = true)
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class EntryID implements Serializable
{
	static final Long serialVersionUID = 6L;

//	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//	@JoinColumn(name = "leasingDeposit_id", nullable = false)
//	private LeasingDeposit leasingDeposit;

	private Long leasingDeposit_id;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "scenario_id")
	private Scenario scenario;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "period_id", nullable = false)
	private Period period;

	private ZonedDateTime CALCULATION_TIME;


}
