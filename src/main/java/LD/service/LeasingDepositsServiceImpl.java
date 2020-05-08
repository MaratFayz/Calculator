package LD.service;

import LD.model.LeasingDeposit.LeasingDeposit;
import LD.model.LeasingDeposit.LeasingDepositDTO;
import LD.model.LeasingDeposit.LeasingDepositTransform;
import LD.repository.LeasingDepositRepository;
import LD.rest.exceptions.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeasingDepositsServiceImpl implements LeasingDepositService
{
	@Autowired
	LeasingDepositRepository leasingDepositRepository;
	@Autowired
	LeasingDepositTransform leasingDepositTransform;

	@Override
	public List<LeasingDepositDTO> getAllLeasingDeposits()
	{
		return leasingDepositRepository.findAll().stream()
				.map(ld -> leasingDepositTransform.LeasingDeposit_to_LeasingDepositDTO(ld))
				.collect(Collectors.toList());
	}

	@Override
	public LeasingDeposit getLeasingDeposit(Long id)
	{
		return leasingDepositRepository.findById(id).orElseThrow(NotFoundException::new);
	}

	@Override
	public LeasingDeposit saveNewLeasingDeposit(LeasingDeposit leasingDeposit)
	{
		return leasingDepositRepository.save(leasingDeposit);
	}

	@Override
	public LeasingDeposit updateLeasingDeposit(Long id, LeasingDeposit leasingDeposit)
	{
		LeasingDeposit leasingDepositToUpdate = getLeasingDeposit(id);

		BeanUtils.copyProperties(leasingDeposit, leasingDepositToUpdate);

		leasingDepositRepository.saveAndFlush(leasingDepositToUpdate);

		return leasingDepositToUpdate;
	}

	@Override
	public boolean delete(Long id)
	{
		try
		{
			leasingDepositRepository.deleteById(id);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}
}
