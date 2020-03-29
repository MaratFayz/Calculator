import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "ENTITY")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class ENTITY
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "name", nullable = false)
	private String name;
}
