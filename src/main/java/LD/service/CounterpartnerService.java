package LD.service;

import LD.model.Counterpartner.Counterpartner;
import LD.model.Counterpartner.Counterpartner_out;

import java.util.List;

public interface CounterpartnerService
{
	Counterpartner getCounterpartner(Long id);

	Counterpartner saveNewCounterpartner(Counterpartner counterpartner);

	Counterpartner updateCounterpartner(Long id, Counterpartner counterpartner);

	void delete(Long id);

	List<Counterpartner_out> getAllCounterpartners();
}
