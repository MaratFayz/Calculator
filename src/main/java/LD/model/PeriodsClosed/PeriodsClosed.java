package LD.model.PeriodsClosed;

import LD.model.Enums.STATUS_X;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "PeriodsClosed")
@Getter
@Setter
@ToString
@NoArgsConstructor()
@Builder
@AllArgsConstructor
public class PeriodsClosed
{
	@EmbeddedId
	private PeriodsClosedID periodsClosedID;

	@Enumerated(value = EnumType.STRING)
	@Column(columnDefinition = "enum('X')")
	private STATUS_X ISCLOSED;

}
