package com.forgerock.edu.oauth2;

import com.forgerock.edu.policy.ContactListPrivilegesEvaluator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.*;
import org.forgerock.openam.oauth2.OpenAMScopeValidator;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

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
    private static final String PRIVILEGE_PREFIX = "privilege://";

    @Inject
    private OpenAMScopeValidator openAMScopeValidator;

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope, OAuth2Request request)
            throws InvalidScopeException, ServerException {
        final Set<String> validatedScope = openAMScopeValidator.validateAuthorizationScope(clientRegistration, scope, request);
        addScopesBasedOnPrivileges(validatedScope, request);
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
        DEBUG.message("validateAccessTokenScope request:" + request.getBody() + ", scope: " + scope);
        final Set<String> validatedScope = openAMScopeValidator.validateAccessTokenScope(clientRegistration, scope, request);
//        addScopesBasedOnPrivileges(validatedScope, request);
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
        DEBUG.message("validateRefreshTokenScope " + request);
        final Set<String> validatedScope = openAMScopeValidator.validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
//        addScopesBasedOnPrivileges(validatedScope, request);
        return validatedScope;
    }

    /**
     * Removes all scopes starting with {@code phonebook://privileges/} which is
     * not in the privilege set of the current user session. Privileges are
     * calculated with
     * {@link #evaluatePrivileges(org.forgerock.oauth2.core.OAuth2Request)}
     * method.
     *
     * @param scopes Modifiable set containing all the scopes. This set will be
     * reduced based on the privileges
     * @param request Current OAuth2Request.
     * @throws InvalidScopeException
     * @throws ServerException
     */
    void addScopesBasedOnPrivileges(Set<String> scopes, OAuth2Request request)
            throws InvalidScopeException, ServerException {
        try {
            Set<String> privilegeSet = evaluatePrivileges(request);
            privilegeSet.stream()
                    .map((privilege) -> PRIVILEGE_PREFIX + privilege)
                    .forEach((scope) -> scopes.add(scope));
        } catch (SSOException | EntitlementException ex) {
            DEBUG.error("Exception during determining privileges", ex);
        }

    }

    /**
     * Gets the resource owners information based on an issued access token. In
     * addition to the behavior of
     * {@link OpenAMScopeValidator#getUserInfo(org.forgerock.oauth2.core.AccessToken, org.forgerock.oauth2.core.OAuth2Request) OpenAMScopeValidator.getUserInfo}
     * method, this implementation adds fields named {@code privileges} and
     * {@code expires_in}. Privileges are extracted from the access token's
     * assigned scopes, the current access token's expiry time is converted into
     * seconds and exposed as a field named {@code expires_in}.
     *
     * @param token The access token.
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
        DEBUG.message("getUserInfo " + request);
        UserInfoClaims userInfoClaims = openAMScopeValidator.getUserInfo(clientRegistration, token, request);
        if (token != null) {
            List<String> privileges = extractPrivileges(token);
            userInfoClaims.getValues().put("contactlist-privileges", privileges);
            // TODO 05_01: Add the expires_in property value to the user claim info in seconds.
            userInfoClaims.getValues().put("expires_in", (token.getExpiryTime() - System.currentTimeMillis()) / 1000);
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
        DEBUG.message("evaluateScope " + token);
        List<String> privileges = extractPrivileges(token);
        Map<String, Object> tokenInfo = openAMScopeValidator.evaluateScope(token);
        //removing extra attributes for each privilege
        for (String privilege : privileges) {
            tokenInfo.remove(privilege);
        }
        tokenInfo.put("contactlist-privileges", privileges); //adding privileges as a single extra attribute
        return tokenInfo;
    }

    /**
     * Extracts privileges list from a given AccessToken. Privileges are
     * conventionally named scopes, starting with
     * {@link ContactListPrivilegesEvaluator#PRIVILEGES_RESOURCE} ({@code phonebook://privileges/}).
     *
     * @param token
     * @return
     */
    List<String> extractPrivileges(AccessToken token) {
        final int prefixLength = PRIVILEGE_PREFIX.length();
        return token.getScope().stream()
                .filter((scope) -> (scope.startsWith(PRIVILEGE_PREFIX)))
                .map((scope) -> scope.substring(prefixLength))
                .collect(Collectors.toList());
    }

    /**
     * Evaluates privileges based on the current SSOToken extracted from the
     * current {@code HttpServletRequest} object. Uses {@link SSOTokenManager}
     * to create the {@link SSOToken} based on the current
     * {@link HttpServletRequest}, then calls
     * {@link ContactListPrivilegesEvaluator#getContactListPrivileges(com.iplanet.sso.SSOToken, java.lang.String)}
     * method to evaluate privileges of the user.
     *
     * @param request
     * @return
     * @throws SSOException
     * @throws EntitlementException
     */
    Set<String> evaluatePrivileges(OAuth2Request request) throws SSOException, EntitlementException {
        //extracting HttpServletRequest from OAuth2Request:
        final HttpServletRequest httpServletRequest = request.getServletRequest();
                // was: ServletUtils.getRequest((Request) request.getRequest());

        final SSOTokenManager tokenManager = SSOTokenManager.getInstance();
        //extracting SSOToken from HttpServletRequest:
        SSOToken ssoToken = tokenManager.createSSOToken(httpServletRequest);
        if (ssoToken == null) {
            DEBUG.warning("evaluatePrivileges: Could not find SSOToken in OAuth2Request! Returning with an empty privileges set.");
            return new HashSet<>();
        }
        Set<String> privileges = ContactListPrivilegesEvaluator.getContactListPrivileges(ssoToken);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("evaluatePrivileges: Returning with: " + privileges);
        }
        return privileges;
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
     * @param token The access token.
     * @param request The OAuth2 request.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the
     * client's id or the client fails to be authenticated.
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
     * @param tokens The tokens that will be returned from the authorization
     * call.
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

}
