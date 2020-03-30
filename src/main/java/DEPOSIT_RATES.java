import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "DEPOSIT_RATES")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@IdClass(value = DEPOSIT_RATES.KeyInDR.class)
public class DEPOSIT_RATES
{
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "entity_id", nullable = false)
	private ENTITY entity;

	@Id
	@Column(name = "START_PERIOD", nullable = false)
	private ZonedDateTime START_PERIOD;

	@Id
	@Column(name = "END_PERIOD", nullable = false)
	private ZonedDateTime END_PERIOD;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "currency_id", nullable = false)
	private CURRENCY currency;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "duration_id", nullable = false)
	private DURATION duration;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private SCENARIO scenario;

	@Column(name = "RATE", nullable = false)
	private BigDecimal RATE;

	@Data
	public static class KeyInDR implements Serializable
	{
		static final long serialVersionUID = 7L;

		private ENTITY entity;
		private ZonedDateTime START_PERIOD;
		private ZonedDateTime END_PERIOD;
		private CURRENCY currency;
		private DURATION duration;
		private SCENARIO scenario;
	}
}
