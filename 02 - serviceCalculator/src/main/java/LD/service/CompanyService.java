package LD.service;

import LD.model.Company.Company;
import LD.model.Company.CompanyDTO_out;

import java.util.List;

public interface CompanyService
{
	List<CompanyDTO_out> getAllCompanies();

	Company getCompany(Long id);

	Company saveNewCompany(Company company);

	Company updateCompany(Long id, Company company);

	void delete(Long id);
}
