package LD.model.EntryIFRSAcc;

import LD.model.Entry.Entry;
import LD.model.IFRSAccount.IFRSAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class EntryIFRSAccID implements Serializable
{
	static final Long serialVersionUID = 10L;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Entry entry;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private IFRSAccount ifrsAccount;

}
