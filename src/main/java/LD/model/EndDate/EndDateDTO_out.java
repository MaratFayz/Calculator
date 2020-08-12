package LD.model.EndDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EndDateDTO_out
{
	private Long leasingDeposit_id;
	private Long scenario;
	private Long period;
	private String End_Date;
	private String user;
	private String lastChange;
}
