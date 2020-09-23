package LD.dao;

import LD.model.Entry.EntryID;
import LD.model.Entry.EntryID_;
import LD.model.Entry.Entry_;
import LD.model.EntryIFRSAcc.EntryIFRSAcc;
import LD.model.EntryIFRSAcc.EntryIFRSAccID;
import LD.model.EntryIFRSAcc.EntryIFRSAccID_;
import LD.model.EntryIFRSAcc.EntryIFRSAcc_;
import LD.model.Enums.EntryStatus;
import LD.model.IFRSAccount.IFRSAccount;
import LD.model.IFRSAccount.IFRSAccount_;
import LD.model.Period.Period;
import LD.model.Period.Period_;
import LD.model.Scenario.Scenario;
import LD.model.Scenario.Scenario_;
import LD.repository.PeriodsClosedRepository;
import LD.repository.ScenarioRepository;
import LD.rest.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

@Log4j2
public class EntryIfrsAccDaoImpl implements EntryIfrsAccDao {

    @Autowired
    private PeriodsClosedRepository periodClosedRepository;
    @Autowired
    private ScenarioRepository scenarioRepository;
    @Autowired
    private EntityManager entityManager;
    private Root<EntryIFRSAcc> root;
    private CriteriaBuilder cb;

    @Override
    public List<Object[]> sumActualEntriesIfrs(long scenarioToId) {
        final Scenario scenario_to = scenarioRepository.findById(scenarioToId)
                .orElseThrow(() -> new NotFoundException("Значение сценария " + scenarioToId + " отсутствует в базе данных"));

        log.info("Был получен сценарий-получатель = {}", scenario_to);

        final Period firstOpenPeriodForScenarioTo = periodClosedRepository.findFirstOpenPeriodByScenario(scenario_to);

        log.info("Был получен первый открытый период для сценария-получателя = {}", firstOpenPeriodForScenarioTo);

        cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> criteriaQuery = cb.createQuery(Object[].class);

        root = criteriaQuery.from(EntryIFRSAcc.class);
        Join<EntryIFRSAccID, IFRSAccount> entryIFRSAccToIfrsAccountJoin = root.join(EntryIFRSAcc_.entryIFRSAccID).join(EntryIFRSAccID_.ifrsAccount);
        Join<EntryID, Scenario> entryIfrsToScenario = root.join(EntryIFRSAcc_.entryIFRSAccID).join(EntryIFRSAccID_.entry).join(Entry_.entryID).join(EntryID_.scenario);
        Join<EntryID, Period> entryIfrsToPeriod = root.join(EntryIFRSAcc_.entryIFRSAccID).join(EntryIFRSAccID_.entry).join(Entry_.entryID).join(EntryID_.period);

        criteriaQuery.multiselect(
                entryIfrsToScenario.get(Scenario_.name).alias("scenario"),
                entryIfrsToPeriod.get(Period_.date).alias("period"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.account_code).alias("account_code"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.account_name).alias("account_name"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.flow_code).alias("flow_code"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.flow_name).alias("flow_name"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.sh).alias("sh"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.pa).alias("pa"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.ct).alias("ct"),
                entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.dr).alias("dr"),
                cb.sum(root.get(EntryIFRSAcc_.sum)).alias("sum")
        );

        criteriaQuery.where(
                cb.and(
                        cb.equal(root.get(EntryIFRSAcc_.entryIFRSAccID).get(EntryIFRSAccID_.entry).get(Entry_.entryID).get(EntryID_.scenario), scenario_to),
                        cb.equal(root.get(EntryIFRSAcc_.entryIFRSAccID).get(EntryIFRSAccID_.entry).get(Entry_.entryID).get(EntryID_.period), firstOpenPeriodForScenarioTo),
                        cb.equal(root.get(EntryIFRSAcc_.entryIFRSAccID).get(EntryIFRSAccID_.entry).get(Entry_.status), EntryStatus.ACTUAL)
                )
        );

        criteriaQuery.groupBy(entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.id));
        criteriaQuery.orderBy(cb.asc(entryIFRSAccToIfrsAccountJoin.get(IFRSAccount_.id)));

        TypedQuery<Object[]> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }
}