package LD.model.IFRSAccount;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "ifrs_account")
@NoArgsConstructor
@Setter
@Getter
@AllArgsConstructor
@Builder
@ToString
public class IFRSAccount
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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
}
