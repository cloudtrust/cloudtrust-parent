package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialModel;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

class AuthenticatorUtilsTest {
    @ParameterizedTest
    @MethodSource("getRequestSamples")
    void getRequestTest(AuthenticationFlowContext ctx, String httpMethod, String paramName, String expected) {
        HttpRequest httpRequest = ctx.getHttpRequest();
        if (httpRequest != null) {
            Mockito.when(httpRequest.getHttpMethod()).thenReturn(httpMethod);
        }
        Assertions.assertEquals(expected, AuthenticatorUtils.getFirstDecodedFormParameter(ctx, paramName));
    }

    private static Stream<Arguments> getRequestSamples() {
        MultivaluedMap<String, String> myDecodedParameters = new MultivaluedHashMap<String, String>();
        myDecodedParameters.put("myParam", Arrays.asList("first", "second", "third"));
        myDecodedParameters.put("myEmptyParam", Collections.emptyList());

        AuthenticationFlowContext noRequestContext = Mockito.mock(AuthenticationFlowContext.class);

        AuthenticationFlowContext context = Mockito.mock(AuthenticationFlowContext.class);
        HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
        Mockito.when(context.getHttpRequest()).thenReturn(httpRequest);
        Mockito.when(httpRequest.getDecodedFormParameters()).thenReturn(myDecodedParameters);

        return Stream.of(
                Arguments.of(noRequestContext, "POST", "myParam", null),
                Arguments.of(context, "GET", "myParam", null),
                Arguments.of(context, "POST", "unknownParam", null),
                Arguments.of(context, "POST", "myEmptyParam", null),
                Arguments.of(context, "POST", "myParam", "first")
        );
    }

    @ParameterizedTest
    @MethodSource("getCredentialLabelSamples")
    void createUniqueCredentialLabelTest(UserModel user, String credentialName, String expected) {
        Assertions.assertEquals(expected, AuthenticatorUtils.createUniqueCredentialLabel(user, credentialName));
    }

    private static Stream<Arguments> getCredentialLabelSamples() {
        UserModel noConflictUser = mockUserWithCredentialLabels(List.of("TrustID", "TrustID on Pixel"));
        UserModel gapInSuffixesUser = mockUserWithCredentialLabels(List.of("TrustID on iPhone", "TrustID on iPhone (1)", "TrustID on iPhone (3)"));
        UserModel consecutiveSuffixesUser = mockUserWithCredentialLabels(List.of("TrustID on iPhone", "TrustID on iPhone (1)"));
        UserModel nullListUser = mockUserWithCredentialLabels(null);

        return Stream.of(
                Arguments.of(noConflictUser, "TrustID on iPhone", "TrustID on iPhone"),
                Arguments.of(gapInSuffixesUser, "TrustID on iPhone", "TrustID on iPhone (2)"),
                Arguments.of(consecutiveSuffixesUser, "TrustID on iPhone", "TrustID on iPhone (2)"),
                Arguments.of(nullListUser, "TrustID on iPhone", "TrustID on iPhone")
        );
    }

    private static UserModel mockUserWithCredentialLabels(List<String> labels) {
        UserModel user = Mockito.mock(UserModel.class);
        SubjectCredentialManager credentialManager = Mockito.mock(SubjectCredentialManager.class, Mockito.withSettings().extraInterfaces(CredentialInputUpdater.class));
        Mockito.when(user.credentialManager()).thenReturn(credentialManager);
        Mockito.when(credentialManager.getStoredCredentialsStream()).thenReturn(labels == null ? Stream.empty() : labels.stream().map(AuthenticatorUtilsTest::mockCredential));
        return user;
    }

    private static CredentialModel mockCredential(String label) {
        CredentialModel credential = Mockito.mock(CredentialModel.class);
        Mockito.when(credential.getUserLabel()).thenReturn(label);
        return credential;
    }
}
