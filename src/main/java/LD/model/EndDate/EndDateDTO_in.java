package LD.model.EndDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndDateDTO_in
{
	private Long leasingDeposit_id;
	private Long scenario;
	private Long period;
	private String End_Date;
}
