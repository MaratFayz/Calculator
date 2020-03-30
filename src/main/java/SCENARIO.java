import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "SCENARIO")
@Getter
@Setter
@ToString
@NoArgsConstructor()
public class SCENARIO
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "name", nullable = false)
	private String name;
}
