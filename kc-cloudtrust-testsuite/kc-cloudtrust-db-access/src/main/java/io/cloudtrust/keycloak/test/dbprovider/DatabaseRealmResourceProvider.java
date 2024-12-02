package io.cloudtrust.keycloak.test.dbprovider;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseRealmResourceProvider implements RealmResourceProvider {
    private static final Logger LOG = Logger.getLogger(DatabaseRealmResourceProvider.class);

    private KeycloakSession session;

    public DatabaseRealmResourceProvider(KeycloakSession session) {
        this.session = session;
        LOG.debugf("Instanciated DatabaseRealmResourceProvider for request %s %s",
                session.getContext().getHttpRequest().getHttpMethod(),
                session.getContext().getHttpRequest().getUri().getPath());
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello() {
        return Response.ok("Hello World!").type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("users/{user}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getUserAttributes(final @PathParam("user") String userId) {
        LOG.debugf("getUserAttributes(%s)", userId);
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        Map<String, List<String>> res = new HashMap<>();
        List<Object[]> result = em.createNativeQuery("SELECT * FROM USER_ATTRIBUTE WHERE USER_ID IN (SELECT ID FROM USER_ENTITY WHERE REALM_ID=:realm) AND USER_ID=:userId")
                .setParameter("realm", this.session.getContext().getRealm().getName())
                .setParameter("userId", userId)
                .getResultList();
        result.forEach(row -> {
            String name = (String) row[0];
            String value = (String) row[1];
            res.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        });
        return res;
    }

    @POST
    @Path("users/{user}/attributes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setUserAttributes(final @PathParam("user") String userId, Map<String, List<String>> attributes) {
        LOG.debugf("setUserAttributes(%s)", userId);
        UserModel user = this.session.users().getUserById(this.session.getContext().getRealm(), userId);
        attributes.forEach(user::setAttribute);
        return Response.noContent().build();
    }
}
