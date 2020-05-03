package LD.model.Scenario;

import LD.model.Enums.ScenarioStornoStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenarioDTO
{
	private Long id;

	private String name;

	private ScenarioStornoStatus status;

	public static Scenario ScenarioDTO_to_Scenario(ScenarioDTO ScenarioDTO)
	{
		return Scenario.builder()
				.id(ScenarioDTO.getId())
				.name(ScenarioDTO.getName())
				.status(ScenarioDTO.getStatus())
				.build();
	}
}
