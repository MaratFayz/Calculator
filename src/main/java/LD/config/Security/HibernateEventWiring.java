package LD.config.Security;

import LD.config.SaveOrUpdateEventsHandler;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Component
@Log4j2
public class HibernateEventWiring {

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private SaveOrUpdateEventsHandler saveOrUpdateEventsHandler;

    @PostConstruct
    public void register() {
        log.info("register");

        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.SAVE_UPDATE).appendListener(saveOrUpdateEventsHandler);
        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(saveOrUpdateEventsHandler);
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(saveOrUpdateEventsHandler);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(saveOrUpdateEventsHandler);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(saveOrUpdateEventsHandler);
    }
}