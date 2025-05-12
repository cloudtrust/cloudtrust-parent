package io.cloudtrust.keycloak.uitabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Objects;

public class ComponentModelProcessorTest {
    private final ComponentModelProcessor<MyModel> processor = new ComponentModelProcessor<>("MYMODEL", MyModel.class);

    @Test
    void emptyModel() throws JsonProcessingException {
        ComponentModel model = createModel();
        Map<String, MyModel> res = processor.readFromModel(model);
        Assertions.assertEquals(0, res.size());
    }

    @Test
    void writeThenReadModel() throws JsonProcessingException {
        ComponentModel model = createModel();
        Map<String, MyModel> map = Map.of(
                "client1", MyModel.create(5, "Tim"),
                "client2", MyModel.create(20, "Steeve")
        );
        processor.writeToModel(model, map);

        Map<String, MyModel> map2 = processor.readFromModel(model);
        Assertions.assertEquals(map.size(), map2.size());
        for (Map.Entry<String, MyModel> entry : map.entrySet()) {
            Assertions.assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
    }

    @Test
    void compareClientModels() throws JsonProcessingException {
        RealmModel realmModel = Mockito.mock(RealmModel.class);
        ClientProvider clients = Mockito.mock(ClientProvider.class);
        ClientModel client1 = Mockito.mock(ClientModel.class);
        ClientModel client2 = Mockito.mock(ClientModel.class);
        ClientModel client3 = Mockito.mock(ClientModel.class);

        Mockito.when(clients.getClientByClientId(realmModel, "client1")).thenReturn(client1);
        Mockito.when(clients.getClientByClientId(realmModel, "client2")).thenReturn(client2);
        Mockito.when(clients.getClientByClientId(realmModel, "client3")).thenReturn(client3);
        Mockito.when(clients.getClientByClientId(realmModel, "client4")).thenReturn(null);

        ComponentModel oldModel = createModel(Map.of(
                "client1", MyModel.create(5, "Tim"),
                "client2", MyModel.create(20, "Steeve"),
                "client4", MyModel.create(999, "Nefertiti")
        ));
        ComponentModel newModel = createModel(Map.of(
                "client2", MyModel.create(25, "Steeve"),
                "client3", MyModel.create(50, "Frederik")
        ));

        processor.compareModels(realmModel, clients, oldModel, newModel, (client, my1, my2) -> {
            if (my1 != null && my2 == null) {
                // Removal
                Assertions.assertSame(client1, client);
            } else if (my1 == null && my2 != null) {
                // Insertion
                Assertions.assertSame(client3, client);
            } else if (my1 != null && my2 != null) {
                // Update
                Assertions.assertSame(client2, client);
            } else {
                // Should not occur
                Assertions.fail();
            }
        });
    }

    private ComponentModel createModel() {
        ComponentModel emptyModel = new ComponentModel();
        emptyModel.setConfig(new MultivaluedHashMap<>());
        return emptyModel;
    }

    private ComponentModel createModel(Map<String, MyModel> map) throws JsonProcessingException {
        ComponentModel model = new ComponentModel();
        model.setConfig(new MultivaluedHashMap<>());
        if (map != null) {
            processor.writeToModel(model, map);
        }
        return model;
    }

    public static class MyModel {
        private int age;
        private String name;

        public static MyModel create(int age, String name) {
            MyModel res = new MyModel();
            res.setAge(age);
            res.setName(name);
            return res;
        }

        public int getAge() {
            return this.age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MyModel myModel)) return false;
            return age == myModel.age && Objects.equals(name, myModel.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, name);
        }
    }
}
