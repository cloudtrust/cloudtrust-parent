package io.cloudtrust.keycloak.test.matchers;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.hamcrest.BaseMatcher;

public class HttpResponseMatcher extends AbstractMatchers<HttpResponse> {
	protected HttpResponseMatcher(Predicate<HttpResponse> predicate, Function<HttpResponse, String> describer) {
		super(predicate, describer);
	}

	@Override
	protected HttpResponse convert(Object item) {
		return item instanceof HttpResponse ? (HttpResponse)item : null;
	}

	private static String getFirstHeaderValue(HttpResponse httpResponse, String name) {
		Header hdr = httpResponse.getFirstHeader(name);
		return hdr!=null ? hdr.getValue() : null;
	}

	public static BaseMatcher<HttpResponse> isStatus(int httpStatus) {
		return new HttpResponseMatcher(
				resp -> resp.getStatusLine().getStatusCode()==httpStatus,
				resp -> "Http status is "+resp.getStatusLine().getStatusCode()+" when expected status is "+httpStatus
				);
	}

	public static BaseMatcher<HttpResponse> isHeaderNotEmpty(String headerName) {
		return new HttpResponseMatcher(
				resp -> StringUtils.isNotEmpty(getFirstHeaderValue(resp, headerName)),
				resp -> String.format("Http header %s is not set", headerName)
				);
	}

	public static BaseMatcher<HttpResponse> hasNoHeader(String headerName) {
		return new HttpResponseMatcher(
				resp -> resp.getFirstHeader(headerName)==null,
				resp -> String.format("Http header %s is present when it is expected to be missing", headerName)
				);
	}

	public static BaseMatcher<HttpResponse> hasHeader(String headerName, int headerValue) {
		return hasHeader(headerName, String.valueOf(headerValue));
	}

	public static BaseMatcher<HttpResponse> hasHeader(String headerName, String headerValue) {
		return new HttpResponseMatcher(
				resp -> headerValue!=null && headerValue.equals(getFirstHeaderValue(resp, headerName)),
				resp -> "Http header "+headerName+" is "+getFirstHeaderValue(resp, headerName)+" when expected value is "+headerValue
				);
	}

	public static BaseMatcher<HttpResponse> hasNotHeader(String headerName, String headerValue) {
		return new HttpResponseMatcher(
				resp -> headerValue!=null && !headerValue.equals(getFirstHeaderValue(resp, headerName)),
				resp -> "Http header "+headerName+" is "+getFirstHeaderValue(resp, headerName)+" when value should not be "+headerValue
				);
	}
}
