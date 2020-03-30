import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "period")
@Getter
@Setter
@ToString
@NoArgsConstructor()
public class PERIOD
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "date", nullable = false, unique = true)
	private ZonedDateTime date;
}
