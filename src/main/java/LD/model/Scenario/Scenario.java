package LD.model.Scenario;

import LD.model.Enums.ScenarioStornoStatus;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "Scenario")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@EqualsAndHashCode
@Builder
@AllArgsConstructor
public class Scenario
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "storno_status", nullable = false, columnDefinition = "enum('FULL','ADDITION')")
	@Enumerated(value = EnumType.STRING)
	private ScenarioStornoStatus status;
}
