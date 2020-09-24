package Utils.XmlDataLoader;

import Utils.TestEntitiesKeeper;
import lombok.Getter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

@Getter
class TestClassParser {

    private ExtensionContext context;
    private Method testMethod;
    private String testMethodName;
    private String fileNameWithTestData;
    private Object testClassInstance;
    private Field testEntitiesKeeperField;
    private TestEntityManager testEntityManager;

    public static TestClassParser parse(ExtensionContext context) throws IllegalAccessException {
        return new TestClassParser(context);
    }

    private TestClassParser(ExtensionContext context) throws IllegalAccessException {
        this.context = context;

        parseMethod();
        parseMethodName();
        parseFileNameWithTestData();
        parseTestClass();
        parseTestEntityKeeperField();
        parseTestEntityManager();
    }

    private void parseMethod() {
        Optional<Method> otestMethod = this.context.getTestMethod();

        if (otestMethod.isEmpty()) {
            throw new IllegalStateException("There is no test method!");
        }

        testMethod = otestMethod.get();
    }

    private void parseMethodName() {
        testMethodName = this.testMethod.getName();
    }

    private void parseFileNameWithTestData() {
        LoadXmlFileForLeasingDepositsTest annotation = testMethod.getAnnotation(LoadXmlFileForLeasingDepositsTest.class);
        fileNameWithTestData = annotation.file();
    }

    private void parseTestClass() {
        Optional<Object> otestInstance = context.getTestInstance();

        if (otestInstance.isEmpty()) {
            throw new IllegalStateException("There is no test class instance!");
        }

        testClassInstance = otestInstance.get();
    }

    private void parseTestEntityKeeperField() {
        Optional<Field> ofield = Arrays.stream(testClassInstance.getClass().getDeclaredFields())
                .filter(f -> f.getType().equals(TestEntitiesKeeper.class)).findAny();

        if (ofield.isEmpty()) {
            throw new IllegalStateException("There is no TestEntitiesKeeper field!");
        }

        testEntitiesKeeperField = ofield.get();
    }

    private void parseTestEntityManager() throws IllegalAccessException {
        Optional<Field> testEntityManagerField = Arrays.stream(testClassInstance.getClass().getDeclaredFields())
                .filter(f -> f.getType().equals(TestEntityManager.class)).findAny();

        if (testEntityManagerField.isEmpty()) {
            throw new IllegalStateException("There is no TestEntityManager field!");
        }

        ReflectionUtils.makeAccessible(testEntityManagerField.get());
        testEntityManager = (TestEntityManager) testEntityManagerField.get().get(this.testClassInstance);
    }
}
