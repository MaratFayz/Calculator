import lombok.*;

import javax.persistence.*;

@Table(name = "CURRENCY")
@Entity
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor()
public class CURRENCY
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "short_name", nullable = false)
	private String short_name;
}