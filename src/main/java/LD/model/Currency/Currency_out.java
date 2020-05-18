package LD.model.Currency;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class Currency_out
{
	private Long id;

	private String short_name;

	private String name;

	private String CBR_currency_code;

	private String user;

	private String lastChange;

	public static Currency_out Currency_to_CurrencyDTO(Currency currency)
	{
		return Currency_out.builder()
				.id(currency.getId())
				.short_name(currency.getShort_name())
				.name(currency.getName())
				.CBR_currency_code(currency.getCBRCurrencyCode())
				.user(currency.getUser().getUsername())
				.lastChange(DateFormat.formatDate(currency.getLastChange()))
				.build();

	}
}