package Utils;

import Utils.Entities.*;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "testData")
public class TestDataKeeper {

    UserTestData user;
    CompanyTestData company;
    List<ScenarioTestData> scenarios;
    List<CurrencyTestData> currencies;
    CounterpartnerTestData counterpartner;
    String periods_start;
    String periods_end;
    List<DurationTestData> durations;
    List<DepositRateTestData> depositRates;
    List<LeasingDepositTestData> leasingDeposits;
    List<EndDateTestData> end_dates;
    List<EntryTestData> entries_into_leasingDeposit;
    List<EntryTestData> entries_expected;
    List<IfrsAccountTestData> ifrsAccounts;
    List<EntryIfrsAccTestData> entriesIfrsExcepted;
    List<ExchangeRateTestData> exchangeRates;
}
