package LD.model.Counterpartner;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CounterpartnerDTO_in
{
	private String name;

	public static Counterpartner CounterpartnerDTO_in_to_Counterpartner(CounterpartnerDTO_in counterpartnerDTO_In)
	{
		return Counterpartner.builder()
				.name(counterpartnerDTO_In.getName())
				.build();
	}
}
