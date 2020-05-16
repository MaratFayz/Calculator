package LD.model.Company;

import LD.config.Security.Repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDTO_in
{
	@Autowired
	UserRepository userRepository;

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
