package LD.service;

import LD.model.IFRSAccount.IFRSAccount;

import java.util.List;

public interface IFRSAccountService
{
	List<IFRSAccount> getAllIFRSAccounts();

	IFRSAccount getIFRSAccount(Long id);

	IFRSAccount saveNewIFRSAccount(IFRSAccount ifrsAccount);

	IFRSAccount updateIFRSAccount(Long id, IFRSAccount ifrsAccount);

	boolean delete(Long id);
}
