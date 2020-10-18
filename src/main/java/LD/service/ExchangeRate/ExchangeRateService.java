package LD.service.ExchangeRate;

import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO_out;
import LD.model.ExchangeRate.ExchangeRateID;

import java.util.List;

public interface ExchangeRateService {

    List<ExchangeRateDTO_out> getAllExchangeRates();

    ExchangeRate getExchangeRate(ExchangeRateID id);

    ExchangeRate saveNewExchangeRate(ExchangeRate period);

    ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate period);

    boolean delete(ExchangeRateID id);

    void importExchangeRatesFormCBR(long scenario_id, boolean isAddOnlyNewestRates);
}
