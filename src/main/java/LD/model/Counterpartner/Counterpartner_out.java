package LD.model.Counterpartner;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Counterpartner_out
{
	private Long id;

	private String name;

	private String user;

	private String lastChange;

	public static Counterpartner_out Counterpartner_to_CounterpartnerDTO_out(Counterpartner counterpartner)
	{
		return Counterpartner_out.builder()
				.id(counterpartner.getId())
				.name(counterpartner.getName())
				.user(counterpartner.getUserLastChanged().getUsername())
				.lastChange(DateFormat.formatDate(counterpartner.getLastChange()))
				.build();
	}
}