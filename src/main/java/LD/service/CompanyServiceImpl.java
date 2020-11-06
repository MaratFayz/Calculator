package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.Company.Company;
import LD.model.Company.CompanyDTO_out;
import LD.repository.CompanyRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<CompanyDTO_out> getAllCompanies() {
        List<Company> resultFormDB = companyRepository.findAll();
        List<CompanyDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new CompanyDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> CompanyDTO_out.Company_to_CompanyDTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public Company getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Company saveNewCompany(Company company) {
        company.setUserLastChanged(userSource.getAuthenticatedUser());

        company.setLastChange(ZonedDateTime.now());

        log.info("Компания для сохранения = {}", company);

        return companyRepository.save(company);
    }

    @Override
    public Company updateCompany(Long id, Company company) {
        company.setId(id);

        Company companyToUpdate = getCompany(id);

        BeanUtils.copyProperties(company, companyToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        companyRepository.saveAndFlush(companyToUpdate);

        return companyToUpdate;
    }

    @Override
    public void delete(Long id) {
        companyRepository.deleteById(id);
    }
}