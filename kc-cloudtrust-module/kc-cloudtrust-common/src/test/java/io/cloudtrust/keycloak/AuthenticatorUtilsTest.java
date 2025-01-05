package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.http.HttpRequest;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
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

    public static Stream<Arguments> getRequestSamples() {
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
}
