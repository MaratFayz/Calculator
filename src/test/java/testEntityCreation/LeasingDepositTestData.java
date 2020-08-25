package testEntityCreation;

import LD.model.Enums.STATUS_X;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeasingDepositTestData {

    Long id;
    Long companyCode;
    Long counterpartnerCode;
    Long currencyCode;
    String start_date;
    BigDecimal deposit_sum_not_disc;
    Long scenarioCode;
    STATUS_X is_created;
    STATUS_X is_deleted;
    Long userCode;
    String lastChange;
}
