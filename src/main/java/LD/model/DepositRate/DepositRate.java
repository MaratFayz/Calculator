package LD.model.DepositRate;

import LD.config.Security.model.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "DepositRate")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class DepositRate
{
	@EmbeddedId
	DepositRateID depositRateID;

	@Column(name = "RATE", nullable = false)
	private BigDecimal RATE;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
