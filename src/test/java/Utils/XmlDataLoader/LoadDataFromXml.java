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

public class LoadDataFromXml implements BeforeEachCallback, AfterEachCallback {

    private TestClassParser testClassParser;
    private TestEntitiesKeeper testEntitiesKeeper;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        parseTestClass(context);
        viewTestData();
        getTestEntityKeeperFromXmlFile();
        pasteTestEntityKeeperIntoTestClass();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        parseTestClass(context);
        System.out.println("[" + ANSI_CYAN + "END" + ANSI_RESET + "]<------------Test called " + ANSI_YELLOW + testClassParser.getTestMethodName() + ANSI_RESET + " finished------------>");
        System.out.println("");
    }

    private void pasteTestEntityKeeperIntoTestClass() throws IllegalAccessException {
        Field testEntitiesKeeperField = testClassParser.getTestEntitiesKeeperField();
        ReflectionUtils.makeAccessible(testEntitiesKeeperField);
        testEntitiesKeeperField.set(testClassParser.getTestClassInstance(), testEntitiesKeeper);
    }

    private void getTestEntityKeeperFromXmlFile() throws java.io.IOException {
        XmlMapper xmlMapper = new XmlMapper();
        FileInputStream fileInputStream = new FileInputStream(testClassParser.getFileNameWithTestData());
        TestDataKeeper data = xmlMapper.readValue(fileInputStream, TestDataKeeper.class);
        testEntitiesKeeper = TestEntitiesKeeper.transformDataKeeperIntoEntitiesKeeper(data);
    }

    private void parseTestClass(ExtensionContext context) {
        testClassParser = TestClassParser.parse(context);
    }

    private void viewTestData() {
        System.out.println("");
        System.out.println("[" + ANSI_CYAN + "START" + ANSI_RESET + "] <------------Test called " + ANSI_YELLOW + testClassParser.getTestMethodName() + ANSI_RESET + " running------------>");
        System.out.println("Getting test data from ----> " + testClassParser.getFileNameWithTestData());
        System.out.println("And paste into variable ----> '" + testClassParser.getTestEntitiesKeeperField().getName() + "'");
    }
}
