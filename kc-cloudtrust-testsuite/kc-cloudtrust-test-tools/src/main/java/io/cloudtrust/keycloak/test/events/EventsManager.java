package io.cloudtrust.keycloak.test.events;

import io.cloudtrust.keycloak.test.util.JsonToolbox;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class EventsManager<T> {
    private static final Logger LOG = Logger.getLogger(EventsManager.class);

    private final BiConsumer<String, Consumer<RealmEventsConfigRepresentation>> configurationHandler;
    private final Consumer<String> eventsCleaner;
    private final Function<String, List<T>> eventsProvider;
    private final Comparator<T> eventsComparator;
    private final Queue<T> events = new LinkedList<>();
    private final Set<String> realmWithActivatedEvents = new HashSet<>();
    private int readEvents = 0;

    public EventsManager(BiConsumer<String, Consumer<RealmEventsConfigRepresentation>> configurationHandler, Consumer<String> eventsCleaner, Function<String, List<T>> eventsProvider, Comparator<T> eventsComparator) {
        this.configurationHandler = configurationHandler;
        this.eventsCleaner = eventsCleaner;
        this.eventsProvider = eventsProvider;
        this.eventsComparator = eventsComparator;
    }

    public void onRealmRemoved(String name) {
        this.realmWithActivatedEvents.remove(name);
    }

    /**
     * Events management: activate events
     */
    public void activate(String realmName) {
        activate(realmName, null);
    }

    public void activate(String realmName, Consumer<RealmEventsConfigRepresentation> configConsumer) {
        this.realmWithActivatedEvents.add(realmName);
        this.configurationHandler.accept(realmName, configConsumer);
    }

    /**
     * Event management: clear events
     */
    public void clear() {
        this.realmWithActivatedEvents.forEach(realm -> {
            try {
                this.eventsCleaner.accept(realm);
            } catch (NotFoundException e) {
                // Ignore
            }
        });
        this.readEvents = 0;
    }

    /**
     * Event management: poll event
     */
    public T poll() {
        if (events.isEmpty()) {
            List<T> newEvents = new ArrayList<>();
            for (String realmName : this.realmWithActivatedEvents) {
                List<T> realmEvents = this.eventsProvider.apply(realmName);
                if (realmEvents != null) {
                    newEvents.addAll(realmEvents);
                }
            }
            if (!newEvents.isEmpty()) {
                newEvents.sort(this.eventsComparator);
                events.clear();
                events.addAll(newEvents);
                for (int i = 0; i < readEvents; i++) {
                    events.poll();
                }
            }
        }
        T res = events.poll();
        if (res != null) {
            readEvents++;
            LOG.debugf("Polled event %s", JsonToolbox.toString(res));
        }
        return res;
    }

    public Collection<T> poll(int number) {
        List<T> events = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            T e = poll();
            if (e == null) {
                break;
            }
            events.add(e);
        }
        return events;
    }
}
