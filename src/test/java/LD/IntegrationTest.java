package LD;

import LD.model.DepositRate.DepositRateDTO_in;
import LD.model.EndDate.EndDateDTO_in;
import LD.model.Enums.STATUS_X;
import LD.model.LeasingDeposit.LeasingDepositDTO_in;
import LD.model.PeriodsClosed.PeriodsClosedDTO_in;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
public class IntegrationTest {

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext wac;

    // will be shared between test methods
    @Container
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:12")
            .withDatabaseName("leasingdepositsdb")
            .withUsername("postgres")
            .withPassword("ZZZXXX5!#~a");

    @Test
    void shouldReturnTrue_whenContainerIsRunning() {
        assertTrue(postgreSQLContainer.isRunning());
    }

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "superadmin", authorities = {"DEPOSIT_RATES_ADDER",
            "END_DATE_ADDER", "LEASING_DEPOSIT_ADDER", "AUTO_ADDING_PERIODS",
            "AUTO_CLOSING_PERIODS", "CALCULATE", "LOAD_EXCHANGE_RATE_FROM_CBR", "PERIODS_CLOSED_ADDER"})
    public void test1_for_several_LDs() throws Exception {
        assertDoesNotThrow(() -> {
//            template.
//            authorize();
            generatePeriods();
            closePeriods();
            addFirstOpenPeriod();
            importRatesFromCbr();
            addLd();
            addLdEndDate();
            addDepositRate();
            calculate();
        });
    }

    private void authorize() throws Exception {
        MvcResult ldResponse = this.mockMvc.perform(post("/login")
                .queryParam("login", "superadmin")
                .queryParam("password", "a"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void addDepositRate() throws Exception {
        DepositRateDTO_in depositRateDTO_in = DepositRateDTO_in.builder()
                .company(1L)
                .currency(2L)
                .duration(2L)
                .START_PERIOD("01.01.2019")
                .END_PERIOD("31.12.2020")
                .RATE(BigDecimal.valueOf(10))
                .scenario(1L)
                .build();

        JsonMapper jsonMapper = new JsonMapper();
        String dr = jsonMapper.writeValueAsString(depositRateDTO_in);

        MvcResult drResponse = this.mockMvc.perform(post("/depositRates")
                .content(dr)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void calculate() throws Exception {
        MvcResult entryResponse = this.mockMvc.perform(post("/entries/calculator")
                .queryParam("scenario_from", "1")
                .queryParam("scenario_to", "1"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void addLd() throws Exception {
        LeasingDepositDTO_in leasingDepositDTO_in = LeasingDepositDTO_in.builder()
                .company(1L)
                .counterpartner(1L)
                .currency(2L)
                .deposit_sum_not_disc(BigDecimal.valueOf(100000))
                .is_created(STATUS_X.X)
                .scenario(1L)
                .start_date("01.01.2019")
                .build();

        JsonMapper jsonMapper = new JsonMapper();
        String ld = jsonMapper.writeValueAsString(leasingDepositDTO_in);

        System.out.println(ld);
        MvcResult ldResponse = this.mockMvc.perform(post("/leasingDeposits")
                .content(ld)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void generatePeriods() throws Exception {
        MvcResult periodsCreated = this.mockMvc.perform(post("/periods/autoCreatePeriods")
                .queryParam("dateFrom", "01.01.2019")
                .queryParam("dateTo", "31.12.2020"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void closePeriods() throws Exception {
        MvcResult closedPeriods = this.mockMvc.perform(put("/periodsClosed/autoClosingPeriods")
                .queryParam("dateBeforeToClose", "30.06.2020")
                .queryParam("scenario_id", "1"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void addFirstOpenPeriod() throws Exception {
        PeriodsClosedDTO_in periodsClosedDTO_in = PeriodsClosedDTO_in.builder()
                .period(19L)
                .scenario(1L)
                .build();

        JsonMapper jsonMapper = new JsonMapper();
        String pc = jsonMapper.writeValueAsString(periodsClosedDTO_in);

        System.out.println(pc);

        MvcResult firstOpenPeriod = this.mockMvc.perform(post("/periodsClosed")
                .content(pc)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void importRatesFromCbr() throws Exception {
        MvcResult exRates = this.mockMvc.perform(post("/exchangeRates/importERFromCBR")
                .queryParam("scenario_id", "1")
                .queryParam("isAddOnlyNewestRates", "0"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }

    private void addLdEndDate() throws Exception {
        EndDateDTO_in endDateDTO_in = EndDateDTO_in.builder()
                .End_Date("30.12.2020")
                .leasingDeposit_id(1L)
                .period(1L)
                .scenario(1L)
                .build();

        JsonMapper jsonMapper = new JsonMapper();
        String s = jsonMapper.writeValueAsString(endDateDTO_in);

        System.out.println(s);

        MvcResult closedPeriods = this.mockMvc.perform(post("/endDates")
                .content(s)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isOk())
                .andReturn();
    }
}