package LD.repository;

import LD.model.Counterpartner.Counterpartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CounterpartnerRepository extends JpaRepository<Counterpartner, Long> {

}
