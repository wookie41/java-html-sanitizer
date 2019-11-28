package org.owasp.html;

import com.steadystate.css.parser.CSSOMParser;

import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class EmbeddedCssStreamRenderer implements HtmlStreamEventReceiver {

    private static final String STYLE = "style";
    private static final String EMPTY_STRING = "";

    private final AttributePolicy.JoinableAttributePolicy stylingPolicy;
    private final HtmlStreamEventReceiver delegateReceiver;

    private final CSSOMParser cssParser = new CSSOMParser();

    private String lastOpenedElement = "";

    public EmbeddedCssStreamRenderer(AttributePolicy.JoinableAttributePolicy stylingPolicy, HtmlStreamEventReceiver delegateReceiver) {
        this.stylingPolicy = stylingPolicy;
        this.delegateReceiver = delegateReceiver;
    }

    @Override
    public void openDocument() {
        delegateReceiver.openDocument();
    }

    @Override
    public void closeDocument() {
        delegateReceiver.closeDocument();
    }

    @Override
    public void openTag(String elementName, List<String> attrs) {
        lastOpenedElement = elementName;
        delegateReceiver.openTag(elementName, attrs);
    }

    @Override
    public void closeTag(String elementName) {
        delegateReceiver.closeTag(elementName);
    }

    @Override
    public void text(String text) {
        if (STYLE.equalsIgnoreCase(lastOpenedElement)) {
            delegateReceiver.text(sanitizeCSS(text));
        } else {
            delegateReceiver.text(text);
        }
    }

    private String sanitizeCSS(String css) {
        StringBuilder sanitizedCss = new StringBuilder();
        InputSource source = new InputSource(new StringReader(css));

        try {
            CSSStyleSheet sheet = cssParser.parseStyleSheet(source, null, null);
            CSSRuleList cssRules = sheet.getCssRules();
            for (int i = 0; i < cssRules.getLength(); ++i) {
                CSSRule item = cssRules.item(i);
                if (item instanceof CSSStyleRule) {
                    CSSStyleRule rule = (CSSStyleRule) item;
                    String ruleToSanitize = rule.getStyle().getCssText();
                    String sanitizedRule = stylingPolicy.apply(STYLE, STYLE, ruleToSanitize);
                    rule.getStyle().setCssText(sanitizedRule == null ? EMPTY_STRING : sanitizedRule);
                    sanitizedCss.append(rule.getCssText());
                }
            }
            return sanitizedCss.toString();
        } catch (IOException e) {
            return EMPTY_STRING;
        }
    }
}
