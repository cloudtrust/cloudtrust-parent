package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.http.HttpRequest;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

class RequiredActionUtilsTest {
    @ParameterizedTest
    @MethodSource("getRequestSamples")
    void getRequestTest(RequiredActionContext ctx, String httpMethod, String paramName, String expected) {
        HttpRequest httpRequest = ctx.getHttpRequest();
        if (httpRequest != null) {
            Mockito.when(httpRequest.getHttpMethod()).thenReturn(httpMethod);
        }
        Assertions.assertEquals(expected, RequiredActionUtils.getFirstDecodedFormParameter(ctx, paramName));
    }

    private static Stream<Arguments> getRequestSamples() {
        MultivaluedMap<String, String> myDecodedParameters = new MultivaluedHashMap<>();
        myDecodedParameters.put("myParam", Arrays.asList("first", "second", "third"));
        myDecodedParameters.put("myEmptyParam", Collections.emptyList());

        RequiredActionContext noRequestContext = Mockito.mock(RequiredActionContext.class);

        RequiredActionContext context = Mockito.mock(RequiredActionContext.class);
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
