package LD.service;

import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccountDTO_out;

import java.util.List;

public interface IFRSAccountService
{
	List<IFRSAccountDTO_out> getAllIFRSAccounts();

	IFRSAccount getIFRSAccount(Long id);

	IFRSAccount saveNewIFRSAccount(IFRSAccount ifrsAccount);

	IFRSAccount updateIFRSAccount(Long id, IFRSAccount ifrsAccount);

	boolean delete(Long id);
}
