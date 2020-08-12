package LD.model.IFRSAccount;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IFRSAccountDTO_out
{
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

	private String user;

	private String lastChange;

	public static IFRSAccountDTO_out IFRSAccount_to_IFRSAccount_DTO_out(IFRSAccount ifrsAccount)
	{
		return IFRSAccountDTO_out.builder()
				.id(ifrsAccount.getId())
				.account_code(ifrsAccount.getAccount_code())
				.account_name(ifrsAccount.getAccount_name())
				.flow_code(ifrsAccount.getFlow_code())
				.flow_name(ifrsAccount.getFlow_name())
				.sh(ifrsAccount.getSh())
				.pa(ifrsAccount.getPa())
				.ct(ifrsAccount.getCt())
				.dr(ifrsAccount.getDr())
				.isInverseSum(ifrsAccount.isInverseSum())
				.mappingFormAndColumn(ifrsAccount.getMappingFormAndColumn())
				.user(ifrsAccount.getUser().getUsername())
				.lastChange(DateFormat.formatDate(ifrsAccount.getLastChange()))
				.build();
	}
}
