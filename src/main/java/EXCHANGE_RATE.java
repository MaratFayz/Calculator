import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Table(name = "EXCHANGE_RATE")
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor()
@IdClass(value = EXCHANGE_RATE.KeyInER.class)
public class EXCHANGE_RATE
{
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private CURRENCY currency;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private SCENARIO scenario;

	@Id
	private ZonedDateTime date;

	private BigDecimal rate_at_date;
	private BigDecimal average_rate_for_month;

	@Data
	public static class KeyInER implements Serializable
	{
		static final long serialVersionUID = 4L;

		private CURRENCY currency;
		private SCENARIO scenario;
		private ZonedDateTime date;
	}
}
