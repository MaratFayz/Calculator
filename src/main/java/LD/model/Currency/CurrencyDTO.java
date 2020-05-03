package LD.model.Currency;

import lombok.*;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class CurrencyDTO
{
	private String short_name;

	public static Currency CurrencyDTO_to_Currency(CurrencyDTO currencyDTO)
	{
		return Currency.builder()
						.short_name(currencyDTO.getShort_name())
						.build();
	}
}
