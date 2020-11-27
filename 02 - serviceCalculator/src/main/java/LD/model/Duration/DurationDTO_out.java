package LD.model.Duration;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DurationDTO_out
{
	private Long id;

	private int MIN_MONTH;

	private int MAX_MONTH;

	private String name;

	private String user;

	private String lastChange;

	public static DurationDTO_out Duration_to_DurationDTO_out(Duration duration)
	{
		return DurationDTO_out.builder()
				.id(duration.getId())
				.MIN_MONTH(duration.getMIN_MONTH())
				.MAX_MONTH(duration.getMAX_MONTH())
				.name(duration.getName())
				.user(duration.getUserLastChanged().getUsername())
				.lastChange(DateFormat.formatDate(duration.getLastChange()))
				.build();
	}
}
