package LD.model.ExchangeRate;

import LD.config.Security.model.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Table(name = "ExchangeRate")
@Entity
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class ExchangeRate
{
	@EmbeddedId
	private ExchangeRateID exchangeRateID;

	@Column(columnDefinition = "DECIMAL(31,12)")
	private BigDecimal rate_at_date;

	@Column(columnDefinition = "DECIMAL(31,12)")
	private BigDecimal average_rate_for_month;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
