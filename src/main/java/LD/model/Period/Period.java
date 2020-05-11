package LD.model.Period;

import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "period")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder(toBuilder = true)
@AllArgsConstructor
public class Period
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "date", nullable = false, unique = true)
	private ZonedDateTime date;
}
