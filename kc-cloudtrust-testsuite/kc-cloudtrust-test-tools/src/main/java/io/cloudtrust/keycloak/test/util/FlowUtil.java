package io.cloudtrust.keycloak.test.util;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserConfiguredAuthenticatorFactory;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FlowUtil {
    private final RealmResource realm;
    private final Random rand = new Random(System.currentTimeMillis());
    private final Pattern identifierAtEndPattern = Pattern.compile("/([^/]+)$");
    private AuthenticationFlowRepresentation currentFlow;
    private int maxPriority = 0;
    private int flowIndex = 0;
    private List<AuthenticationExecutionRepresentation> executions = null;

    public enum Requirement {
        REQUIRED,
        CONDITIONAL,
        ALTERNATIVE,
        DISABLED
    }

    private static final String BROWSER_FLOW = "browser";
    private static final String DIRECT_GRANT_FLOW = "direct grant";
    private static final String RESET_CREDENTIALS_FLOW = "reset credentials";
    private static final String FIRST_BROKER_LOGIN_FLOW = "first broker login";
    private static final String BASIC_FLOW = "basic-flow";
    private static final String FORM_FLOW = "form-flow";

    public static class FlowUtilException extends RuntimeException {
        private static final long serialVersionUID = 5118401044519260295L;

        public FlowUtilException(String message) {
            super(message);
        }
    }

    public FlowUtil(RealmResource realm) {
        this.realm = realm;
    }

    public AuthenticationFlowRepresentation build() {
        return currentFlow;
    }

    private AuthenticationFlowRepresentation getFlowByAlias(String alias) {
        return this.realm.flows().getFlows().stream().filter(f -> f.getAlias().equals(alias)).findFirst().orElse(null);
    }

    public static FlowUtil inCurrentRealm(RealmResource realm) {
        return new FlowUtil(realm);
    }

    private FlowUtil newFlowUtil(AuthenticationFlowRepresentation flowModel) {
        FlowUtil subflow = new FlowUtil(realm);
        subflow.currentFlow = flowModel;
        return subflow;
    }

    public FlowUtil selectFlow(String flowAlias) {
        currentFlow = getFlowByAlias(flowAlias);
        if (currentFlow == null) {
            throw new FlowUtilException("Can't select flow: " + flowAlias + " does not exist");
        }

        return this;
    }

    public String describe() {
        return JsonToolbox.toString(this.currentFlow);
    }

    public FlowUtil copyBrowserFlow(String newFlowAlias) {
        return copyFlow(BROWSER_FLOW, newFlowAlias);
    }

    public FlowUtil copyDirectGrantFlow(String newFlowAlias) {
        return copyFlow(DIRECT_GRANT_FLOW, newFlowAlias);
    }

    public FlowUtil copyResetCredentialsFlow(String newFlowAlias) {
        return copyFlow(RESET_CREDENTIALS_FLOW, newFlowAlias);
    }

    public FlowUtil copyFirstBrokerLoginFlow(String newFlowAlias) {
        return copyFlow(FIRST_BROKER_LOGIN_FLOW, newFlowAlias);
    }

    public FlowUtil copyFlow(String original, String newFlowAlias) {
        AuthenticationFlowRepresentation originalFlow = getFlowByAlias(original);
        if (originalFlow == null) {
            throw new FlowUtilException("Can't copy flow: " + original + " does not exist");
        }
        Map<String, String> data = Collections.singletonMap("newName", newFlowAlias);
        String newFlowId = getLocation(() -> this.realm.flows().copy(newFlowAlias, data));
        currentFlow = this.realm.flows().getFlow(newFlowId);

        return this;
    }

    public FlowUtil createFlow(String alias, Consumer<FlowUtil> flowConsumer) {
        return createFlow(alias, BASIC_FLOW, flowConsumer);
    }

    public FlowUtil createStandardBrowserFlow(String alias, Consumer<FlowUtil> flowConsumer) {
        return createStandardBrowserFlow(alias, true, flowConsumer);
    }

    public FlowUtil createStandardBrowserFlow(String alias, boolean addUserConfiguredAuth, Consumer<FlowUtil> flowConsumer) {
        return createFlow(alias, f -> f
                .addAuthenticatorExecution(Requirement.ALTERNATIVE, CookieAuthenticatorFactory.PROVIDER_ID)
                .addAuthenticatorExecution(Requirement.ALTERNATIVE, IdentityProviderAuthenticatorFactory.PROVIDER_ID)
                .addSubFlowExecution(Requirement.ALTERNATIVE, subflow -> subflow
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(addUserConfiguredAuth ? Requirement.CONDITIONAL : Requirement.REQUIRED, condflow -> {
                            if (addUserConfiguredAuth) {
                                condflow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalUserConfiguredAuthenticatorFactory.PROVIDER_ID);
                            }
                            flowConsumer.accept(condflow);
                        })
                )
        );
    }

    public FlowUtil createStandardRegistrationFlow(String alias, Consumer<FlowUtil> flowConsumer) {
        return createStandardRegistrationFlow(alias, RegistrationPage.PROVIDER_ID, flowConsumer);
    }

    private FlowUtil createStandardRegistrationFlow(String alias, String formAuthenticatorId, Consumer<FlowUtil> flowConsumer) {
        return createFlow(alias, BASIC_FLOW, f -> f.addSubFlowExecution(Requirement.REQUIRED, formAuthenticatorId, flowConsumer));
    }

    public FlowUtil createFlow(String alias, String providerId, Consumer<FlowUtil> subFlowInitializer) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setProviderId(providerId);
        flow.setBuiltIn(false);
        flow.setTopLevel(true);
        flow.setId(getLocation(() -> this.realm.flows().createFlow(flow)));
        currentFlow = getFlowByAlias(alias);
        FlowUtil subFlow = newFlowUtil(flow);
        subFlowInitializer.accept(subFlow);

        return this;
    }

    private String getLocation(Supplier<Response> apiCall) {
        return getHeader(apiCall, "Location", identifierAtEndPattern);
    }

    private String getHeader(Supplier<Response> apiCall, String headerName, Pattern pattern) {
        try (Response resp = apiCall.get()) {
            MultivaluedMap<String, Object> headers = resp.getHeaders();
            if (headers == null || !headers.containsKey(headerName) || headers.get(headerName).isEmpty()) {
                return null;
            }
            String value = (String) headers.get(headerName).get(0);
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public FlowUtil inForms(Consumer<FlowUtil> subFlowInitializer) {
        return inFlow(this.currentFlow.getAlias() + " forms", subFlowInitializer);
    }

    public FlowUtil inVerifyExistingAccountByReAuthentication(Consumer<FlowUtil> subFlowInitializer) {
        return inFlow(this.currentFlow.getAlias() + " Verify Existing Account by Re-authentication", subFlowInitializer);
    }

    public FlowUtil inFlow(String alias, Consumer<FlowUtil> subFlowInitializer) {
        if (subFlowInitializer != null) {
            AuthenticationFlowRepresentation flow = getFlowByAlias(alias);
            if (flow == null) {
                throw new FlowUtilException("Can't find flow by alias: " + alias);
            }
            FlowUtil subFlow = newFlowUtil(flow);
            subFlowInitializer.accept(subFlow);
        }

        return this;
    }

    public FlowUtil inFirstSubflow(Consumer<FlowUtil> subFlowInitializer) {
        return inFlow(authFlowRep -> true, subFlowInitializer);
    }

    public FlowUtil inFlow(Predicate<AuthenticationFlowRepresentation> flowSelector, Consumer<FlowUtil> subFlowInitializer) {
        AuthenticationFlowRepresentation flow = this.currentFlow.getAuthenticationExecutions().stream()
                .filter(e -> e.getFlowAlias() != null)
                .map(e -> this.realm.flows().getFlows().stream()
                        .filter(f -> f.getAlias().equals(e.getFlowAlias()))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(flowSelector)
                .findFirst()
                .orElse(null);
        if (flow != null) {
            subFlowInitializer.accept(newFlowUtil(flow));
        }
        return this;
    }

    public FlowUtil clear() {
        // Get executions from current flow
        currentFlow.getAuthenticationExecutions().clear();
        realm.flows().updateFlow(currentFlow.getId(), currentFlow);

        return this;
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId) {
        return addAuthenticatorExecution(requirement, providerId, null);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, int priority) {
        return addAuthenticatorExecution(requirement, providerId, priority, null);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, Consumer<AuthenticatorConfigRepresentation> configInitializer) {
        return addAuthenticatorExecution(requirement, providerId, maxPriority + 10, configInitializer);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, int priority, Consumer<AuthenticatorConfigRepresentation> configInitializer) {
        AuthenticatorConfigRepresentation authConfig = null;

        maxPriority = Math.max(maxPriority, priority);

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setRequirement(requirement.toString());
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(providerId);
        execution.setPriority(priority);
        execution.setParentFlow(currentFlow.getId());
        if (configInitializer != null) {
            authConfig = new AuthenticatorConfigRepresentation();
            // Caller is free to update this alias
            authConfig.setAlias("cfg-" + rand.nextInt(1000));
            authConfig.setConfig(new HashMap<>());
            configInitializer.accept(authConfig);
        }
        execution.setId(getLocation(() -> realm.flows().addExecution(execution)));
        if (authConfig != null) {
            final AuthenticatorConfigRepresentation finalAuthCfg = authConfig;
            authConfig.setId(getLocation(() -> realm.flows().newExecutionConfig(execution.getId(), finalAuthCfg)));
        }

        return this;
    }

    public FlowUtil defineAsBrowserFlow() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setBrowserFlow(currentFlow.getAlias());
        realm.update(rep);
        return this;
    }

    public FlowUtil defineAsDirectGrantFlow() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setDirectGrantFlow(currentFlow.getAlias());
        realm.update(rep);
        return this;
    }

    public FlowUtil defineAsResetCredentialsFlow() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setResetCredentialsFlow(currentFlow.getAlias());
        realm.update(rep);
        return this;
    }

    public FlowUtil defineAsRegisterFlow() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setRegistrationFlow(currentFlow.getAlias());
        realm.update(rep);
        return this;
    }

    public FlowUtil usesInIdentityProvider(String idpAlias) {
        // Setup new FirstBrokerLogin flow to identity provider
        IdentityProviderResource idp = realm.identityProviders().get(idpAlias);
        IdentityProviderRepresentation rep = idp.toRepresentation();
        rep.setFirstBrokerLoginFlowAlias(currentFlow.getAlias());
        idp.update(rep);
        return this;
    }

    private String subFlowName() {
        return (this.currentFlow == null ? "sf" : this.currentFlow.getAlias() + "-") + ++this.flowIndex;
    }

    public FlowUtil addSubFlowExecution(Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(subFlowName(), BASIC_FLOW, requirement, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(Requirement requirement, String formAuthenticatorId, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(subFlowName(), FORM_FLOW, requirement, formAuthenticatorId, maxPriority + 10, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(String alias, String providerId, Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(alias, providerId, requirement, null, maxPriority + 10, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(String alias, String providerId, Requirement requirement, String formAuthenticatorId, int priority, Consumer<FlowUtil> flowInitializer) {
        AuthenticationFlowRepresentation flowRep = createFlowRepresentation(alias, providerId, null, false, false);
        return addSubFlowExecution(flowRep, requirement, formAuthenticatorId, priority, flowInitializer);
    }

    public static AuthenticationFlowRepresentation createFlowRepresentation(String alias, String providerId, String desc, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flowRep = new AuthenticationFlowRepresentation();
        flowRep.setAlias(alias);
        flowRep.setDescription(desc);
        flowRep.setProviderId(providerId);
        flowRep.setTopLevel(topLevel);
        flowRep.setBuiltIn(builtIn);
        return flowRep;
    }

    public FlowUtil addSubFlowExecution(AuthenticationFlowRepresentation flowRep, Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(flowRep, requirement, null, maxPriority + 10, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(AuthenticationFlowRepresentation flowRep, Requirement requirement, String formAuthenticatorId, int priority, Consumer<FlowUtil> flowInitializer) {
        maxPriority = Math.max(maxPriority, priority);

        flowRep.setId(getLocation(() -> realm.flows().createFlow(flowRep)));
        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setRequirement(requirement.toString());
        execution.setAuthenticatorFlow(true);
        execution.setAuthenticator(formAuthenticatorId);
        execution.setPriority(priority);
        execution.setFlowId(flowRep.getId());
        execution.setParentFlow(currentFlow.getId());
        execution.setId(getLocation(() -> realm.flows().addExecution(execution)));

        if (flowInitializer != null) {
            FlowUtil subflow = newFlowUtil(flowRep);
            flowInitializer.accept(subflow);
        }

        return this;
    }

    private List<AuthenticationExecutionRepresentation> getExecutions() {
        if (executions == null) {
            AuthenticationManagementResource flows = realm.flows();
            executions = flows.getExecutions(currentFlow.getAlias()).stream().map(e -> flows.getExecution(e.getId())).collect(Collectors.toList());
            if (executions == null) {
                throw new FlowUtilException("Can't get executions of unknown flow " + currentFlow.getAlias());
            }
        }
        return executions;
    }

    public FlowUtil removeExecution(int index) {
        List<AuthenticationExecutionRepresentation> authExecutions = getExecutions();
        realm.flows().removeExecution(authExecutions.get(index).getId());

        return this;
    }

    public FlowUtil updateExecution(int index, Consumer<AuthenticationExecutionInfoRepresentation> updater) {
        List<AuthenticationExecutionInfoRepresentation> flowExecutions = realm.flows().getExecutions(currentFlow.getAlias());
        if (updater != null) {
            AuthenticationExecutionInfoRepresentation exec = flowExecutions.get(index);
            updater.accept(exec);
            realm.flows().updateExecutions(currentFlow.getAlias(), exec);
        }

        return this;
    }
}