package LD.model.PeriodsClosed;

import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PeriodsClosedID implements Serializable
{
	static final Long serialVersionUID = 2L;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Scenario scenario;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private Period period;
}
