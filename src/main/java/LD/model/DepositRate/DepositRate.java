package LD.model.DepositRate;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "DepositRate")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class DepositRate
{
	@EmbeddedId
	DepositRateID depositRateID;

	@Column(name = "RATE", nullable = false)
	private BigDecimal RATE;
}
