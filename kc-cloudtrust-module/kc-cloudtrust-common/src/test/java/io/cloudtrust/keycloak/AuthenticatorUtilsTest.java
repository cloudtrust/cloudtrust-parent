package io.cloudtrust.keycloak;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class AuthenticatorUtilsTest {
    private static final String CONF_NAME = "myConfig";
    private static final String CONF_VALUE = "myValue";

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

    @Test
    void getMandatoryConfig_Success() {
        AuthenticationFlowContext context = Mockito.mock(AuthenticationFlowContext.class);
        AuthenticatorConfigModel configModel = Mockito.mock(AuthenticatorConfigModel.class);

        Mockito.when(context.getAuthenticatorConfig()).thenReturn(configModel);
        Mockito.when(configModel.getConfig()).thenReturn(Collections.singletonMap(CONF_NAME, CONF_VALUE));

        String result = AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME);
        Assertions.assertEquals(CONF_VALUE, result);
    }

    @Test
    void getMandatoryConfig_MissingConfig() {
        AuthenticationFlowContext context = Mockito.mock(AuthenticationFlowContext.class);
        AuthenticatorConfigModel configModel = Mockito.mock(AuthenticatorConfigModel.class);

        Mockito.when(context.getAuthenticatorConfig()).thenReturn(null);
        Assertions.assertThrows(CloudtrustRuntimeException.class, () -> AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME));

        Mockito.when(context.getAuthenticatorConfig()).thenReturn(configModel);
        Mockito.when(configModel.getConfig()).thenReturn(null);
        Assertions.assertThrows(CloudtrustRuntimeException.class, () -> AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME));

        Mockito.when(context.getAuthenticatorConfig()).thenReturn(configModel);
        Mockito.when(configModel.getConfig()).thenReturn(Collections.emptyMap());
        Assertions.assertThrows(CloudtrustRuntimeException.class, () -> AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME));
    }

    @Test
    void getMandatoryConfig_EmptyString() {
        AuthenticationFlowContext context = Mockito.mock(AuthenticationFlowContext.class);
        AuthenticatorConfigModel configModel = Mockito.mock(AuthenticatorConfigModel.class);

        Map<String, String> config = new HashMap<>();

        config.put(CONF_NAME, "");
        Mockito.when(context.getAuthenticatorConfig()).thenReturn(configModel);
        Assertions.assertThrows(CloudtrustRuntimeException.class, () -> AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME));

        config.put(CONF_NAME, "   ");
        Mockito.when(context.getAuthenticatorConfig()).thenReturn(configModel);
        Mockito.when(configModel.getConfig()).thenReturn(config);
        Assertions.assertThrows(CloudtrustRuntimeException.class, () -> AuthenticatorUtils.getMandatoryConfig(context, CONF_NAME));
    }

}
