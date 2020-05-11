package LD.model.Scenario;

import LD.model.Enums.ScenarioStornoStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenarioDTO_in
{
	private String name;

	private ScenarioStornoStatus status;

	public static Scenario ScenarioDTO_to_Scenario(ScenarioDTO_in ScenarioDTO_in)
	{
		return Scenario.builder()
				.name(ScenarioDTO_in.getName())
				.status(ScenarioDTO_in.getStatus())
				.build();
	}
}
