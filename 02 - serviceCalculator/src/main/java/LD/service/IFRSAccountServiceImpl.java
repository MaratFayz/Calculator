package LD.service;

import LD.config.UserSource;
import LD.model.AbstractModelClass_;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccountDTO_out;
import LD.repository.IFRSAccountRepository;
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
public class IFRSAccountServiceImpl implements IFRSAccountService {

    @Autowired
    IFRSAccountRepository ifrsAccountRepository;
    @Autowired
    UserSource userSource;

    @Override
    public List<IFRSAccountDTO_out> getAllIFRSAccounts() {
        List<IFRSAccount> resultFormDB = ifrsAccountRepository.findAll();
        List<IFRSAccountDTO_out> resultFormDB_out = new ArrayList<>();

        if (resultFormDB.size() == 0) {
            resultFormDB_out.add(new IFRSAccountDTO_out());
        } else {
            resultFormDB_out = resultFormDB.stream()
                    .map(c -> IFRSAccountDTO_out.IFRSAccount_to_IFRSAccount_DTO_out(c))
                    .collect(Collectors.toList());
        }

        return resultFormDB_out;
    }

    @Override
    public IFRSAccount getIFRSAccount(Long id) {
        return ifrsAccountRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public IFRSAccount saveNewIFRSAccount(IFRSAccount ifrsAccount) {
        ifrsAccount.setUserLastChanged(userSource.getAuthenticatedUser());
        ifrsAccount.setLastChange(ZonedDateTime.now());

        log.info("Счёт МСФО для сохранения = {}", ifrsAccount);

        return ifrsAccountRepository.save(ifrsAccount);
    }

    @Override
    public IFRSAccount updateIFRSAccount(Long id, IFRSAccount ifrsAccount) {
        ifrsAccount.setId(id);

        IFRSAccount ifrsAccountToUpdate = getIFRSAccount(id);

        BeanUtils.copyProperties(ifrsAccount, ifrsAccountToUpdate, AbstractModelClass_.LAST_CHANGE, AbstractModelClass_.USER_LAST_CHANGED);

        ifrsAccountRepository.saveAndFlush(ifrsAccountToUpdate);

        return ifrsAccountToUpdate;
    }

    @Override
    public void delete(Long id) {
        ifrsAccountRepository.deleteById(id);
    }
}