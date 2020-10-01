package Utils.XmlDataLoader;

import Utils.TestEntitiesKeeper;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.ZonedDateTime;

import static Utils.Color.ANSI_CYAN;
import static Utils.Color.ANSI_RESET;

public class EntitiesIntoDatabaseSaver implements BeforeEachCallback {

    private TestClassParser testClassParser;
    private TestEntitiesKeeper testEntitiesKeeper;
    private TestEntityManager testEntityManager;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        parseTestClass(context);
        viewMetaData();
        getTestEntityManager();
        getTestEntitiesKeeper();
        saveDataIntoTestDatabase();
        viewEndSaving();
    }

    private void parseTestClass(ExtensionContext context) throws IllegalAccessException {
        testClassParser = TestClassParser.parse(context);
    }

    private void viewMetaData() {
        System.out.println("");
        System.out.println("[" + ANSI_CYAN + "START" + ANSI_RESET + "] <------------Saving into database starting------------>");
    }

    private void getTestEntityManager() {
        this.testEntityManager = testClassParser.getTestEntityManager();
    }

    private void getTestEntitiesKeeper() {
        this.testEntitiesKeeper = testClassParser.getTestEntitiesKeeper();
    }

    private void saveDataIntoTestDatabase() {
        setNullIntoUserId();
        setNullIntoCompanyId();
        setNullIntoCounterpartnerId();
        setNullIntoCurrencyId();
        setNullIntoScenarioId();
        setNullIntoDurationId();
        setNullIdPeriod();
        setLastChangeDateIntoPeriod();
        setNullIdIfrsAccount();
        setLastChangeDateIntoIfrsAccount();
        setNullIdIfrsAccount();
        setLastChangeDateIntoLeasingDeposit();
        setNullIdLeasingDeposit();
        setLastChangeDateIntoEntriesForIfrsSumDaoTest();
        setUserIntoEntries();

        testEntityManager.persistAndFlush(testEntitiesKeeper.getUser());
        testEntityManager.persistAndFlush(testEntitiesKeeper.getCompany());
        testEntityManager.persistAndFlush(testEntitiesKeeper.getCounterpartner());
        testEntitiesKeeper.getCurrencies().forEach(c -> c = testEntityManager.persistAndFlush(c));
        testEntitiesKeeper.getScenarios().forEach(s -> s = testEntityManager.persistAndFlush(s));
        testEntitiesKeeper.getDurations().forEach(d -> d = testEntityManager.persistAndFlush(d));
        testEntitiesKeeper.getDepositRates().forEach(dr -> dr = testEntityManager.persistAndFlush(dr));
        testEntitiesKeeper.getPeriods().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getEndDates().forEach(p -> p = testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getEntriesForIfrsSumDaoTest().forEach(p -> testEntityManager.persistAndFlush(p));
        testEntitiesKeeper.getEntriesIfrsForIfrsSumDaoTests().forEach(e -> e = testEntityManager.persistAndFlush(e));
    }

    private void setUserIntoEntries() {
        testEntitiesKeeper.getEntriesForIfrsSumDaoTest().forEach(e -> e.setUser(testEntitiesKeeper.getUser()));
    }

    private void setNullIntoUserId() {
        testEntitiesKeeper.getUser().setId(null);
    }

    private void setNullIntoCompanyId() {
        testEntitiesKeeper.getCompany().setId(null);
    }

    private void setNullIntoCounterpartnerId() {
        testEntitiesKeeper.getCounterpartner().setId(null);
    }

    private void setNullIntoCurrencyId() {
        testEntitiesKeeper.getCurrencies().forEach(cu -> cu.setId(null));
    }

    private void setNullIntoScenarioId() {
        testEntitiesKeeper.getScenarios().forEach(s -> s.setId(null));
    }

    private void setNullIntoDurationId() {
        testEntitiesKeeper.getDurations().forEach(d -> d.setId(null));
    }

    private void setNullIdPeriod() {
        testEntitiesKeeper.getPeriods().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoPeriod() {
        testEntitiesKeeper.getPeriods().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setNullIdIfrsAccount() {
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoIfrsAccount() {
        testEntitiesKeeper.getIfrsAccounts().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setNullIdLeasingDeposit() {
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p.setId(null));
    }

    private void setLastChangeDateIntoLeasingDeposit() {
        testEntitiesKeeper.getLeasingDeposits().forEach(p -> p.setLastChange(ZonedDateTime.now()));
    }

    private void setLastChangeDateIntoEntriesForIfrsSumDaoTest() {
        testEntitiesKeeper.getEntriesForIfrsSumDaoTest().forEach(e -> e.setLastChange(ZonedDateTime.now()));
    }

    private void viewEndSaving() {
        System.out.println("[" + ANSI_CYAN + "END" + ANSI_RESET + "]<------------Saving into database finished------------>");
        System.out.println("");
    }
}