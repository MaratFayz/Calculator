package LD.model.EntryIFRSAcc;

import LD.config.Security.model.User.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "EntryIFRSAcc")
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class EntryIFRSAcc
{
	@EmbeddedId
	private EntryIFRSAccID entryIFRSAccID;

	@Column(columnDefinition = "DECIMAL(30,10)", scale = 10, precision = 30, nullable = false)
	private BigDecimal sum;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
