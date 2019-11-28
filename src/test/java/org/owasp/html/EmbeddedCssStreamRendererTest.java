package org.owasp.html;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public final class EmbeddedCssStreamRendererTest extends TestCase {

    static class MockStylingPolicy implements AttributePolicy.JoinableAttributePolicy {

        private final String expectedText;
        private final String sanitizedText;
        private final boolean shouldBeCalled;
        private boolean wasCalled = false;

        MockStylingPolicy(String expectedText, String sanitizedText, boolean shouldBeCalled) {
            this.expectedText = expectedText;
            this.sanitizedText = sanitizedText;
            this.shouldBeCalled = shouldBeCalled;
        }

        @Nullable
        @Override
        public String apply(String elementName, String attributeName, String value) {
            assertEquals(expectedText, value);
            wasCalled = true;
            return sanitizedText;
        }

        @Override
        public JoinStrategy<JoinableAttributePolicy> getJoinStrategy() {
            return null;
        }

        void assertPassed() {
            assertEquals(shouldBeCalled, wasCalled);
        }
    }


    static class MockReceiver implements HtmlStreamEventReceiver {

        private final String expectedText;

        MockReceiver(String expectedText) {
            this.expectedText = expectedText;
        }

        @Override
        public void openDocument() { }

        @Override
        public void closeDocument() { }

        @Override
        public void openTag(String elementName, List<String> attrs) { }

        @Override
        public void closeTag(String elementName) { }

        @Override
        public void text(String text) {
            assertEquals(expectedText, text);
        }
    }

    @Test
    public static final void testDontAlterTextInTagsOthersThanStyle() {
        //given
        String someNonCssText = "abracadabra";
        MockStylingPolicy mockStylingPolicy = new MockStylingPolicy("", "", false);
        MockReceiver mockReceiver = new MockReceiver(someNonCssText);
        EmbeddedCssStreamRenderer embeddedCssStreamRenderer = new EmbeddedCssStreamRenderer(mockStylingPolicy, mockReceiver);

        //when
        embeddedCssStreamRenderer.openTag("span", Collections.<String>emptyList());
        embeddedCssStreamRenderer.text(someNonCssText);

        //then
        mockStylingPolicy.assertPassed();
    }

    @Test
    public static final void testSanitizeEmbeddedCss() {
        //given
        String notSanitizedCss = "color: red; background-color: white";
        String sanitizedCss = "background-color: white";

        String notSanitizedClass = ".some-class { " + notSanitizedCss + " }";
        String sanitizedClass = ".some-class { " + sanitizedCss + " }";

        MockStylingPolicy mockStylingPolicy = new MockStylingPolicy(notSanitizedCss, sanitizedCss, true);
        MockReceiver mockReceiver = new MockReceiver(sanitizedClass);
        EmbeddedCssStreamRenderer embeddedCssStreamRenderer = new EmbeddedCssStreamRenderer(mockStylingPolicy, mockReceiver);

        //when
        embeddedCssStreamRenderer.openTag("style", Collections.<String>emptyList());
        embeddedCssStreamRenderer.text(notSanitizedClass);

        //then
        mockStylingPolicy.assertPassed();
    }


    @Test
    public static final void testMalformedCssHandling() {
        //given

        String notSanitizedClass = "xasd d}";

        MockStylingPolicy mockStylingPolicy = new MockStylingPolicy("", "", false);
        MockReceiver mockReceiver = new MockReceiver("");
        EmbeddedCssStreamRenderer embeddedCssStreamRenderer = new EmbeddedCssStreamRenderer(mockStylingPolicy, mockReceiver);

        //when
        embeddedCssStreamRenderer.openTag("style", Collections.<String>emptyList());
        embeddedCssStreamRenderer.text(notSanitizedClass);

        //then
        mockStylingPolicy.assertPassed();
    }
}