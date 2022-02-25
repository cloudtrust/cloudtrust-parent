package io.cloudtrust.keycloak.test.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class RegexMatcher extends BaseMatcher<String> {
	private final String regex;
	private final boolean matches;
	private String input;

	public RegexMatcher(String regex, boolean matches) {
		this.regex = regex;
		this.matches = matches;
	}

	@Override
	public boolean matches(Object item) {
		if (!(item instanceof String)) {
			return false;
		}
		this.input = (String)item;
		boolean match = input.matches(this.regex);
		return matches ? match : !match;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(input).appendText(matches ? " does not match " : " matches ").appendText(this.regex);
	}

	public static BaseMatcher<String> matchesRegex(String regex) {
		return new RegexMatcher(regex, true);
	}

	public static BaseMatcher<String> doesNotMatchRegex(String regex) {
		return new RegexMatcher(regex, false);
	}
}
