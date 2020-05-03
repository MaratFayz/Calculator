package LD.model.PeriodsClosed;

import LD.model.Enums.STATUS_X;
import lombok.Data;

@Data
public class PeriodsClosedDTO
{
	private Long scenario;
	private Long period;

	private STATUS_X ISCLOSED;
}

