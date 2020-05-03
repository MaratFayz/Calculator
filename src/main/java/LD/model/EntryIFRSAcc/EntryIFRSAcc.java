package LD.model.EntryIFRSAcc;

import LD.model.Entry.Entry;
import LD.model.IFRSAccount.IFRSAccount;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "EntryIFRSAcc")
@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class EntryIFRSAcc
{
	@EmbeddedId
	private EntryIFRSAccID entryIFRSAccID;

	@Column(columnDefinition = "DECIMAL(30,10)")
	private BigDecimal sum;
}
