package LD.model.Company;

import LD.config.DateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CompanyDTO_out
{
	private Long id;

	private String code;

	private String name;

	private String user;

	private String lastChange;

	public static CompanyDTO_out Company_to_CompanyDTO_out(Company company)
	{
		return CompanyDTO_out.builder()
				.id(company.getId())
				.code(company.getCode())
				.name(company.getName())
				.user(company.getUser().getUsername())
				.lastChange(DateFormat.formatDate(company.getLastChange()))
				.build();
	}
}
