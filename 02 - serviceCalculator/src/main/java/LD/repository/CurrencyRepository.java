package LD.repository;

import LD.model.Currency.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findByCBRCurrencyCodeNotNull();
}
