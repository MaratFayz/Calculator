import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "PERIODS_CLOSED")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@IdClass(value = PERIODS_CLOSED.KeyInPC.class)
public class PERIODS_CLOSED
{
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "period_id", nullable = false)
	private PERIOD period;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private SCENARIO scenario;

	@Enumerated(value = EnumType.STRING)
	private STATUS_X ISCLOSED;

	@Data
	public static class KeyInPC implements Serializable
	{
		static final long serialVersionUID = 2L;

		private SCENARIO scenario;
		private PERIOD period;
	}
}
