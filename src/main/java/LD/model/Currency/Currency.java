package LD.model.Currency;

import LD.config.Security.model.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Table(name = "Currency")
@Entity
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Currency
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Column(name = "short_name", nullable = false)
	private String short_name;

	@Column(name = "CBRCurrencyCode")
	private String CBRCurrencyCode;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}