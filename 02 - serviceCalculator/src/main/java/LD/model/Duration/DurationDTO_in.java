package LD.model.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DurationDTO_in
{
	private int MIN_MONTH;

	private int MAX_MONTH;

	private String name;

	public static Duration DurationDTO_in_to_Duration(DurationDTO_in DurationDTO_in)
	{
		return Duration.builder()
				.MIN_MONTH(DurationDTO_in.MIN_MONTH)
				.MAX_MONTH(DurationDTO_in.MAX_MONTH)
				.name(DurationDTO_in.getName())
				.build();
	}
}
