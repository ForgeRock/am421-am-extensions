package com.forgerock.edu.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * Builder class to build a UID. UIDs do not contain special characters or
 * whitespace characters. Characters with accents changed to the accentless
 * version, whitespace characters are changed to underscores. This class follows the
 * builder pattern and exposes a fluent API.
 * <p>
 * Usage: {@code String uid = new UIDBuilder().add("Varga").addSeparator().add("József Mihály").build();}
 * </p>
 *
 * <p>
 * which results in
 * </p>
 * 
 * <p>
 * {@code "Varga_Jozsef_Mihaly"}
 * </p>
 *
 * @author vrg
 */
public class UIDBuilder {

    StringBuilder builder = new StringBuilder();

    public UIDBuilder add(String part) {
        builder.append(part);
        return this;
    }

    public UIDBuilder addSeparator() {
        builder.append(" ");
        return this;
    }

    private static String removeAccents(CharSequence text) {
        return text == null ? null
                : Normalizer.normalize(text, Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public String build() {
        return removeAccents(builder).replaceAll("\\w+", " ").trim().replace(" ", "_");
    }
}
