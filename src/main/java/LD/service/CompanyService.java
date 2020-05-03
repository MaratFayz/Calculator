package LD.service;

import LD.model.Company.Company;

import java.util.List;

public interface CompanyService
{
	List<Company> getAllCompanies();

	Company getCompany(Long id);

	Company saveNewCompany(Company company);

	Company updateCompany(Long id, Company company);

	boolean delete(Long id);
}
