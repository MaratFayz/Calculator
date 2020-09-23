package Utils.XmlDataLoader;

import Utils.TestDataKeeper;
import Utils.TestEntitiesKeeper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.FileInputStream;
import java.lang.reflect.Field;

import static Utils.Color.*;

public class LoadDataFromXml implements BeforeEachCallback, AfterEachCallback {

    private TestClassParser testClassParser;
    private TestEntitiesKeeper testEntitiesKeeper;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        parseTestClass(context);
        viewTestData();
        getTestEntityKeeperFromXmlFile();
        pasteTestEntityKeeperIntoTestClass();
    }

    @Override
    public void afterEach(ExtensionContext context) throws IllegalAccessException {
        parseTestClass(context);
        System.out.println("[" + ANSI_CYAN + "END" + ANSI_RESET + "]<------------Test called " + ANSI_YELLOW + testClassParser.getTestMethodName() + ANSI_RESET + " finished------------>");
        System.out.println("");
    }

    private void parseTestClass(ExtensionContext context) throws IllegalAccessException {
        testClassParser = TestClassParser.parse(context);
    }

    private void viewTestData() {
        System.out.println("");
        System.out.println("[" + ANSI_CYAN + "START" + ANSI_RESET + "] <------------Test called " + ANSI_YELLOW + testClassParser.getTestMethodName() + ANSI_RESET + " running------------>");
        System.out.println("Getting test data from ----> " + testClassParser.getFileNameWithTestData());
        System.out.println("And paste into variable ----> '" + testClassParser.getTestEntitiesKeeperField().getName() + "'");
    }

    private void getTestEntityKeeperFromXmlFile() throws java.io.IOException {
        XmlMapper xmlMapper = new XmlMapper();
        FileInputStream fileInputStream = new FileInputStream(testClassParser.getFileNameWithTestData());
        TestDataKeeper data = xmlMapper.readValue(fileInputStream, TestDataKeeper.class);
        testEntitiesKeeper = TestEntitiesKeeper.transformDataKeeperIntoEntitiesKeeper(data);
    }

    private void pasteTestEntityKeeperIntoTestClass() throws IllegalAccessException {
        Field testEntitiesKeeperField = testClassParser.getTestEntitiesKeeperField();
        ReflectionUtils.makeAccessible(testEntitiesKeeperField);
        testEntitiesKeeperField.set(testClassParser.getTestClassInstance(), testEntitiesKeeper);
    }
}
