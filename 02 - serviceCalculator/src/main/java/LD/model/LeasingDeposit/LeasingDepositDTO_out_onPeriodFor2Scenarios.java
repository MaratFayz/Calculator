package LD.model.LeasingDeposit;

import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeasingDepositDTO_out_onPeriodFor2Scenarios {

    private Long id;
    private BigDecimal deposit_sum_not_disc;
    private String scenario;
    private String currency;
    private String start_date;
    private String periodOfChangingEndDate;
    private String endDate;
    private String company;
    private String counterpartner;
    private STATUS_X is_created;
    private STATUS_X is_deleted;
    private String username;
    private String lastChange;
}