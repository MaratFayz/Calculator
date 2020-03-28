import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "END_DATES")
@Data
@Builder(toBuilder = true)
@ToString(exclude = "ld")
@EqualsAndHashCode(exclude = "ld")
public class END_DATES
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "ld_id", nullable = false)
	private LD ld;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "scenario_id", nullable = false)
	private SCENARIO scenario;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private PERIOD period;

	@Column(name = "End_Date", nullable = false)
	private Date End_Date;
}
