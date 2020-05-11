package LD.model.Currency;

import lombok.*;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class CurrencyDTO_in
{
	private String short_name;

	public static Currency CurrencyDTO_in_to_Currency(CurrencyDTO_in currencyDTO_In)
	{
		return Currency.builder()
						.short_name(currencyDTO_In.getShort_name())
						.build();
	}
}
