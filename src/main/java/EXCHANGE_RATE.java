import lombok.Builder;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Builder(toBuilder = true)
@Entity
@Table(name = "EXCHANGE_RATE")
public class EXCHANGE_RATE
{
/*	id;
	SCENARIO
	CURRENCY
			date;*/
	double rate_at_date;
	double average_rate;
}
