package LD.model.Counterpartner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
