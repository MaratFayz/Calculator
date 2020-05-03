package LD.service;

import LD.model.IFRSAccount.IFRSAccount;
import LD.repository.IFRSAccountRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IFRSAccountServiceImpl implements IFRSAccountService
{
	@Autowired
	IFRSAccountRepository ifrsAccountRepository;

	@Override
	public List<IFRSAccount> getAllIFRSAccounts()
	{
		return ifrsAccountRepository.findAll();
	}

	@Override
	public IFRSAccount getIFRSAccount(Long id)
	{
		return ifrsAccountRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public IFRSAccount saveNewIFRSAccount(IFRSAccount ifrsAccount)
	{
		return ifrsAccountRepository.save(ifrsAccount);
	}

	@Override
	public IFRSAccount updateIFRSAccount(Long id, IFRSAccount ifrsAccount)
	{
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
