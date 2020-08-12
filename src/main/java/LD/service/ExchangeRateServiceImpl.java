package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Currency.Currency;
import LD.model.ExchangeRate.ExchangeRate;
import LD.model.ExchangeRate.ExchangeRateDTO_out;
import LD.model.ExchangeRate.ExchangeRateID;
import LD.model.ExchangeRate.ExchangeRateTransform;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import LD.repository.*;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ExchangeRateServiceImpl implements ExchangeRateService
{
	@Autowired
	ExchangeRateRepository exchangeRateRepository;
	@Autowired
	ExchangeRateTransform exchangeRateTransform;
	@Autowired
	UserRepository userRepository;
	@Autowired
	CurrencyRepository currencyRepository;
	@Autowired
	PeriodRepository periodRepository;
	@Autowired
	PeriodsClosedRepository periodsClosedRepository;
	@Autowired
	ScenarioRepository scenarioRepository;

	@Override
	public List<ExchangeRateDTO_out> getAllExchangeRates()
	{
		List<ExchangeRate> resultFormDB = exchangeRateRepository.findAll();
		List<ExchangeRateDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new ExchangeRateDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(er -> exchangeRateTransform.ExchangeRate_to_ExchangeRateDTO_out(er))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public ExchangeRate getExchangeRate(ExchangeRateID id)
	{
		return exchangeRateRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public ExchangeRate saveNewExchangeRate(ExchangeRate exchangeRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		exchangeRate.setUser(userRepository.findByUsername(username));

		exchangeRate.setLastChange(ZonedDateTime.now());

		log.info("Валютный курс для сохранения = {}", exchangeRate);

		return exchangeRateRepository.save(exchangeRate);
	}

	@Override
	public ExchangeRate updateExchangeRate(ExchangeRateID id, ExchangeRate exchangeRate)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		exchangeRate.setUser(userRepository.findByUsername(username));

		exchangeRate.setLastChange(ZonedDateTime.now());

		ExchangeRate exchangeRateToUpdate = getExchangeRate(id);

		BeanUtils.copyProperties(exchangeRate, exchangeRateToUpdate);

		exchangeRateRepository.saveAndFlush(exchangeRateToUpdate);

		return exchangeRateToUpdate;
	}

	@Override
	public boolean delete(ExchangeRateID id)
	{
		try
		{
			exchangeRateRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

	@Override
	public void importExchangeRatesFormCBR(long scenario_id, boolean isAddOnlyNewestRates)
	{
		//перечень валют с кодом ЦБ
		List<Currency> currenciesWithCBRCode = currencyRepository.findByCBRCurrencyCodeNotNull();

		log.info("Перечень валют, которые имеют код ЦБ => {}", currenciesWithCBRCode);

		//самая ранняя дата периодов в базе данных (с неё будет загружаться информация с ЦБ)
		Optional<Period> minPeriod = periodRepository.findAll().stream().min(Comparator.comparing(Period::getDate));
		ZonedDateTime minPeriodDate;

		if(minPeriod.isPresent())
			minPeriodDate = minPeriod.get().getDate().withDayOfMonth(1);
		else
		{
			log.info("Наименьшая дата периода не представлена в справочнике периодов => завершение программы");
			return;
		}

		log.info("Наименьшая дата (1е число) в справочнике периодов => {}", minPeriodDate);

		//самая поздняя дата периодов в базе данных (по неё будет загружаться информация с ЦБ)
		Optional<Period> maxPeriod = periodRepository.findAll().stream().max(Comparator.comparing(Period::getDate));
		ZonedDateTime maxPeriodDate;

		if(maxPeriod.isPresent())
			maxPeriodDate = maxPeriod.get().getDate().plusDays(1);
		else
		{
			log.info("Наибольшая дата периода не представлена в справочнике периодов => завершение программы");
			return;
		}

		log.info("Наибольшая дата в справочнике периодов => {}", maxPeriodDate);

		//определим, за какие даты в базе данных есть курсы
		HashMap<Currency, List<ExchangeRate>> CurExRates= new HashMap<>();

		currenciesWithCBRCode.stream().forEach(currency -> {
			List<ExchangeRate> exchangeRates = exchangeRateRepository.findAll().stream()
					.filter(er -> er.getExchangeRateID().getScenario().getId().equals(scenario_id))
					.filter(er -> er.getExchangeRateID().getCurrency().equals(currency))
					.collect(Collectors.toList());

			CurExRates.put(currency, exchangeRates);
		});

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User loadingUser = userRepository.findByUsername(username);
		Scenario loadingScenario = scenarioRepository.findById(scenario_id).get();

		CurExRates.keySet().stream().forEach(currency ->
		{
			if(isAddOnlyNewestRates)
			{
				Optional<ExchangeRate> maxCurEx = CurExRates.get(currency).stream()
						.max(Comparator.comparing(er -> er.getExchangeRateID().getDate()));

				ZonedDateTime prevDayBeforeNow = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));

				if (maxCurEx.isPresent())
				{
					ZonedDateTime maxCurExDate = maxCurEx.get().getExchangeRateID().getDate();

					if (maxPeriodDate.isAfter(maxCurExDate) && prevDayBeforeNow.isAfter(maxCurExDate))
					{
						ZonedDateTime saveFromDate = maxCurEx.get().getExchangeRateID().getDate().plusDays(1);

						getCurExRateFrmCBRAndSaveIntoDB(loadingUser, loadingScenario, currency,
								saveFromDate, maxPeriodDate);
					}
					else
					{
						log.info("Даты равны: Максимальная дата курсов валют есть = {}, " +
								"дата макс периода = {}, дата на момент расчета = {}; расчет проводиться не будет",
								maxCurEx, maxPeriodDate, prevDayBeforeNow);
					}
				}
				else
				{
					log.info("Курсов валют в базе не представлено, начинается загрузка с нуля");
					deleteDownloadSaveCurExFormCBR(minPeriodDate, maxPeriodDate, loadingUser, loadingScenario, currency);
				}
			}
			else
			{
				log.info("Требуется всё удалить и загрузить по новой");
				deleteDownloadSaveCurExFormCBR(minPeriodDate, maxPeriodDate, loadingUser, loadingScenario, currency);
			}
		});
	}

	private void deleteDownloadSaveCurExFormCBR(ZonedDateTime minPeriodDate, ZonedDateTime maxPeriodDate, User loadingUser, Scenario loadingScenario, Currency currency)
	{
		List<ExchangeRate> toDeleteExR = exchangeRateRepository.findAll((root, query, qb) -> qb.equal(root.get("exchangeRateID").get("currency"), currency));
		exchangeRateRepository.deleteAll(toDeleteExR);

		ZonedDateTime queryDateFrom = minPeriodDate;
		ZonedDateTime queryDateTo = maxPeriodDate;

		getCurExRateFrmCBRAndSaveIntoDB(loadingUser, loadingScenario, currency, queryDateFrom, queryDateTo);
	}

	private void getCurExRateFrmCBRAndSaveIntoDB(User loadingUser, Scenario loadingScenario, Currency currency,
												 ZonedDateTime queryDateFrom, ZonedDateTime queryDateTo)
	{
		ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"));
		queryDateTo = now.isBefore(queryDateTo) ? now : queryDateTo;

		ZonedDateTime saveFromDate = queryDateFrom;
		ZonedDateTime saveTillDate = queryDateTo;

		queryDateFrom = queryDateFrom.minusMonths(1);

		TreeMap<ZonedDateTime, BigDecimal> exRatesFormCBR =
			getExRatesFromCBR(queryDateFrom, queryDateTo, currency);

		saveFromDate.toLocalDate().datesUntil(saveTillDate.toLocalDate().plusDays(1), java.time.Period.ofDays(1))
				.forEach(date ->
		{
			ZonedDateTime dateZDT = ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("UTC"));

			ExchangeRateID exRId = ExchangeRateID.builder()
					.scenario(loadingScenario)
					.currency(currency)
					.date(dateZDT)
					.build();

			ExchangeRate exR = ExchangeRate.builder()
					.exchangeRateID(exRId)
					.user(loadingUser)
					.lastChange(ZonedDateTime.now())
					.rate_at_date(exRatesFormCBR.floorEntry(dateZDT).getValue())
					.build();

			exR = exchangeRateRepository.save(exR);

			LocalDate lastDayOfDateMonth = date.withDayOfMonth(date.lengthOfMonth());
			BigDecimal avgRate = BigDecimal.ZERO;
			if(date.getDayOfMonth() == lastDayOfDateMonth.getDayOfMonth())
			{
				for(int day = 1; day <= lastDayOfDateMonth.getDayOfMonth(); day++)
				{
					ZonedDateTime z = ZonedDateTime.of(date.withDayOfMonth(day), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
					BigDecimal exRateForDay = exRatesFormCBR.floorEntry(z).getValue();
					avgRate = avgRate.add(exRateForDay);
				}

				avgRate = avgRate.divide(BigDecimal.valueOf(lastDayOfDateMonth.getDayOfMonth()), RoundingMode.HALF_UP);
				exR.setAverage_rate_for_month(avgRate);

				exchangeRateRepository.save(exR);
			}

		});
	}

	private TreeMap<ZonedDateTime, BigDecimal> getExRatesFromCBR(ZonedDateTime queryDateFrom,
																 ZonedDateTime queryDateTo,
																 Currency currency)
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String dateFrom = queryDateFrom.format(dtf); //"01/01/2020";
		String dateTo = queryDateTo.format(dtf); //"10/01/2020";
		String curCodeCBR = currency.getCBRCurrencyCode();

		Document doc = null;
		try
		{
			doc = Jsoup.connect("http://www.cbr.ru/scripts/XML_dynamic.asp?date_req1=" +
					dateFrom + "&date_req2=" + dateTo + "&VAL_NM_RQ=" + curCodeCBR).get();

			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

			TreeMap<ZonedDateTime, BigDecimal> currencyRates = new TreeMap<>();

			doc.select("Record").forEach(element -> {
				LocalDate Date_curExRate = LocalDate.parse(element.attr("Date"), dateTimeFormatter);
				log.info("Date_curExRate = " + Date_curExRate);
				ZonedDateTime DateTime_curExRate = ZonedDateTime.of(Date_curExRate, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
				log.info("DateTime_curExRate = " + DateTime_curExRate);

				BigDecimal ExRateForDate = BigDecimal.valueOf(Double.parseDouble(
						element.select("Value").text().replace(",", ".")));
				log.info("exRate = " + ExRateForDate);

				currencyRates.put(DateTime_curExRate, ExRateForDate);
			});

			return currencyRates;
		}
		catch (IOException e)
		{
			log.info(e);
			return null;
		}
	}
}
