package LD.repository;

import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryIFRSAccRepository extends JpaRepository<EntryIFRSAcc, EntryIFRSAccID>
{
}
