package Utils.Entities;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EntryIfrsAccTestData {

    private Long ifrsAccountCode;
    private BigDecimal sum;
    private Long userCode;
    private String lastChange;

    //необходимо для привязки EntryIfrs -> Entry через код депозита
    // при этом предполагается, что у депозита берется лишь первая запись
    // поэтому достаточно 1 запись на депозит!
    private long leasingDepositCode;
}
