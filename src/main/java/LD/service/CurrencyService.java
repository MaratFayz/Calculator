package LD.service;

import LD.model.Currency.Currency;
import LD.model.Currency.Currency_out;

import java.util.List;

public interface CurrencyService
{
	List<Currency_out> getAllCurrencies();

	Currency getCurrency(Long id);

	Currency saveNewCurrency(Currency currency);

	Currency updateCurrency(Long id, Currency currency);

	boolean delete(Long id);
}
