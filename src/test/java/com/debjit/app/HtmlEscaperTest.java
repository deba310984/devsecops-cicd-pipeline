package com.debjit.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlEscaperTest {

    @Test
    void escapesScriptTag() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                HtmlEscaper.escape("<script>alert(1)</script>"));
    }

    @Test
    void escapesQuotesAndAmpersand() {
        assertEquals("a &amp; b &quot;c&quot; &#x27;d&#x27;",
                HtmlEscaper.escape("a & b \"c\" 'd'"));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("hello world", HtmlEscaper.escape("hello world"));
    }

    @Test
    void handlesNull() {
        assertEquals("", HtmlEscaper.escape(null));
    }
}
