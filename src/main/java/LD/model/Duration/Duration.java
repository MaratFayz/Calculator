package LD.model.Duration;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Duration")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Duration
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "MIN_MONTH", nullable = false)
	private int MIN_MONTH;

	@Column(name = "MAX_MONTH", nullable = false)
	private int MAX_MONTH;

	@Column(name = "name", nullable = false)
	private String name;
}
