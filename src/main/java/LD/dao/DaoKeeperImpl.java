package LD.dao;

import LD.repository.DepositRatesRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DaoKeeperImpl implements DaoKeeper {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private DepositRatesRepository depositRatesRepository;

    @Override
    public ExchangeRateRepository getExchangeRateRepository() {
        return exchangeRateRepository;
    }

    @Override
    public PeriodRepository getPeriodRepository() {
        return periodRepository;
    }

    @Override
    public DepositRatesRepository getDepositRatesRepository() {
        return depositRatesRepository;
    }
}
