package LD.service;

import LD.model.Counterpartner.Counterpartner;

import java.util.List;

public interface CounterpartnerService
{
	Counterpartner getCounterpartner(Long id);

	Counterpartner saveNewCounterpartner(Counterpartner counterpartner);

	Counterpartner updateCounterpartner(Long id, Counterpartner counterpartner);

	boolean delete(Long id);

	List<Counterpartner> getAllCounterpartners();
}
