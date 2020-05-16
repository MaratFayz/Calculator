package LD.model.IFRSAccount;

import LD.config.Security.model.User.User;
import lombok.*;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "ifrs_account")
@NoArgsConstructor
@Data
@AllArgsConstructor()
@Builder
public class IFRSAccount
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	public IFRSAccount(Long id, String account_code, String account_name, String flow_code, String flow_name, String sh, String pa, String ct, String dr, boolean isInverseSum, String mappingFormAndColumn)
	{
		this.id = id;
		this.account_code = account_code;
		this.account_name = account_name;
		this.flow_code = flow_code;
		this.flow_name = flow_name;
		this.sh = sh;
		this.pa = pa;
		this.ct = ct;
		this.dr = dr;
		this.isInverseSum = isInverseSum;
		this.mappingFormAndColumn = mappingFormAndColumn;
	}

	private String account_code;
	private String account_name;
	private String flow_code;
	private String flow_name;

	private String sh;
	private String pa;
	private String ct;
	private String dr;

	private boolean isInverseSum;

	private String mappingFormAndColumn;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private User user;

	@Column(name = "DateTime_lastChange", nullable = false)
	private ZonedDateTime lastChange;
}
