package LD.model.EndDate;

import LD.model.LeasingDeposit.LeasingDeposit;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "EndDate")
@ToString
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EndDate
{
	@EmbeddedId
	EndDateID endDateID;

	@Column(name = "End_Date", nullable = false, columnDefinition = "DATE")
	private ZonedDateTime End_Date;

	@ManyToOne(cascade = CascadeType.ALL)
	@MapsId(value = "leasingDeposit_id")
	private LeasingDeposit leasingDeposit;
}
