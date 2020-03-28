import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "LD_STATUS")
@Data
@Builder(toBuilder = true)
public class LD_STATUS
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

	@Enumerated(value = EnumType.STRING)
	private STATUS_X is_created;

	@Enumerated(value = EnumType.STRING)
	private STATUS_X is_deleted;
}
