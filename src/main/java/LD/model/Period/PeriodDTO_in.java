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
public class PeriodDTO_in
{
	private String date;

	public static Period PeriodDTO_to_Period(PeriodDTO_in periodDTO_in)
	{
		return Period.builder()
				.date(DateFormat.parsingDate(periodDTO_in.getDate()))
				.build();
	}

}
