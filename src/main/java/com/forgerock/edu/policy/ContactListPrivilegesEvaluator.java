package com.forgerock.edu.policy;

import com.forgerock.edu.util.OAuth2Util;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.openam.utils.CollectionUtils;

/**
 *
 * @author vrg
 */
public class ContactListPrivilegesEvaluator {

    private static final Debug DEBUG = Debug.getInstance("ContactListPrivilegesEvaluator");
    private static final String CONTACTLIST_PRIVILEGES_APP = "ContactListPrivileges";
    public static final String PRIVILEGES_RESOURCE = "privileges";

    /**
     * Simplified version of {@link #getContactListPrivileges(com.iplanet.sso.SSOToken)
     * } method that returns a List&lt;String&gt;. This method does not throw an
     * {@link EntitlementException}, when there is an error during the policy
     * evaluation it returns an empty list.
     *
     * @param tokenId
     * @return list of privileges or an empty list if there is a problem during
     * the policy evaluation.
     */
    public static List<String> getContactListPrivilegesArray(String tokenId) {
        try {
            SSOTokenManager tokenManager = SSOTokenManager.getInstance();
            SSOToken ssoToken = tokenManager.createSSOToken(tokenId);
            return getContactListPrivileges(ssoToken)
                    .stream().collect(Collectors.toList());
        } catch (EntitlementException|SSOException ex) {
            DEBUG.error("Policy evaluation error", ex);
            //Returning with an empty List
            return new ArrayList<>();
        }
    }

    /**
     * Returns with the ContactList related privileges for the current user
     * session. Uses Entitlement engine's policy evaluator API to retrieve
     * privileges.
     *
     * @param userToken
     * @return Set of privileges which are assigned with the current user
     * session like {@code "phonebook://privileges/entries/add"}. Only the
     * allowed privileges are included.
     * @throws EntitlementException
     */
    public static Set<String> getContactListPrivileges(SSOToken userToken)
            throws EntitlementException {

        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

        Subject admin = SubjectUtils.createSubject(adminToken);
        Subject user = SubjectUtils.createSubject(userToken);

        Set<String> resources = CollectionUtils.asSet(PRIVILEGES_RESOURCE);

        Evaluator evaluator = new Evaluator(admin, CONTACTLIST_PRIVILEGES_APP);

        Map<String, Set<String>> env = new HashMap<>();

        List<Entitlement> entitlements = evaluator.evaluate("/", user, resources, env);

        Set<String> privileges = new TreeSet<>();

        for (Entitlement entitlement : entitlements) {
            for (Map.Entry<String, Boolean> privilege : entitlement.getActionValues().entrySet()) {
                if (privilege.getValue()) {
                    privileges.add(privilege.getKey());
                }
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Privilege Set: " + privileges);
        }

        return privileges;
    }

    /**
     * Evaluates privileges based on the current SSOToken extracted from the
     * current {@code HttpServletRequest} object. Uses
     * {@link OAuth2Util#extractSSOToken(org.forgerock.oauth2.core.OAuth2Request)}
     * to create the {@link SSOToken} based on the current
     * {@link OAuth2Request}, then calls
     * {@link #getContactListPrivileges(com.iplanet.sso.SSOToken)}
     * method to evaluate privileges of the user.
     *
     * @param request
     * @return
     * @throws SSOException
     * @throws EntitlementException
     */
    public static Set<String> evaluatePrivileges(OAuth2Request request) throws SSOException, EntitlementException {
        //extracting SSOToken from OAuth2Request:
        SSOToken ssoToken = OAuth2Util.extractSSOToken(request);
        if (ssoToken == null) {
            DEBUG.warning("evaluatePrivileges: Could not find SSOToken in OAuth2Request! Returning with an empty privileges set.");
            return new HashSet<>();
        }
        Set<String> privileges = getContactListPrivileges(ssoToken);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("evaluatePrivileges: Returning with: " + privileges);
        }
        return privileges;
    }

}
