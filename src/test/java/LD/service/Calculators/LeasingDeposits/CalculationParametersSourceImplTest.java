package LD.service.Calculators.LeasingDeposits;

import LD.config.Security.Repository.UserRepository;
import LD.config.Security.model.User.User;
import LD.model.Enums.ScenarioStornoStatus;
import LD.model.Scenario.Scenario;
import LD.repository.IFRSAccountRepository;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static Utils.Builders.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {CalculationParametersSourceImpl.class})
public class CalculationParametersSourceImplTest {

    @Autowired
    private CalculationParametersSource cps;
    @MockBean
    ScenarioRepository scenarioRepository;
    @MockBean
    private PeriodsClosedRepository periodsClosedRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private IFRSAccountRepository ifrsAccountRepository;

    @MockBean
    SecurityContext securityContext;
    @MockBean
    Authentication authentication;

    static User user = getAnyUser();

    @BeforeEach
    public void setUp() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        String user = "User-1";
        when(authentication.getName()).thenReturn(user);
        when(userRepository.findByUsername(eq(user))).thenReturn(new User());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateMinScenariosDifferAndScenarioFromAdditionScenarioToAddition() {
        //ADD -> ADD для разных сценариев
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationAddition = getSC("SCENARIO_D_A", ScenarioStornoStatus.ADDITION, user);

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Запрещённая операция расчёта между разными сценариями со статусами: ADDITION -> ADDITION", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldNotThrowException_whenCopyDateMinScenariosEqualAndScenarioFromToAddition() {
        //ADD -> ADD для одного сценария
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 1L;

        Scenario scenarioSourceAddition = getSC("SCENARIO", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationAddition = scenarioSourceAddition;

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationAddition))).thenReturn(LocalDate.of(2019, 11, 30));

        assertDoesNotThrow(() -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });
    }

    @Test
    public void prepareParameters_shouldNotThrowException_whenCopyDateMinScenariosDifferAndScenarioFromAdditionScenarioToFull() {
        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(eq(scenarioSource))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestination))).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        assertDoesNotThrow(() -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSource, scenarioDestination);
        });
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateMinScenariosDifferAndScenarioFromFullScenarioToAddition() {
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceFull = getSC("SCENARIO_S_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationAddition = getSC("SCENARIO_D_A", ScenarioStornoStatus.ADDITION, user);

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceFull));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationAddition));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceFull))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationAddition))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Запрещённая операция расчёта между разными сценариями со статусами: FULL -> ADDITION", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateMinScenariosDifferAndScenarioFromFullScenarioToFull() {
        //разные сценарии -> ошибка
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceFull = getSC("SCENARIO_S_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(scenarioSourceId)).thenReturn(Optional.ofNullable(scenarioSourceFull));
        when(scenarioRepository.findById(scenarioDestinationId)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceFull))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Запрещённая операция расчёта между разными сценариями со статусами: FULL -> FULL", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateMinScenariosEqualAndScenarioFromToFull() {
        //разные сценарии -> ошибка
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 1L;

        Scenario scenarioSourceFull = getSC("SCENARIO_F", ScenarioStornoStatus.FULL, user);
        Scenario scenarioDestinationFull = scenarioSourceFull;

        when(scenarioRepository.findById(scenarioSourceId)).thenReturn(Optional.ofNullable(scenarioSourceFull));
        when(scenarioRepository.findById(scenarioDestinationId)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceFull))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Запрещённая операция расчёта между одним сценарием со статусом: FULL -> FULL", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateEqualsFirstOpenPeriodAndFirstOpenPeriodsOfTwoScenariosEqual() {
        LocalDate copyDate = getDate(30, 11, 2019);

        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(scenarioSourceId)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(scenarioDestinationId)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(copyDate, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Дата начала копирования со сценария-источника всегда " +
                "должна быть меньше любого первого открытого периода каждого из двух сценариев, либо равна LocalDate.MIN", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateGreaterFirstOpenPeriodAndFirstOpenPeriodsOfTwoScenariosEqual() {
        LocalDate copyDate = getDate(31, 12, 2019);

        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(scenarioSourceId)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(scenarioDestinationId)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(copyDate, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Дата начала копирования со сценария-источника всегда должна быть " +
                "меньше любого первого открытого периода каждого из двух сценариев, либо равна LocalDate.MIN", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldNotThrowException_whenCopyDateLessFirstOpenPeriodAndFirstOpenPeriodsOfTwoDifferentScenariosEqual() {
        LocalDate copyDate = getDate(31, 10, 2019);

        long scenarioSource = 1L;
        long scenarioDestination = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(scenarioSource)).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(scenarioDestination)).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        assertDoesNotThrow(() -> {
            cps.prepareParameters(copyDate, scenarioSource, scenarioDestination);
        });
    }

    @Test
    public void prepareParameters_shouldNotThrowException_whenCopyDateMinFirstOpenPeriodScenarioSourceLessThanFirstOpenPeriodScenarioDestination() {
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 10, 31));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        assertDoesNotThrow(() -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });
    }

    @Test
    public void prepareParameters_shouldNotThrowException_whenCopyDateMinScenariosDifferentFirstOpenPeriodsEqual() {
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 11, 30));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        assertDoesNotThrow(() -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateMinScenariosDifferentFirstOpenPeriodScenarioSourceIsGreaterThanFirstOpenPeriodOfScenarioDestination() {
        long scenarioSourceId = 1L;
        long scenarioDestinationId = 2L;

        Scenario scenarioSourceAddition = getSC("SCENARIO_S_A", ScenarioStornoStatus.ADDITION, user);
        Scenario scenarioDestinationFull = getSC("SCENARIO_D_F", ScenarioStornoStatus.FULL, user);

        when(scenarioRepository.findById(eq(scenarioSourceId))).thenReturn(Optional.ofNullable(scenarioSourceAddition));
        when(scenarioRepository.findById(eq(scenarioDestinationId))).thenReturn(Optional.ofNullable(scenarioDestinationFull));

        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioSourceAddition))).thenReturn(LocalDate.of(2019, 12, 31));
        when(periodsClosedRepository.findFirstOpenPeriodDateByScenario(eq(scenarioDestinationFull))).thenReturn(LocalDate.of(2019, 11, 30));

        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(LocalDate.MIN, scenarioSourceId, scenarioDestinationId);
        });

        assertEquals("Дата первого открытого периода сценария-источника всегда должна быть меньше " +
                "ИЛИ равно первому открытому периоду сценария-получателя", thrown.getMessage());
    }

    @Test
    public void prepareParameters_shouldThrowException_whenCopyDateIsNull() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            cps.prepareParameters(null, 1L, 2L);
        });

        assertEquals("Wrong! copyDate equals null!", exception.getMessage());
    }
}