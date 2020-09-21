package LD.dao;

import LD.repository.DepositRatesRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import org.springframework.stereotype.Component;

@Component
public interface DaoKeeper {

    ExchangeRateRepository getExchangeRateRepository();

    PeriodRepository getPeriodRepository();

    DepositRatesRepository getDepositRatesRepository();
}
