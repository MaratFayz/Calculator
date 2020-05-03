package LD.repository;

import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryRepository extends JpaRepository<Entry, EntryID>, JpaSpecificationExecutor<Entry>
{
}
