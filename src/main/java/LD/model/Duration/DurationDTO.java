package LD.model.Duration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DurationDTO
{
	private Long id;

	private int MIN_MONTH;

	private int MAX_MONTH;

	private String name;

	public static Duration DurationDTO_to_Duration(DurationDTO DurationDTO)
	{
		return Duration.builder()
				.id(DurationDTO.getId())
				.MIN_MONTH(DurationDTO.MIN_MONTH)
				.MAX_MONTH(DurationDTO.MAX_MONTH)
				.name(DurationDTO.getName())
				.build();
	}
}
