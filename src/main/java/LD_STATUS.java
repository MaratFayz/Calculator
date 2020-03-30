import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "LD_STATUS")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@IdClass(value = LD_STATUS.KeyInLDS.class)
public class LD_STATUS
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

	@Enumerated(value = EnumType.STRING)
	private STATUS_X is_created;

	@Enumerated(value = EnumType.STRING)
	private STATUS_X is_deleted;

	@Data
	public static class KeyInLDS implements Serializable
	{
		static final long serialVersionUID = 5L;

		private LD ld;
		private SCENARIO scenario;
	}
}
