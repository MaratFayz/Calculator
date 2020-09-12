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

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        parseTestClass(context);
        viewTestData();
        parseTestEntityKeeperFromXmlFile();
        pasteTestEntityKeeperIntoTestClassField();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        parseTestClass(context);
        System.out.println("<------------Test called '" + testClassParser.getTestMethodName() + "' finished------------>");
    }

    private void pasteTestEntityKeeperIntoTestClassField() throws IllegalAccessException {
        Field testEntitiesKeeperField = testClassParser.getTestEntitiesKeeperField();
        ReflectionUtils.makeAccessible(testEntitiesKeeperField);
        testEntitiesKeeperField.set(testClassParser.getTestClassInstance(), testEntitiesKeeper);
    }

    private void parseTestEntityKeeperFromXmlFile() throws java.io.IOException {
        XmlMapper xmlMapper = new XmlMapper();
        FileInputStream fileInputStream = new FileInputStream(testClassParser.getFileNameWithTestData());
        TestDataKeeper data = xmlMapper.readValue(fileInputStream, TestDataKeeper.class);
        testEntitiesKeeper = TestEntitiesKeeper.transformDataKeeperIntoEntitiesKeeper(data);
    }

    private void parseTestClass(ExtensionContext context) {
        testClassParser = TestClassParser.parse(context);
    }

    private void viewTestData() {
        System.out.println("<------------Test called '" + testClassParser.getTestMethodName() + "' running------------>");
        System.out.println("Getting test data from ----> " + testClassParser.getFileNameWithTestData());
        System.out.println("And paste into variable ----> '" + testClassParser.getTestEntitiesKeeperField().getName() + "'");
    }
}
