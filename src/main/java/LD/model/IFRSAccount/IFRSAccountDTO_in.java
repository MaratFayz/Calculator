package LD.model.IFRSAccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IFRSAccountDTO_in
{
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

	public static IFRSAccount IFRSAccountDTO_in_to_IFRSAccount(IFRSAccountDTO_in ifrsAccountDTO_in)
	{
		return IFRSAccount.builder()
				.account_code(ifrsAccountDTO_in.account_code)
				.account_name(ifrsAccountDTO_in.account_name)
				.flow_code(ifrsAccountDTO_in.flow_code)
				.flow_name(ifrsAccountDTO_in.flow_name)
				.sh(ifrsAccountDTO_in.sh)
				.pa(ifrsAccountDTO_in.pa)
				.ct(ifrsAccountDTO_in.ct)
				.dr(ifrsAccountDTO_in.dr)
				.isInverseSum(ifrsAccountDTO_in.isInverseSum)
				.mappingFormAndColumn(ifrsAccountDTO_in.mappingFormAndColumn)
				.build();
	}
}
