package com.forgerock.edu.oauth2;

import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for parsing and modifying the claims structure providing a
 * fluent API for this.
 *
 * @author vrg
 */
public class Claims {

    private final JSONObject claimsObject;

    public Claims(JSONObject claimsObject) {
        this.claimsObject = claimsObject;
    }

    /**
     * Factory method that parses the given claims string and
     *
     * @param claims
     * @return
     * @throws JSONException
     */
    public static Claims parse(String claims) throws JSONException {
        JSONObject claimsObject = claims == null ? new JSONObject() : new JSONObject(claims);
        return new Claims(claimsObject);
    }

    /**
     * Adds a claim definition to the specified target. If there is already a
     * claim definition with the given target/claimName pair, this method simply
     * replaces the existing claim. You can chain method calls into a single
     * statement, because this method returns with the Claims object itself
     * ({@code this}).
     *
     * @param target Target branch of the claims JSON structure. Should be
     * {@code "userinfo"} or {@code "id_token"}.
     * @param claimName The name of the claim.
     * @param value The value to be assigned with the claim.
     * @return The claim instance ({@code this}).
     * @throws JSONException
     */
    public Claims setClaimValue(String target, String claimName, String value) throws JSONException {
        getTarget(target).put(claimName, value);
        return this;
    }

    /**
     * Adds a claim definition to the specified target. If there is already a
     * claim definition with the given target/claimName pair, this method simply
     * replaces the existing claim. You can chain method calls into a single
     * statement, because this method returns with the Claims object itself
     * ({@code this}).
     *
     * @param target Target branch of the claims JSON structure. Should be
     * {@code "userinfo"} or {@code "id_token"}.
     * @param claimName The name of the claim.
     * @param values The values to be assigned with the claim.
     * @return The claim instance ({@code this}).
     * @throws JSONException
     */
    public Claims setClaimValues(String target, String claimName, Collection<String> values) throws JSONException {
        getTarget(target).put(claimName, toClaimValuesObject(values));
        return this;
    }

    JSONObject getTarget(String targetName) throws JSONException {
        JSONObject target = claimsObject.optJSONObject(targetName);
        if (target == null) {
            target = new JSONObject();
            claimsObject.put(targetName, target);
        }
        return target;
    }

    private static JSONObject toClaimValuesObject(Collection<String> values) throws JSONException {
        JSONArray array = new JSONArray(values);
        JSONObject privObject = new JSONObject();
        privObject.put("values", array);
        return privObject;
    }

    /**
     * Converts the claims structure to a JSON String.
     *
     * @return
     */
    @Override
    public String toString() {
        try {
            return claimsObject.toString(2);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
