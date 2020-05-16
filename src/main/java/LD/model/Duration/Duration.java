package LD.model.Duration;

import LD.config.Security.model.User.User;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "Duration")
@Data
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

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
