package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Currency.Currency;
import LD.model.Currency.Currency_out;
import LD.repository.CurrencyRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CurrencyServiceImpl implements CurrencyService {

    @Autowired
    CurrencyRepository currencyRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<Currency_out> getAllCurrencies() {
        List<Currency> resultFormDB = currencyRepository.findAll();
        List<Currency_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new Currency_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> Currency_out.Currency_to_CurrencyDTO(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public Currency getCurrency(Long id) {
        return currencyRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public Currency saveNewCurrency(Currency currency) {
        currency.setUserLastChanged(userSource.getAuthenticatedUser());
        currency.setLastChange(ZonedDateTime.now());

        return currencyRepository.save(currency);
    }

    @Override
    public Currency updateCurrency(Long id, Currency currency) {
        currency.setId(id);

        Currency currencyToUpdate = getCurrency(id);

        BeanUtils.copyProperties(currency, currencyToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        currencyRepository.saveAndFlush(currencyToUpdate);

        return currencyToUpdate;
    }

    @Override
    public void delete(Long id) {
        currencyRepository.deleteById(id);
    }
}