package LD.model.Scenario;

import LD.config.DateFormat;
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
public class ScenarioDTO_out
{
	private Long id;

	private String name;

	private ScenarioStornoStatus status;

	private STATUS_X isBlocked;

	private String user;

	private String lastChange;

	public static ScenarioDTO_out Scenario_to_ScenarioDTO_out(Scenario scenario)
	{
		return ScenarioDTO_out.builder()
				.id(scenario.getId())
				.name(scenario.getName())
				.status(scenario.getStatus())
				.isBlocked(scenario.getIsBlocked())
				.user(scenario.getUserLastChanged().getUsername())
				.lastChange(DateFormat.formatDate(scenario.getLastChange()))
				.build();
	}
}
