package com.forgerock.edu.policy;

//import com.forgerock.edu.authmodule.SelectRole;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.entitlement.conditions.environment.ConditionConstants;
import org.forgerock.openam.utils.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * EntitlementCondition implementation that satisfies if {@link #requiredRole}
 * equals to {@code current}
 *
 * @author vrg
 */
public class SelectedRoleCondition implements EntitlementCondition {

    private String displayType;
    private String requiredRole;
    private static final String REQUIRED_ROLE_FIELD = "requiredRole";
    private static final Debug DEBUG = Debug.getInstance("SelectedRoleCondition");
    private static final String SELECTABLE_ROLES_PROPERTY = "selectableRoles";
    private static final String SELECTED_ROLE_PROPERTY = "selectedRole";

    public String getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    @Override
    public String getDisplayType() {
        return displayType;
    }

    @Override
    public void init(Map<String, Set<String>> map) {
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(REQUIRED_ROLE_FIELD)) {
                requiredRole = getValue(map.get(key));
            }
        }
    }

    @Override
    public void setState(String state) {
        try {
            JSONObject parsedState = new JSONObject(state);
            requiredRole = parsedState.getString(REQUIRED_ROLE_FIELD);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getState() {
        try {
            JSONObject json = new JSONObject();
            json.put(REQUIRED_ROLE_FIELD, requiredRole);
            return json.toString(); // {"requiredRole" : "Phonebook Admin"}
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void validate() throws EntitlementException {
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            // requiredRole is required, cannot be null or empty.
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED);
        }
    }

    /**
     * Extracts one value from a set if the set is nut null and not empty.
     *
     * @param <T>
     * @param values
     * @return The first element in the set or null if values = null or
     * values.isEmpty() = true
     */
    private <T> T getValue(Set<T> values) {
        if (values != null && !values.isEmpty()) {
            return values.iterator().next();
        }
        return null;
    }

    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resource,
            Map<String, Set<String>> environment) throws EntitlementException {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("SelectedRoleCondition.evaluate():entering");
        }

        SSOToken token = (SSOToken) getValue(subject.getPrivateCredentials());

        try {
            String selectedRole = token.getProperty(/*SelectRole.*/SELECTED_ROLE_PROPERTY);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("SelectedRoleCondition.evaluate(): selectedRole: " + selectedRole);
            }
            if (requiredRole != null && requiredRole.equalsIgnoreCase(selectedRole)) {
                //Condition is satisfied
                return ConditionDecision.newSuccessBuilder().build();
            } else {
                //Condition is NOT satisfied
                ConditionDecision.Builder decisionBuilder = 
                        ConditionDecision.newFailureBuilder();
                
                if (isMemberOfRequiredRole(token)) {
                    String requiredAuthScheme = realm + ISAuthConstants.COLON + "testSelectRole";
                    
                    //Adding advice to the condition decision
                    Map<String, Set<String>> advices = new HashMap<>();
                    advices.put(ConditionConstants.AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                            CollectionUtils.asSet(requiredAuthScheme));
                    decisionBuilder.setAdvice(advices);
                }
                return decisionBuilder.build();
            }

        } catch (SSOException e) {
            DEBUG.error("Condition evaluation failed", e);
            throw new EntitlementException(EntitlementException.CONDITION_EVALUATION_FAILED, e);
        }
    }

    /**
     * Checks whether the current user is the member of the
     * {@link #requiredRole} regardless of the user's {@code selectedRole}.
     * <p>
     * {@code SelectRoleNode} shares a special property in {@code SSOToken} called
     * {@code selectableRoles} (the name of this property is exposed in
     * {@link #SELECTABLE_ROLES_PROPERTY}). This contains all the
     * roles which can be selected by the user (because the user has the
     * corresponding group memberships).
     * </p>
     *
     * @param token
     * @return
     * @throws SSOException
     */
    private boolean isMemberOfRequiredRole(SSOToken token) throws SSOException {
        String selectableRolesString = token.getProperty(/*SelectRole.*/SELECTABLE_ROLES_PROPERTY);
        if (selectableRolesString != null) {
            String[] selectableRoles = selectableRolesString.split("\\s*,\\s*");
            for (String selectableRole : selectableRoles) {
                if (selectableRole.equalsIgnoreCase(requiredRole)) {
                    return true;
                }
            }
        }
        return false;
    }
}
