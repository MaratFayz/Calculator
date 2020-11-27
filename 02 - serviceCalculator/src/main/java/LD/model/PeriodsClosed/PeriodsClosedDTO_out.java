package LD.model.PeriodsClosed;

import LD.model.Enums.STATUS_X;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PeriodsClosedDTO_out {

    private Long scenario;
    private Long period;

    private String user;

    private String lastChange;

    private STATUS_X ISCLOSED;
}

