package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.Company.Company;
import LD.model.Company.CompanyDTO_out;
import LD.repository.CompanyRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CompanyServiceImpl implements CompanyService
{
	@Autowired
	CompanyRepository companyRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<CompanyDTO_out> getAllCompanies()
	{
		List<Company> resultFormDB = companyRepository.findAll();
		List<CompanyDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new CompanyDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> CompanyDTO_out.Company_to_CompanyDTO_out(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public Company getCompany(Long id)
	{
		return companyRepository.findById(id)
				.orElseThrow(NotFoundException::new);
	}

	@Override
	public Company saveNewCompany(Company company)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		company.setUser(userRepository.findByUsername(username));

		company.setLastChange(ZonedDateTime.now());

		log.info("Компания для сохранения = {}", company);

		return companyRepository.save(company);
	}

	@Override
	public Company updateCompany(Long id, Company company)
	{
		company.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		company.setUser(userRepository.findByUsername(username));

		company.setLastChange(ZonedDateTime.now());

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
