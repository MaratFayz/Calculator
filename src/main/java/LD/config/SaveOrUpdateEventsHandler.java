package LD.config;

import LD.config.Security.model.User.User;
import LD.model.AbstractModelClass;
import LD.model.AbstractModelClass_;
import lombok.extern.log4j.Log4j2;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;

@Log4j2
@Component
public class SaveOrUpdateEventsHandler implements SaveOrUpdateEventListener,
        PreInsertEventListener, PreUpdateEventListener, PostUpdateEventListener, PostInsertEventListener {

    @Autowired
    UserSource userSource;

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        log.info("SaveOrUpdateEvent произошёл");

        Object entity = event.getEntity();
        addDataTimeAndUser(entity);
    }

    private void addDataTimeAndUser(Object object) {
        log.info("addDataTimeAndUser start");
        if (object instanceof AbstractModelClass) {
            User authenticatedUser = userSource.getAuthenticatedUser();
            log.info("authenticatedUser => {}", authenticatedUser.getUsername());

            Field userField = ReflectionUtils.findField(object.getClass(), AbstractModelClass_.USER_LAST_CHANGED);
            ReflectionUtils.makeAccessible(userField);
            ReflectionUtils.setField(userField, object, authenticatedUser);

            Field lastChangeField = ReflectionUtils.findField(object.getClass(), AbstractModelClass_.LAST_CHANGE);
            ReflectionUtils.makeAccessible(lastChangeField);
            ReflectionUtils.setField(lastChangeField, object, ZonedDateTime.now());

            log.info("object: {}", object);

        }
        log.info("addDataTimeAndUser end");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        log.info("PostInsertEvent произошёл");

    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        log.info("PostUpdateEvent произошёл");

    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        log.info("PreInsertEvent произошёл");

        Object entity = event.getEntity();
        addDataTimeAndUser(entity);

        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        log.info("PreUpdateEvent произошёл");

        Object entity = event.getEntity();
        addDataTimeAndUser(entity);

        return false;
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        log.info("requiresPostCommitHanding произошёл");

        return false;
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        log.info("requiresPostCommitHandling произошёл");

        return false;
    }
}