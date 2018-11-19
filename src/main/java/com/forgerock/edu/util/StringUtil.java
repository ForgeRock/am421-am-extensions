package com.forgerock.edu.util;

import java.util.Collection;

/**
 * String utility method collection.
 *
 * @author vrg
 */
public class StringUtil {

    /**
     * This method creates a comma separated list from the given collection.
     * @param collection
     * @param separator
     * @return 
     */
    public static String join(Collection<String> collection, String separator) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (String element : collection) {
            if (index++ > 0) {
                sb.append(separator);
            }
            sb.append(element);
        }
        return sb.toString();
    }
}
