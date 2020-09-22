package LD.repository;

import LD.dao.EntryDao;
import LD.model.Entry.Entry;
import LD.model.Entry.EntryID;
import LD.model.Enums.EntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, EntryID>, JpaSpecificationExecutor<Entry>, EntryDao {

    List<Entry> findBystatus(EntryStatus entryStatus);
}
