package LD.model.Currency;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyDTO_in
{
	private String short_name;
	private String name;
	private String CBRCurrencyCode;

	public static Currency CurrencyDTO_in_to_Currency(CurrencyDTO_in currencyDTO_In)
	{
		return Currency.builder()
						.short_name(currencyDTO_In.getShort_name())
						.name(currencyDTO_In.getName())
						.CBRCurrencyCode(currencyDTO_In.getCBRCurrencyCode())
						.build();
	}
}
