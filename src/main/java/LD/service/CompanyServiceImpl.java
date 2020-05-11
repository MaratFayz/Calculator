package LD.service;

import LD.model.Company.Company;
import LD.repository.CompanyRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService
{
	@Autowired
	CompanyRepository companyRepository;

	@Override
	public List<Company> getAllCompanies()
	{
		return companyRepository.findAll();
	}

	@Override
	public Company getCompany(Long id)
	{
		return companyRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public Company saveNewCompany(Company company)
	{
		return companyRepository.save(company);
	}

	@Override
	public Company updateCompany(Long id, Company company)
	{
		company.setId(id);

		Company companyToUpdate = getCompany(id);

		BeanUtils.copyProperties(company, companyToUpdate);

		companyRepository.saveAndFlush(companyToUpdate);

		return companyToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			companyRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
