package LD.model.Counterpartner;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CounterpartnerDTO
{
	private Long id;

	private String name;

	public static Counterpartner CounterpartnerDTO_to_Counterpartner(CounterpartnerDTO counterpartnerDTO)
	{
		return Counterpartner.builder()
				.id(counterpartnerDTO.getId())
				.name(counterpartnerDTO.getName())
				.build();
	}
}
