package LD.model.Period;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PeriodDTO_out
{
	private Long id;
	private String date;

	public static PeriodDTO_out Period_to_PeriodDTO_out(Period period)
	{
		return PeriodDTO_out.builder()
				.id(period.getId())
				.date(DateFormat.formatDate(period.getDate()))
				.build();
	}
}
