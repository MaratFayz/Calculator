package LD.model.IFRSAccount;

import lombok.Data;

@Data
public class IFRSAccountDTO
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

	public static IFRSAccount IFRSAccountDTO_to_IFRSAccount(IFRSAccountDTO ifrsAccountDTO)
	{
		return IFRSAccount.builder()
				.account_code(ifrsAccountDTO.account_code)
				.account_name(ifrsAccountDTO.account_name)
				.flow_code(ifrsAccountDTO.flow_code)
				.flow_name(ifrsAccountDTO.flow_name)
				.sh(ifrsAccountDTO.sh)
				.pa(ifrsAccountDTO.pa)
				.ct(ifrsAccountDTO.ct)
				.dr(ifrsAccountDTO.dr)
				.isInverseSum(ifrsAccountDTO.isInverseSum)
				.mappingFormAndColumn(ifrsAccountDTO.mappingFormAndColumn)
				.build();
	}
}
