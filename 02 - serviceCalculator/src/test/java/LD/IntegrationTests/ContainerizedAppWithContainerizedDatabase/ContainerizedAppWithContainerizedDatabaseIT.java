package LD.IntegrationTests.ContainerizedAppWithContainerizedDatabase;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
@Log4j2
public class ContainerizedAppWithContainerizedDatabaseIT {

    Network databaseNetwork = Network.newNetwork();

    String imageName;

    GenericContainer postgreSQLContainer;

    GenericContainer applicationContainer;

    @BeforeEach
    void runContainers() {
        getBuiltImageNameFromApplicationProperties();

        System.out.println("name = " + imageName);

        postgreSQLContainer = new PostgreSQLContainer("postgres:12")
                .withDatabaseName("leasingdepositsdb")
                .withUsername("postgres")
                .withPassword("ZZZXXX5!#~a")
                .withExposedPorts(5432)
                .withNetwork(databaseNetwork)
                .withNetworkAliases("database");

        postgreSQLContainer.start();

        applicationContainer = new GenericContainer(imageName)
                .withExposedPorts(8080)
                .withNetwork(databaseNetwork)
                .dependsOn(postgreSQLContainer);

        applicationContainer.start();
    }

    @SneakyThrows
    void getBuiltImageNameFromApplicationProperties() {
        String applicationPropertiesPath = Thread.currentThread().getContextClassLoader().getResource("").getPath()
                .replaceAll("%20", " ")
                .concat("application.properties");

        log.info("Поиск файла application.properties по адресу: {}", applicationPropertiesPath);

        Properties appProps = new Properties();
        appProps.load(new FileInputStream(applicationPropertiesPath));

        imageName = (String) appProps.get("jib.dockerImagePath");
        log.info("Название образа, полученного из переменной jib.dockerImagePath файла: {}", imageName);
    }

    @Test
    void getExcelReport_shouldNotThrowException_whenThereAreData() {
        String url = "http://" + applicationContainer.getContainerIpAddress() + ":" + applicationContainer.getFirstMappedPort();
        log.info("Ip application => {}", url);

        assertDoesNotThrow(() -> {
            WebTestClient
                    .bindToServer()
                    .baseUrl(url)
                    .build()
                    .get()
                    .uri("/excelReports/ld_regld1_2_3?scenarioFromId=1&scenarioToId=1")
                    .exchange()
                    .expectStatus().is2xxSuccessful()
                    .expectHeader().valueEquals("Content-Type", "application/vnd.ms-excel")
                    .expectBody();
        });
    }
}