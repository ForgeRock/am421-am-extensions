package com.forgerock.edu.oauth2;

import com.forgerock.edu.policy.ContactListPrivilegesEvaluator;
import com.forgerock.edu.util.OAuth2Util;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.*;
import org.forgerock.oauth2.core.exceptions.*;
import org.forgerock.openam.oauth2.OpenAMScopeValidator;
import org.json.JSONException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * ScopeValidator implementation that handles special scopes representing
 * ContactList privileges. This ScopeValidator delegates the method calls to the
 * matching method of the default implementation ({@link OpenAMScopeValidator})
 * but then decorates the original response. The only customization this
 * ScopeValidator adds is that the user_info endpoint response with the
 * expiration time of the access token. This way the ContactList backend does
 * not need to issue another request to the token info endpoint to determine the
 * token expiration time.
 *
 * @author vrg
 */
@Singleton
public class ContactListScopeValidator implements ScopeValidator {

    private static final Debug DEBUG = Debug.getInstance("ContactListScopeValidator");

    // DONE Ch5L1Ex2: add the @Inject annotation
    @Inject
    private OpenAMScopeValidator openAMScopeValidator;

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope, OAuth2Request request)
            throws InvalidScopeException, ServerException {
        final Set<String> validatedScope = openAMScopeValidator.validateAuthorizationScope(clientRegistration, scope, request);
        DEBUG.message("validateAuthorizationScope validated scope: " + validatedScope);
        return validatedScope;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<String> validateAccessTokenScope(
            ClientRegistration clientRegistration,
            Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException {
        final Set<String> validatedScope = openAMScopeValidator.validateAccessTokenScope(clientRegistration, scope, request);
        DEBUG.message("validateAccessTokenScope validated scope: " + validatedScope);
        return validatedScope;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<String> validateRefreshTokenScope(
            ClientRegistration clientRegistration,
            Set<String> requestedScope,
            Set<String> tokenScope,
            OAuth2Request request) throws ServerException, InvalidScopeException {
        final Set<String> validatedScope = openAMScopeValidator.validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
        DEBUG.message("validateRefreshTokenScope validated scope: " + validatedScope);
        return validatedScope;
    }

    /**
     * Returns the resource owners claim information based on the request and
     * optionally based on the access token. This method is called in two
     * separate use cases:
     * <ul>
     * <li>When collecting the data that will be shown on the consent screen. In
     * this case the {@code token} parameter is null.</li>
     * <li>When the OpenID connect userinfo REST endpoint is called. In this
     * case the {@code token} parameter contains the access token.
     * </li>
     * </ul>
     * <p>
     * In addition to the behavior of
     * {@link OpenAMScopeValidator#getUserInfo(ClientRegistration, AccessToken, OAuth2Request)}
     * method, this implementation adds fields named {@code privileges} and
     * {@code expires_in}. Privileges are extracted from the access token's
     * assigned scopes, the current access token's expiry time is converted into
     * seconds and exposed as a field named {@code expires_in}.
     *
     * @param token   The access token.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, Object>} of the resource owner's
     * information.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    @Override
    public UserInfoClaims getUserInfo(
            ClientRegistration clientRegistration,
            AccessToken token,
            OAuth2Request request)
            throws UnauthorizedClientException, NotFoundException, ServerException, InvalidRequestException {
        UserInfoClaims userInfoClaims = openAMScopeValidator.getUserInfo(clientRegistration, token, request);
        DEBUG.message("getUserInfo claims provided by openAMScopeValidator: " + userInfoClaims.getValues() + ", composite scopes: " + userInfoClaims.getCompositeScopes());
        //DONE Ch5L1Ex2: Add a new claim called "expires_in" which should cointain the token's time to live in seconds.
        //DONE Ch5L1Ex2: If the token is not null, calculate the TTL: (token.getExpiryTime() - System.currentTimeMillis()) / 1000
        //DONE Ch5L1Ex2: Put this value to userInfoClaims.getValues() with the key "expires_in".
        if (token != null) {
            userInfoClaims.getValues().put("expires_in", (token.getExpiryTime() - System.currentTimeMillis()) / 1000);
            DEBUG.message("getUserInfo modified claim set: " + userInfoClaims.getValues());
        }
        return userInfoClaims;
    }

    /**
     * Gets the specified access token's information. This method is called
     * during handling a request sent to {@code /oauth2/tokeninfo} endpoint.
     *
     * @param token The access token.
     * @return A {@code Map<String, Object>} of the access token's information.
     */
    @Override
    public Map<String, Object> evaluateScope(AccessToken token) {
        Map<String, Object> tokenInfo = openAMScopeValidator.evaluateScope(token);
        DEBUG.message("evaluateScope response: " + tokenInfo);
        return tokenInfo;
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to return
     * additional data from an access token request.
     * <br>
     * Any additional data to be returned should be added to the access token by
     * invoking, AccessToken#addExtraData(String, String).
     * <br>
     * This method simply calls
     * {@link OpenAMScopeValidator#additionalDataToReturnFromTokenEndpoint(org.forgerock.oauth2.core.AccessToken, org.forgerock.oauth2.core.OAuth2Request) OpenAMScopeValidator.additionalDataToReturnFromTokenEndpoint}
     * , which adds the token_id field with the signed JWT tokenID if the
     * {@code openid} scope is assigned to the current access token.
     *
     * @param token   The access token.
     * @param request The OAuth2 request.
     * @throws ServerException        If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the
     *                                client's id or the client fails to be authenticated.
     */
    @Override
    public void additionalDataToReturnFromTokenEndpoint(
            AccessToken token,
            OAuth2Request request)
            throws ServerException, InvalidClientException {
        try {
            DEBUG.message("additionalDataToReturnFromTokenEndpoint accessToken: " + token.toMap());
            openAMScopeValidator.additionalDataToReturnFromTokenEndpoint(token, request);
            DEBUG.message("additionalDataToReturnFromTokenEndpoint accessToken after calling OpenAMScopeValidator: " + token.toMap());
        } catch (Exception ex) {
            DEBUG.error("Error in additionalDataToReturnFromTokenEndpoint", ex);
        }
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to return
     * additional data from an authorization request. Returns an empty map.
     *
     * @param tokens  The tokens that will be returned from the authorization
     *                call.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, String>} of the additional data to return.
     */
    @Override
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(
            Map<String, Token> tokens,
            OAuth2Request request) {
        Map<String, String> additionalData =
                openAMScopeValidator.additionalDataToReturnFromAuthorizeEndpoint(tokens, request);
        DEBUG.message("additionalDataToReturnFromAuthorizeEndpoint: " + additionalData);
        return additionalData;
    }

    @Override
    public Set<String> validateBackChannelAuthorizationScope(ClientRegistration cr, Set<String> set, OAuth2Request oar) throws InvalidScopeException, ServerException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void modifyAccessToken(AccessToken accessToken, OAuth2Request request) throws NotFoundException, ServerException, UnauthorizedClientException {
        openAMScopeValidator.modifyAccessToken(accessToken, request);
        SSOToken ssoToken = OAuth2Util.extractSSOToken(request);
        if (ssoToken != null) {
            hardCodeClaimValues(accessToken, ssoToken);
        }
    }

    private void hardCodeClaimValues(AccessToken accessToken, SSOToken ssoToken) {
        try {
            String claims = accessToken.getClaims();
            DEBUG.message("Original claims string: " + claims);
            String selectedRole = ssoToken.getProperty("selectedRole");
            if (DEBUG.messageEnabled()) {
                DEBUG.message("selectedRole: " + selectedRole);
            }
            Set<String> privileges = ContactListPrivilegesEvaluator.getContactListPrivileges(ssoToken);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("privileges: " + privileges);
            }

            claims = Claims.parse(claims)
                    .setClaimValue("userinfo", "selectedRole", selectedRole)
                    .setClaimValue("id_token", "selectedRole", selectedRole)
                    .setClaimValues("userinfo", "contactlist-privileges", privileges)
                    .setClaimValues("id_token", "contactlist-privileges", privileges)
                    .toString();
            accessToken.setClaims(claims);
            DEBUG.message("Replaced claims string: " + claims);
        } catch (SSOException | EntitlementException | JSONException ex) {
            DEBUG.error("Error during extending claims: ", ex);
        }
    }
}
