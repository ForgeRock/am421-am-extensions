package com.forgerock.edu.saml;

import com.forgerock.edu.policy.ContactListPrivilegesEvaluator;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.saml2.plugins.DefaultLibraryIDPAttributeMapper;
import java.util.List;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.debug.Debug;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adds contactList privileges as SAML2 Attributes, if
 * {@code contactList-privileges} attribute is mapped. The value of this attribute
 * contains multiple strings. Each of them represents a single privilege like
 * {@code "contactList://privileges/entries/search"}. Only the allowed privileges
 * are listed.
 *
 * @author vrg
 */
public class ContactListPrivilegesIDPAttributeMapper extends DefaultLibraryIDPAttributeMapper {

    private static final Debug DEBUG = Debug.getInstance("ContactListPrivilegesIDPAttributeMapper");
    private static final String CONTACTLIST_PRIVILEGES_ATTRIBUTE = "contactList-privileges";

    /**
     * Returns with false to let superclass read user profile attributes based
     * on idp entity config in its getAttributes method.
     *
     * @param realm
     * @return false
     */
    @Override
    protected boolean isIgnoredProfile(Object session, String realm) {
        return false;
    }

    @Override
    public List<Attribute> getAttributes(Object session, String hostEntityID, String remoteEntityID, String realm)
            throws SAML2Exception {

        DEBUG.message("getAttributes(hostEntity = " + hostEntityID
                + ", remoteEntity = " + remoteEntityID + ", realm = " + realm + ")");

        List<Attribute> attributes = super.getAttributes(session, hostEntityID, remoteEntityID, realm);

        if (attributes != null) {
            Map<String, String> configMap = getConfigAttributeMap(realm, hostEntityID, IDP);
            if (configMap == null || configMap.isEmpty()) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Configuration map is not defined.");
                }
                return attributes;
            }

            Set<String> samlAttributeNames = new HashSet<>();
            // Finding entries in configMap, where the value equals to special
            // attribute CONTACTLIST_PRIVILEGES_ATTRIBUTE.
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                if (CONTACTLIST_PRIVILEGES_ATTRIBUTE.equals(entry.getValue())) {
                    //entry.key contains the SAML attribute's name.
                    samlAttributeNames.add(entry.getKey());
                }
            }

            if (!samlAttributeNames.isEmpty()) {
                Set<String> privilegeValues;
                try {
                    privilegeValues = ContactListPrivilegesEvaluator.getContactListPrivileges((SSOToken) session);
                    for (String samlAttributeName : samlAttributeNames) {
                        Attribute attribute = getSAMLAttribute(samlAttributeName,
                                null,
                                privilegeValues,
                                hostEntityID,
                                remoteEntityID,
                                realm);
                        attributes.add(attribute);
                    }
                } catch (EntitlementException ex) {
                    DEBUG.error("Error during evaluating policies", ex);
                }
            }
        }

        return attributes;
    }
}
