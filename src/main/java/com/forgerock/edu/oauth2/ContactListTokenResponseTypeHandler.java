package com.forgerock.edu.oauth2;

import com.forgerock.edu.policy.ContactListPrivilegesEvaluator;
import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.Grant;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResponseTypeHandler;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.TokenResponseTypeHandler;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.token.TokenStore;
import org.json.JSONException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import static org.forgerock.openam.oauth2.OAuth2Constants.AuthorizationEndpoint.TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;

/**
 * This ResponseTypeHandler implementation handles the response type named
 * "token" in a ContactList specific way. The purpose of this handler is to
 * capture the user's ContactList specific privileges in the requested claim set
 * in the token before the token is stored in the {@link TokenStore}. This
 * implementation The {@link #handle(java.lang.String, java.util.Set, org.forgerock.oauth2.core.ResourceOwner, java.lang.String, java.lang.String, java.lang.String, org.forgerock.oauth2.core.OAuth2Request, java.lang.String, java.lang.String)
 * }
 * method is basically copied from the
 * {@link TokenResponseTypeHandler#handle(java.lang.String, java.util.Set, org.forgerock.oauth2.core.ResourceOwner, java.lang.String, java.lang.String, java.lang.String, org.forgerock.oauth2.core.OAuth2Request, java.lang.String, java.lang.String) TokenResponseTypeHandler's handle method}.
 * The only difference is that this handler calculates the ContactList specific
 * privilege set (see
 * {@link #addContactListPrivilegesAsClaim(org.forgerock.oauth2.core.OAuth2Request, java.lang.String)}) and
 * places it into the requested claim set as a claim with hard-coded values.
 *
 * @author vrg
 */
@Singleton
public class ContactListTokenResponseTypeHandler implements ResponseTypeHandler {

    private static final Debug DEBUG = Debug.getInstance("ContactListTokenResponseTypeHandler");
    private final TokenStore tokenStore;

    @Inject
    public ContactListTokenResponseTypeHandler(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map.Entry<String, Token> handle(String tokenType, Set<String> scope, ResourceOwner resourceOwner, String clientId, String redirectUri, String nonce, OAuth2Request request, String codeChallenge, String codeChallengeMethod) throws InvalidClientException, ServerException, NotFoundException, UnauthorizedClientException {
        String claims = null;
        //only pass the claims param if this is a request to the authorize endpoint
        if (request.getParameter(OAuth2Constants.Params.CODE) == null) {
            claims = request.getParameter(OAuth2Constants.Custom.CLAIMS);
            claims = addContactListPrivilegesAsClaim(request, claims);
        }
        
        // Note: use the default implementation from TokenResponseTypeHandler 6.5.1
        request.setContextFor(OAuth2Request.ContextKey.NEW_GRANT_SET, false);
        request.setContextFor(OAuth2Request.ContextKey.CLIENT_ID, clientId);
        request.setContextFor(OAuth2Request.ContextKey.RESOURCE_OWNER, resourceOwner.getId());
        Grant grant = this.tokenStore.createGrant(clientId, resourceOwner.getId(), scope, request);
        this.tokenStore.saveNewGrant(grant, request);
        AccessToken generatedAccessToken = this.tokenStore.createAccessToken(grant, TOKEN, tokenType, nonce, claims, request, resourceOwner.getAuthTime(), scope, resourceOwner.getAuthLevel());
        this.tokenStore.saveNewAccessToken(generatedAccessToken, request);
        this.tokenStore.saveGrantSet(request);
        return new AbstractMap.SimpleEntry(ACCESS_TOKEN, generatedAccessToken);
    }

    /**
     * Evaluates the user's privileges and adds it into the claims structure.
     * @param request The current OAuth2Request
     * @param claims The initial claims JSON structure as a String.
     * @return The modified claims JSON as a String.
     */
    String addContactListPrivilegesAsClaim(OAuth2Request request, String claims) {
        try {
            DEBUG.message("Original claims string: " + claims);
            claims = request.getParameter(OAuth2Constants.Custom.CLAIMS);
            Set<String> privileges = ContactListPrivilegesEvaluator.evaluatePrivileges(request);
            claims = Claims.parse(claims)
                    //DONE Ch5L1Ex2: Put a new claim definition named "contactlist-privileges" into the "userinfo" branch - this is needed when the userinfo endpoint is used.
                    //DONE Ch5L1Ex2: Provide the user's privilege set as the hard-coded value for the "contactlist-privileges" claim.
                    //DONE Ch5L1Ex2: Place the same claim definition to the "id_token" branch  - this is relevant during the id_token generation.
                    //DONE Ch5L1Ex2: Hint: use the setClaimValues method of the Claims class to add
                    .setClaimValues("userinfo", "contactlist-privileges", privileges)
                    .setClaimValues("id_token", "contactlist-privileges", privileges)
                    .toString();
            DEBUG.message("Replaced claims string: " + claims);
        } catch (SSOException | EntitlementException | JSONException ex) {
            DEBUG.error("Error during extending claims: ", ex);
        }
        return claims;
    }

    @Override
    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.FRAGMENT;
    }

}
