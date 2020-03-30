import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "DURATION")
@Getter
@Setter
@ToString
@NoArgsConstructor()
public class DURATION
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "MIN_MONTH", nullable = false)
	private int MIN_MONTH;

	@Column(name = "MAX_MONTH", nullable = false)
	private int MAX_MONTH;

	@Column(name = "name", nullable = false)
	private String name;
}
