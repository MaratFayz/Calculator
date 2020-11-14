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
        log.trace("SaveOrUpdateEvent произошёл");

        Object entity = event.getEntity();
        addDataTimeAndUser(entity);
    }

    private void addDataTimeAndUser(Object object) {
        log.trace("addDataTimeAndUser start");
        if (object instanceof AbstractModelClass) {
            User authenticatedUser = userSource.getAuthenticatedUser();
            log.trace("authenticatedUser => {}", authenticatedUser.getUsername());

            Field userField = ReflectionUtils.findField(object.getClass(), AbstractModelClass_.USER_LAST_CHANGED);
            ReflectionUtils.makeAccessible(userField);
            ReflectionUtils.setField(userField, object, authenticatedUser);

            Field lastChangeField = ReflectionUtils.findField(object.getClass(), AbstractModelClass_.LAST_CHANGE);
            ReflectionUtils.makeAccessible(lastChangeField);
            ReflectionUtils.setField(lastChangeField, object, ZonedDateTime.now());

            log.trace("object: {}", object);
        }
        log.trace("addDataTimeAndUser end");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        log.trace("PostInsertEvent произошёл");
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        log.trace("PostUpdateEvent произошёл");
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        log.trace("PreInsertEvent произошёл");

        Object entity = event.getEntity();
        addDataTimeAndUser(entity);

        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        log.trace("PreUpdateEvent произошёл");

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