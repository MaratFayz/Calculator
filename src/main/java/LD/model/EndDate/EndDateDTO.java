package LD.model.EndDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndDateDTO
{
	private Long leasingDeposit;
	private Long scenario;
	private Long period;
	private String End_Date;
}
