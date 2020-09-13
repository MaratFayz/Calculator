package LD.model.EndDate;

import LD.config.Security.model.User.User;
import LD.model.LeasingDeposit.LeasingDeposit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "EndDate")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class EndDate
{
	@EmbeddedId
	EndDateID endDateID;

	@Column(name = "endDate", nullable = false)
	private ZonedDateTime endDate;

	@ManyToOne(cascade = CascadeType.ALL)
	@MapsId(value = "leasingDeposit_id")
	@JsonIgnore
	private LeasingDeposit leasingDeposit;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
