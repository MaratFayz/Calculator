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

import static java.util.Objects.nonNull;

@Getter
class TestClassParser {

    private ExtensionContext context;
    private Method testMethod;
    private String testMethodName;
    private String fileNameWithTestData;
    private Object testClassInstance;
    private Field testEntitiesKeeperField;
    private TestEntityManager testEntityManager;
    private TestEntitiesKeeper testEntitiesKeeper;

    public static TestClassParser parse(ExtensionContext context) throws IllegalAccessException {
        return new TestClassParser(context);
    }

    private TestClassParser(ExtensionContext context) throws IllegalAccessException {
        this.context = context;

        parseTestMethod();
        parseTestMethodName();
        parseFileNameWithTestData();
        parseTestClass();
        parseTestEntityKeeperField();
        parseTestEntityKeeper();
        parseTestEntityManager();
    }

    private void parseTestMethod() {
        Optional<Method> otestMethod = this.context.getTestMethod();

        if (otestMethod.isEmpty()) {
            throw new IllegalStateException("There is no test method!");
        }

        testMethod = otestMethod.get();
    }

    private void parseTestMethodName() {
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

    private void parseTestEntityKeeper() throws IllegalAccessException {
        ReflectionUtils.makeAccessible(testEntitiesKeeperField);
        testEntitiesKeeper = (TestEntitiesKeeper) testEntitiesKeeperField.get(this.testClassInstance);
    }

    private void parseTestEntityManager() throws IllegalAccessException {
        SaveEntitiesIntoDatabase annotation = testMethod.getAnnotation(SaveEntitiesIntoDatabase.class);

        if (nonNull(annotation)) {
            Optional<Field> testEntityManagerField = Arrays.stream(testClassInstance.getClass().getDeclaredFields())
                    .filter(f -> f.getType().equals(TestEntityManager.class)).findAny();

            if (testEntityManagerField.isEmpty()) {
                throw new IllegalStateException("There is no TestEntityManager field!");
            }

            ReflectionUtils.makeAccessible(testEntityManagerField.get());
            testEntityManager = (TestEntityManager) testEntityManagerField.get().get(this.testClassInstance);
        }
    }
}