package LD.model.Period;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NegativeOrZero;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PeriodDTO
{
	private String date;

	public static Period PeriodDTO_to_Period(PeriodDTO periodDTO)
	{
		return Period.builder()
				.date(DateFormat.parsingDate(periodDTO.getDate()))
				.build();
	}

	public static PeriodDTO Period_to_PeriodDTO(Period period)
	{
		return PeriodDTO.builder()
				.date(DateFormat.formatDate(period.getDate()))
				.build();
	}
}
