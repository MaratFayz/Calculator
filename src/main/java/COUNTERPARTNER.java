import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "COUNTERPARTNER")
@Getter
@Setter
@ToString
@NoArgsConstructor()
public class COUNTERPARTNER
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "name", nullable = false)
	private String name;
}