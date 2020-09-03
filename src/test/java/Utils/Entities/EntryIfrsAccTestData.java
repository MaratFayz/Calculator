package Utils.Entities;

import LD.config.Security.model.User.User;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.Period.Period;
import LD.model.Scenario.Scenario;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class EntryIfrsAccTestData {

    private Long leasingDepositCode;
    private Long scenarioCode;
    private String period;
    private String calculation_time;

    private Long ifrsAccountCode;
    private BigDecimal sum;
    private Long userCode;
    private String lastChange;
}
