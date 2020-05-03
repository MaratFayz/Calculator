package LD.model.EndDate;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EndDateID implements Serializable
{
	static final Long serialVersionUID = 1L;

//	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	@JoinColumn(name = "leasingDeposit_id", nullable = false)
//	private LeasingDeposit leasingDeposit;

	private Long leasingDeposit_id;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private Scenario scenario;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private Period period;

}
