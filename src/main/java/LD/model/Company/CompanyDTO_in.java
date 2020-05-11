package LD.model.Company;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyDTO_in
{
	private String code;

	private String name;

	public static Company CompanyDTO_in_to_Company(CompanyDTO_in companyDTO_in)
	{
		return Company.builder()
				.code(companyDTO_in.getCode())
				.name(companyDTO_in.getName())
				.build();
	}
}
