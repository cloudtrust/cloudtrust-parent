package io.cloudtrust.keycloak.uitabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.function.TriConsumer;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentModelProcessor<T> {
    private static final Logger LOGGER = Logger.getLogger(ComponentModelProcessor.class);

    private final String configName;
    private final Class<T> clazz;

    public ComponentModelProcessor(String configName, Class<T> clazz) {
        this.configName = configName;
        this.clazz = clazz;
    }

    public void compareModels(RealmModel realmModel, IdentityProviderStorageProvider identityProviders, ComponentModel oldModel, ComponentModel newModel, TriConsumer<IdentityProviderModel, T, T> consumer) throws JsonProcessingException {
        Map<String, T> oldSettings = this.readFromModel(oldModel);
        Map<String, T> newSettings = this.readFromModel(newModel);
        oldSettings.entrySet().stream().filter(e -> !newSettings.containsKey(e.getKey())).forEach(e -> {
            IdentityProviderModel idp = identityProviders.getByAlias(e.getKey());
            if (idp == null) {
                // Not a blocker cause we are removing the configuration
                LOGGER.warnf("Identity provider with alias %s not found", e.getKey());
                return;
            }
            consumer.accept(idp, e.getValue(), null);
        });
        newSettings.forEach((k, v) -> {
            IdentityProviderModel idp = identityProviders.getByAlias(k);
            if (idp == null) {
                throw new ComponentValidationException("Identity provider with alias %s not found", k);
            }
            consumer.accept(idp, oldSettings.get(k), v);
        });
    }

    public void compareModels(RealmModel realmModel, ClientProvider clients, ComponentModel oldModel, ComponentModel newModel, TriConsumer<ClientModel, T, T> consumer) throws JsonProcessingException {
        Map<String, T> oldSettings = this.readFromModel(oldModel);
        Map<String, T> newSettings = this.readFromModel(newModel);
        oldSettings.entrySet().stream().filter(e -> !newSettings.containsKey(e.getKey())).forEach(e -> {
            ClientModel client = clients.getClientByClientId(realmModel, e.getKey());
            if (client == null) {
                // Not a blocker cause we are removing the configuration
                LOGGER.warnf("Client with ID %s not found", e.getKey());
                return;
            }
            consumer.accept(client, e.getValue(), null);
        });
        newSettings.forEach((k, v) -> {
            ClientModel client = clients.getClientByClientId(realmModel, k);
            if (client == null) {
                throw new ComponentValidationException("Client with ID %s not found", k);
            }
            consumer.accept(client, oldSettings.get(k), v);
        });
    }

    public Map<String, T> readFromModel(ComponentModel model) throws JsonProcessingException {
        if (model == null) {
            return new HashMap<>();
        }
        return fromModel(model.get(configName, "[]"));
    }

    public void writeToModel(ComponentModel model, Map<String, T> settings) throws JsonProcessingException {
        model.put(configName, toModel(settings));
    }

    public Map<String, T> fromModel(String raw) throws JsonProcessingException {
        Map<String, T> res = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        List<Map<String, String>> items = om.readValue(raw, new TypeReference<>() {
        });
        for (Map<String, String> item : items) {
            String key = item.get("key");
            T value = om.readValue(item.get("value"), clazz);
            res.put(key, value);
        }
        return res;
    }

    public String toModel(Map<String, T> settings) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        List<Map<String, String>> model = settings.entrySet().stream().map(entry -> {
            Map<String, String> res = new HashMap<>();
            res.put("key", entry.getKey());
            try {
                res.put("value", om.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException jpe) {
                throw new ComponentValidationException("Can't serialize value for key " + entry.getKey());
            }
            return res;
        }).toList();
        return om.writeValueAsString(model);
    }
}
