import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

@Entity
@Table(name = "END_DATES")
@ToString(exclude = {"ld", "KeyInEND_DATES"})
@Getter
@Setter
@NoArgsConstructor
@IdClass(value = END_DATES.KeyInEND_DATES.class)
public class END_DATES
{
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "ld_id", nullable = false)
	private LD ld;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private SCENARIO scenario;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private PERIOD period;

	@Column(name = "End_Date", nullable = false)
	private ZonedDateTime End_Date;

	@Data
	public static class KeyInEND_DATES implements Serializable
	{
		static final long serialVersionUID = 1L;

		private LD ld;
		private SCENARIO scenario;
		private PERIOD period;
	}
}
