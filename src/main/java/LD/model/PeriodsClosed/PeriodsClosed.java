package LD.model.PeriodsClosed;

import LD.config.Security.model.User.User;
import LD.model.Enums.STATUS_X;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "PeriodsClosed")
@Data
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class PeriodsClosed
{
	@EmbeddedId
	private PeriodsClosedID periodsClosedID;

	@Enumerated(value = EnumType.STRING)
	private STATUS_X ISCLOSED;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
