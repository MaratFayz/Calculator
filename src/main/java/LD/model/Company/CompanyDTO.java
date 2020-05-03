package LD.model.Company;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyDTO
{
	private Long id;

	private String code;

	private String name;

	public static Company CompanyDTO_to_Company(CompanyDTO companyDTO)
	{
		return Company.builder()
				.id(companyDTO.getId())
				.code(companyDTO.getCode())
				.name(companyDTO.getName())
				.build();
	}
}
