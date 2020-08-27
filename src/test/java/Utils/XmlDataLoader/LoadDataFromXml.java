package Utils.XmlDataLoader;

import Utils.TestDataKeeper;
import Utils.TestEntitiesKeeper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class LoadDataFromXml implements BeforeEachCallback, AfterEachCallback {

    public void beforeEach(ExtensionContext context) throws Exception {
        DataSource dataSource = new DataSource(context);

        System.out.println("<------------Test called '" + dataSource.getTestMethodName() + "' running------------>");
        System.out.println("Getting test data from ----> " + dataSource.getFileName());
        System.out.println("TestClass => " + dataSource.getTestInstance());
        System.out.println("And paste into variable ----> '" + dataSource.getField().getName() + "'");

        XmlMapper xmlMapper = new XmlMapper();
        FileInputStream fileInputStream = new FileInputStream(dataSource.getFileName());
        TestDataKeeper data = xmlMapper.readValue(fileInputStream, TestDataKeeper.class);
        TestEntitiesKeeper testEntitiesKeeper = new TestEntitiesKeeper(data);

        ReflectionUtils.makeAccessible(dataSource.getField());
        dataSource.getField().set(dataSource.getTestInstance(), testEntitiesKeeper);

        System.out.println("Before test = " + dataSource.getField().get(dataSource.getTestInstance()));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        DataSource dataSource = new DataSource(context);
        System.out.println("<------------Test called '" + dataSource.getTestMethodName() + "' finished------------>");
    }

    @Getter
    class DataSource {

        private ExtensionContext context;
        private Method testMethod;
        private String testMethodName;
        private String fileName;
        private Object testInstance;
        private Field field;

        private DataSource() {
        }

        public DataSource(ExtensionContext context) {
            this.context = context;

            testMethod = extractMethod();
            testMethodName = this.testMethod.getName();
            fileName = extractFileName();
            testInstance = extractTestClass();
            field = extractTestEntityField();
        }

        private Field extractTestEntityField() {
            Optional<Field> ofield = Arrays.stream(testInstance.getClass().getDeclaredFields()).filter(f -> f.getType().equals(TestEntitiesKeeper.class)).findAny();

            if (!ofield.isPresent()) {
                throw new IllegalStateException("There is no TestEntitiesKeeper field!");
            }

            return ofield.get();
        }

        private Object extractTestClass() {
            Optional<Object> otestInstance = context.getTestInstance();

            if (!otestInstance.isPresent()) {
                throw new IllegalStateException("There is no test class instance!");
            }

            return otestInstance.get();
        }

        private String extractFileName() {
            LoadXmlFileForLeasingDepositsTest annotation = testMethod.getAnnotation(LoadXmlFileForLeasingDepositsTest.class);
            return annotation.file();
        }

        private Method extractMethod() {
            Optional<Method> otestMethod = this.context.getTestMethod();

            if (!otestMethod.isPresent()) {
                throw new IllegalStateException("There is no test method!");
            }

            return otestMethod.get();
        }
    }
}
