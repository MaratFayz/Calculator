package LD.service;

import LD.config.Security.Repository.UserRepository;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccountDTO_out;
import LD.repository.IFRSAccountRepository;
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
public class IFRSAccountServiceImpl implements IFRSAccountService
{
	@Autowired
	IFRSAccountRepository ifrsAccountRepository;
	@Autowired
	UserRepository userRepository;

	@Override
	public List<IFRSAccountDTO_out> getAllIFRSAccounts()
	{
		List<IFRSAccount> resultFormDB = ifrsAccountRepository.findAll();
		List<IFRSAccountDTO_out> resultFormDB_out = new ArrayList<>();

		if(resultFormDB.size() == 0)
		{
			resultFormDB_out.add(new IFRSAccountDTO_out());
		}
		else
		{
			resultFormDB_out = resultFormDB.stream()
					.map(c -> IFRSAccountDTO_out.IFRSAccount_to_IFRSAccount_DTO_out(c))
					.collect(Collectors.toList());
		}

		return resultFormDB_out;
	}

	@Override
	public IFRSAccount getIFRSAccount(Long id)
	{
		return ifrsAccountRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public IFRSAccount saveNewIFRSAccount(IFRSAccount ifrsAccount)
	{
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		ifrsAccount.setUserLastChanged(userRepository.findByUsername(username));

		ifrsAccount.setLastChange(ZonedDateTime.now());

		log.info("Счёт МСФО для сохранения = {}", ifrsAccount);

		return ifrsAccountRepository.save(ifrsAccount);
	}

	@Override
	public IFRSAccount updateIFRSAccount(Long id, IFRSAccount ifrsAccount)
	{
		ifrsAccount.setId(id);

		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		ifrsAccount.setUserLastChanged(userRepository.findByUsername(username));

		ifrsAccount.setLastChange(ZonedDateTime.now());

		IFRSAccount ifrsAccountToUpdate = getIFRSAccount(id);

		BeanUtils.copyProperties(ifrsAccount, ifrsAccountToUpdate);

		ifrsAccountRepository.saveAndFlush(ifrsAccountToUpdate);

		return ifrsAccountToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			ifrsAccountRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
