package LD.service;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.repository.EntryIFRSAccRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntryIFRSAccServiceImpl implements EntryIFRSAccService
{
	@Autowired
	EntryIFRSAccRepository entryIFRSAccRepository;

	@Override
	public List<EntryIFRSAcc> getAllEntriesIFRSAcc()
	{
		return entryIFRSAccRepository.findAll();
	}

	@Override
	public EntryIFRSAcc getEntryIFRSAcc(EntryIFRSAccID id)
	{
		return entryIFRSAccRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public EntryIFRSAcc saveNewEntryIFRSAcc(EntryIFRSAcc entryIFRSAcc)
	{
		return entryIFRSAccRepository.saveAndFlush(entryIFRSAcc);
	}

	@Override
	public EntryIFRSAcc updateEntryIFRSAcc(EntryIFRSAccID id, EntryIFRSAcc entryIFRSAcc)
	{
		EntryIFRSAcc endDateToUpdate = getEntryIFRSAcc(id);

		BeanUtils.copyProperties(entryIFRSAcc, endDateToUpdate);

		entryIFRSAccRepository.saveAndFlush(endDateToUpdate);

		return endDateToUpdate;
	}

	@Override
	public boolean delete(EntryIFRSAccID id)
	{
		try
		{
			entryIFRSAccRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
