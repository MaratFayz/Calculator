package LD.model.Scenario;

import LD.model.Enums.STATUS_X;
import LD.model.Enums.ScenarioStornoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScenarioDTO_in
{
	private String name;

	private ScenarioStornoStatus status;

	private STATUS_X isBlocked;

	public static Scenario ScenarioDTO_to_Scenario(ScenarioDTO_in ScenarioDTO_in)
	{
		return Scenario.builder()
				.name(ScenarioDTO_in.getName())
				.status(ScenarioDTO_in.getStatus())
				.isBlocked(ScenarioDTO_in.getIsBlocked())
				.build();
	}
}
