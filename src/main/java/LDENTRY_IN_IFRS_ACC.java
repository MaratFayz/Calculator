import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "LDENTRY_IN_IFRS_ACC")
@NoArgsConstructor
@Getter
@Setter
@IdClass(value = LDENTRY_IN_IFRS_ACC.Key_IN_EntryIFRSAcc.class)
public class LDENTRY_IN_IFRS_ACC
{
	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private LD_ENTRY LDENTRY;

	@Id
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private IFRS_ACCOUNT account_flow;

	@Column(columnDefinition = "DECIMAL(30,10)")
	private BigDecimal sum;

	@Data
	public static class Key_IN_EntryIFRSAcc implements Serializable
	{
		static final long serialVersionUID = 10L;

		private LD_ENTRY LDENTRY;
		private IFRS_ACCOUNT account_flow;
	}

}
