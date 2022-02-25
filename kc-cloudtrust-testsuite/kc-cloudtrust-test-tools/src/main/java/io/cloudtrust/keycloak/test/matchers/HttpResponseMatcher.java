package io.cloudtrust.keycloak.test.matchers;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class HttpResponseMatcher extends BaseMatcher<HttpResponse> {
	private HttpResponse httpResponse;
	private Predicate<HttpResponse> predicate;
	private Function<HttpResponse, String> describer;

	private HttpResponseMatcher(Predicate<HttpResponse> predicate, Function<HttpResponse, String> describer) {
		this.predicate = predicate;
		this.describer = describer;
	}

	@Override
	public boolean matches(Object item) {
		if (!(item instanceof HttpResponse)) {
			return false;
		}
		this.httpResponse = (HttpResponse)item;
		return this.predicate.test(this.httpResponse);
	}

	@Override
	public void describeTo(Description description) {
		if (this.httpResponse==null) {
			description.appendText("HttpResponse is null -or not an HttpResponse-");
		} else {
			description.appendText(describer.apply(this.httpResponse));
		}
	}

	public static BaseMatcher<HttpResponse> isStatus(int httpStatus) {
		return new HttpResponseMatcher(
				resp -> resp.getStatusLine().getStatusCode()==httpStatus,
				resp -> "Http status is "+resp.getStatusLine().getStatusCode()+" when expected status is "+httpStatus
				);
	}

	public static BaseMatcher<HttpResponse> isHeaderNotEmpty(String headerName) {
		return new HttpResponseMatcher(
				resp -> StringUtils.isNotEmpty(resp.getFirstHeader(headerName).getValue()),
				resp -> "Http header "+headerName+" is not set"
				);
	}

	public static BaseMatcher<HttpResponse> hasHeader(String headerName, int headerValue) {
		return hasHeader(headerName, String.valueOf(headerValue));
	}

	public static BaseMatcher<HttpResponse> hasHeader(String headerName, String headerValue) {
		return new HttpResponseMatcher(
				resp -> headerValue!=null && headerValue.equals(resp.getFirstHeader(headerName).getValue()),
				resp -> "Http header "+headerName+" is "+resp.getFirstHeader(headerName).getValue()+" when expected value is "+headerValue
				);
	}
}
