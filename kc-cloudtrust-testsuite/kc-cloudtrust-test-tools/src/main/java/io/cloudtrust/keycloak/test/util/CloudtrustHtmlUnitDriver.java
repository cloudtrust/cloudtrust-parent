package io.cloudtrust.keycloak.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.css.parser.CSSErrorHandler;
import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSParseException;
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler;

/**
 * This extend of HtmlUnitDriver exists to ignore some flooding errors or warnings
 * @author fpe
 *
 */
public class CloudtrustHtmlUnitDriver extends HtmlUnitDriver implements CSSErrorHandler {
	private final List<String> ignorePatterns = new ArrayList<>();
	private final DefaultCssErrorHandler defaultHandler;

	public CloudtrustHtmlUnitDriver(Collection<String> ignorePatterns) {
		defaultHandler = new DefaultCssErrorHandler();
		this.getWebClient().setCssErrorHandler(this);
		this.ignorePatterns.addAll(ignorePatterns);
	}

	@Override
	public void warning(CSSParseException exception) throws CSSException {
		if (!ignore(exception)) {
			defaultHandler.warning(exception);
		}
	}
	
	@Override
	public void fatalError(CSSParseException exception) throws CSSException {
		if (!ignore(exception)) {
			defaultHandler.fatalError(exception);
		}
	}
	
	@Override
	public void error(CSSParseException exception) throws CSSException {
		if (!ignore(exception)) {
			defaultHandler.error(exception);
		}
	}

	private boolean ignore(CSSParseException exception) {
		String uri = exception.getURI();
		return uri!=null && ignorePatterns.stream().anyMatch(uri::contains);
	}
}
