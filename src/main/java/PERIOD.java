import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "period")
@Data
@Builder(toBuilder = true)
public class PERIOD
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@Column(name = "date")
	private Date date;
}
