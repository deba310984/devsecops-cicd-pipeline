package com.debjit.app;

/**
 * Minimal output-encoding helper. Encoding untrusted input before reflecting it
 * back is a baseline defence against reflected XSS — the kind of secure-coding
 * practice a DevSecOps pipeline is meant to enforce.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#x27;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
