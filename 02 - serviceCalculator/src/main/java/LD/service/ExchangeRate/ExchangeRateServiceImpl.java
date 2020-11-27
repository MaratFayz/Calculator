package LD.service.ExchangeRate;

import LD.config.Security.model.User.User;
import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Currency.Currency;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO_out;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateTransform;
import LD.model.Scenario.Scenario;
import LD.repository.CurrencyRepository;
import LD.repository.ExchangeRateRepository;
import LD.repository.PeriodRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@Log4j2
public class ExchangeRateServiceImpl implements ExchangeRateService {

    @Autowired
    ExchangeRateRepository exchangeRateRepository;
    @Autowired
    ExchangeRateTransform exchangeRateTransform;
    @Autowired
    CurrencyRepository currencyRepository;
    @Autowired
    PeriodRepository periodRepository;
    @Autowired
    ScenarioRepository scenarioRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<ExchangeRateDTO_out> getAllExchangeRates() {
        List<ExchangeRate> resultFormDB = exchangeRateRepository.findAll();
        List<ExchangeRateDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new ExchangeRateDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(er -> exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(er))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public ExchangeRate getExchangeRate(ExchangeRateID id) {
        return exchangeRateRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public ExchangeRate saveNewExchangeRate(ExchangeRate exchangeRate) {
        exchangeRate.setUserLastChanged(userSource.getAuthenticatedUser());

        exchangeRate.setLastChange(ZonedDateTime.now());

        log.info("Валютный курс для сохранения = {}", exchangeRate);

        return exchangeRateRepository.save(exchangeRate);
    }

    @Override
    public ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate exchangeRate) {
        ExchangeRate exchangeRateToUpdate = getExchangeRate(id);

        BeanUtils.copyProperties(exchangeRate, exchangeRateToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        exchangeRateRepository.saveAndFlush(exchangeRateToUpdate);

        return exchangeRateToUpdate;
    }

    @Override
    public void delete(ExchangeRateID id) {
        exchangeRateRepository.deleteById(id);
    }

    @Override
    public void importExchangeRatesFormCBR(long scenario_id, boolean isAddOnlyNewestRates) {
        //перечень валют с кодом ЦБ
        List<Currency> currenciesWithCBRCode = currencyRepository.findByCBRCurrencyCodeNotNull();

        log.info("Перечень валют, которые имеют код ЦБ => {}", currenciesWithCBRCode);

        //самая ранняя дата периодов в базе данных (с неё будет загружаться информация с ЦБ)
        LocalDate minPeriodDate = periodRepository.findMinPeriodDateInDatabase();
        log.info("Наименьшая дата (1е число) в справочнике периодов => {}", minPeriodDate);

        //самая поздняя дата периодов в базе данных (по неё будет загружаться информация с ЦБ)
        LocalDate maxPeriodDate = periodRepository.findMaxPeriodDateInDatabase();
        log.info("Наибольшая дата в справочнике периодов => {}", maxPeriodDate);

        User loadingUser = userSource.getAuthenticatedUser();
        Scenario loadingScenario = scenarioRepository.findById(scenario_id).get();

        currenciesWithCBRCode.stream().forEach(currency ->
        {
            if (isAddOnlyNewestRates) {
                LocalDate maxCurExDate;

                maxCurExDate = exchangeRateRepository.findMaxDateWithExchangeRateByCurrencyIdAndScenarioId(currency.getId(), scenario_id);

                if (isNull(maxCurExDate)) {
                    log.info("Курсов валют в базе не представлено, начинается загрузка с нуля");
                    maxCurExDate = LocalDate.MIN;
                }

                LocalDate prevDayBeforeNow = LocalDate.now();

                if (!maxCurExDate.isEqual(LocalDate.MIN)) {
                    if (maxPeriodDate.isAfter(maxCurExDate) && prevDayBeforeNow.isAfter(maxCurExDate)) {
                        LocalDate saveFromDate = maxCurExDate.plusDays(1);

                        getCurExRateFromCentrobankAndSaveIntoDB(loadingUser, loadingScenario, currency,
                                saveFromDate, maxPeriodDate);
                    } else {
                        log.info("Даты равны: Максимальная дата курсов валют есть = {}, " +
                                        "дата макс периода = {}, дата на момент расчета = {}; расчет проводиться не будет",
                                maxCurExDate, maxPeriodDate, prevDayBeforeNow);
                    }
                } else {
                    log.info("Курсов валют в базе не представлено, начинается загрузка с нуля");
                    deleteDownloadSaveCurExFormCBR(minPeriodDate, maxPeriodDate, loadingUser, loadingScenario, currency);
                }
            } else {
                log.info("Требуется всё удалить и загрузить по новой");
                deleteDownloadSaveCurExFormCBR(minPeriodDate, maxPeriodDate, loadingUser, loadingScenario, currency);
            }
        });
    }

    private void deleteDownloadSaveCurExFormCBR(LocalDate minPeriodDate, LocalDate maxPeriodDate, User loadingUser,
                                                Scenario loadingScenario, Currency currency) {
        List<ExchangeRate> toDeleteExR = exchangeRateRepository.findAll((root, query, qb) -> qb.equal(root.get("exchangeRateID").get("currency"), currency));
        exchangeRateRepository.deleteAll(toDeleteExR);

        getCurExRateFromCentrobankAndSaveIntoDB(loadingUser, loadingScenario, currency, minPeriodDate, maxPeriodDate);
    }

    private void getCurExRateFromCentrobankAndSaveIntoDB(User loadingUser, Scenario loadingScenario, Currency currency,
                                                         LocalDate queryDateFrom, LocalDate queryDateTo) {
        LocalDate now = LocalDate.now();
        queryDateTo = now.isBefore(queryDateTo) ? now : queryDateTo;
        LocalDate saveFromDate = queryDateFrom.minusMonths(1);

        queryDateFrom = queryDateFrom.minusMonths(2);
        LocalDate saveTillDate = queryDateTo;

        queryDateFrom = queryDateFrom.minusMonths(1);

        TreeMap<LocalDate, BigDecimal> exRatesFormCBR = getExRatesFromCBR(queryDateFrom, queryDateTo, currency);

        saveFromDate.datesUntil(saveTillDate.plusDays(1), java.time.Period.ofDays(1))
                .forEach(date ->
                {
                    ExchangeRateID exRId = ExchangeRateID.builder()
                            .scenario(loadingScenario)
                            .currency(currency)
                            .date(date)
                            .build();

                    ExchangeRate exR = ExchangeRate.builder()
                            .exchangeRateID(exRId)
                            .rate_at_date(exRatesFormCBR.floorEntry(date).getValue())
                            .build();

                    exR.setLastChange(ZonedDateTime.now());
                    exR.setUserLastChanged(loadingUser);

                    exR = exchangeRateRepository.save(exR);

                    LocalDate lastDayOfDateMonth = date.withDayOfMonth(date.lengthOfMonth());
                    BigDecimal avgRate = BigDecimal.ZERO;
                    if (date.getDayOfMonth() == lastDayOfDateMonth.getDayOfMonth()) {
                        for (int day = 1; day <= lastDayOfDateMonth.getDayOfMonth(); day++) {
                            LocalDate z = date.withDayOfMonth(day);
                            BigDecimal exRateForDay = exRatesFormCBR.floorEntry(z).getValue();
                            avgRate = avgRate.add(exRateForDay);
                        }

                        avgRate = avgRate.divide(BigDecimal.valueOf(lastDayOfDateMonth.getDayOfMonth()), RoundingMode.HALF_UP);
                        exR.setAverage_rate_for_month(avgRate);

                        exchangeRateRepository.save(exR);
                    }

                });
    }

    private TreeMap<LocalDate, BigDecimal> getExRatesFromCBR(LocalDate queryDateFrom,
                                                             LocalDate queryDateTo,
                                                             Currency currency) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFrom = queryDateFrom.format(dtf); //"01/01/2020";
        String dateTo = queryDateTo.format(dtf); //"10/01/2020";
        String curCodeCBR = currency.getCBRCurrencyCode();

        Document doc = null;

        String url = getUrl(dateFrom, dateTo, curCodeCBR);

        try {

            doc = Jsoup.connect(url).get();

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            TreeMap<LocalDate, BigDecimal> currencyRates = new TreeMap<>();

            doc.select("Record").forEach(element -> {
                LocalDate Date_curExRate = LocalDate.parse(element.attr("Date"), dateTimeFormatter);
                log.info("Date_curExRate = " + Date_curExRate);

                BigDecimal ExRateForDate = BigDecimal.valueOf(Double.parseDouble(
                        element.select("Value").text().replace(",", ".")));
                log.info("exRate = " + ExRateForDate);

                currencyRates.put(Date_curExRate, ExRateForDate);
            });

            return currencyRates;
        } catch (IOException e) {
            log.info(e);
            return null;
        }
    }

    @Lookup
    GetPropertiesForLdProjectFromFile getDataFromFile(String fileDirectory) {
        return null;
    }

    private String getUrl(String dateFrom, String dateTo, String curCodeCBR) {
        GetPropertiesForLdProjectFromFile dataFromFile = getDataFromFile("../config/PropertiesForLdProject.xml");
        PropertiesForLdProject propertiesForLdProject = dataFromFile.getPropertiesForLdProject();

        String cbrUrl = propertiesForLdProject.getCbrUrl();
        String dateFromValue = propertiesForLdProject.getDateFromFile();
        String dateToValue = propertiesForLdProject.getDateToName();
        String currencyValue = propertiesForLdProject.getCurrencyValue();

        String url = cbrUrl +
                dateFromValue + "=" + dateFrom +
                "&" +
                dateToValue + "=" + dateTo +
                "&" +
                currencyValue + "=" + curCodeCBR;

        return url;
    }
}