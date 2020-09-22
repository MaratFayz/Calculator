package LD.dao;

public class EntryDaoImpl implements EntryDao {
/*
* //ActiveEntriesForFirstOpenPeriod
        public List<R> getActiveEntriesForScenarioAndFirstOpenPeriodAsFormRegLd1 (Scenario scenario){
            LocalDate firstOpenPeriod = periodClosedRepository.getFirstOpenPeriod(scenario);

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entry> cq = cb.createQuery(Entry.class);
            Root<Entry> root = cq.from(Entry.class);
            Join<EntryIFRSAccID, IFRSAccount> entryToJoin = entryIfrsAccRoot.join(EntryIFRSAcc_.entryIFRSAccID).join(EntryIFRSAccID_.ifrsAccount);

            cq.select(root);


            return null;
        }*/
}
