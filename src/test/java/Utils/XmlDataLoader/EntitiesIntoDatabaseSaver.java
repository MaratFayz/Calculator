package Utils.XmlDataLoader;

import LD.config.Security.model.User.User;
import Utils.TestEntitiesKeeper;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static Utils.Color.*;

public class EntitiesIntoDatabaseSaver implements BeforeEachCallback {

    private TestClassParser testClassParser;
    private TestEntitiesKeeper testEntitiesKeeper;
    private TestEntityManager testEntityManager;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        parseTestClass(context);
        viewTestData();
        getTestEntityManager();
        saveUserIntoDatabase();
    }

    private void parseTestClass(ExtensionContext context) throws IllegalAccessException {
        testClassParser = TestClassParser.parse(context);
    }

    private void viewTestData() {
        System.out.println("");
        System.out.println("[" + ANSI_CYAN + "START" + ANSI_RESET + "] <------------Saving into database starts running------------>");
    }

    private void getTestEntityManager() {
        this.testEntityManager = testClassParser.getTestEntityManager();
    }

    private void saveUserIntoDatabase() {
        User user = testEntitiesKeeper.getUser();
        setNullIntoUserId(user);
        testEntityManager.persistAndFlush(user);
    }

    private void setNullIntoUserId(User user) {
        user.setId(null);
    }

    private void saveDepositRatesIntoTestDatabase() {
        setNullIntoCompanyId();
        setNullIntoCurrencyId();
        setNullIntoScenarioId();
        setNullIntoDurationId();

        testEntityManager.persistAndFlush(testEntitiesKeeper.getCompany());
        testEntitiesKeeper.getCurrencies().forEach(c -> testEntityManager.persistAndFlush(c));
        testEntitiesKeeper.getScenarios().forEach(s -> testEntityManager.persistAndFlush(s));
        testEntitiesKeeper.getDurations().forEach(d -> testEntityManager.persistAndFlush(d));
        testEntitiesKeeper.getDepositRates().forEach(dr -> testEntityManager.persistAndFlush(dr));
    }



    private void setNullIntoCompanyId() {
        testEntitiesKeeper.getCompany().setId(null);
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


}
