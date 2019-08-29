package com.forgerock.edu.oauth2;

import com.forgerock.edu.policy.ContactListPrivilegesEvaluator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.*;
import org.forgerock.openam.oauth2.OpenAMScopeValidator;

/**
 * ScopeValidator implementation that handles special scopes representing
 * ContactList privileges. This ScopeValidator delegates the method calls to the
 * matching method of the default implementation ({@link OpenAMScopeValidator})
 * but then decorates the original response. The main added value to the
 * original behavior is that this class calculates the users's ContactList
 * privileges and dynamically adds these as scopes to the requested and
 * validated scope list. These special scopes are all prefixed with
 * {@link #PRIVILEGE_PREFIX}. On the other hand it adds a field to the user_info
 * endpoint response with the expiration time of the access token. This way the
 * ContactList backend does not need to issue another request to the token info
 * endpoint to determine the token expiration time.
 *
 * @author vrg
 */
@Singleton
public class ScopeExtenderContactListScopeValidator implements ScopeValidator {

    private static final Debug DEBUG = Debug.getInstance("ContactListScopeValidator");
    private static final String PRIVILEGE_PREFIX = "privilege://";

    @Inject
    private OpenAMScopeValidator openAMScopeValidator;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customize
     * the scope requested when authorization is requested. First calls
     * {@link OpenAMScopeValidator}'s implementation, then calls
     * {@link #addScopesBasedOnPrivileges(java.util.Set, org.forgerock.oauth2.core.OAuth2Request)}
     * to remove all the scopes representing a privilege that is not currently
     * assigned to the current user session.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws InvalidScopeException
     * @throws ServerException
     */
    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope, OAuth2Request request)
            throws InvalidScopeException, ServerException {
        final Set<String> validatedScope = openAMScopeValidator.validateAuthorizationScope(clientRegistration, scope, request);
        return validatedScope;
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to customize
     * the scope requested when an access token is requested. First calls
     * {@link OpenAMScopeValidator}'s implementation, then calls
     * {@link #addScopesBasedOnPrivileges(java.util.Set, org.forgerock.oauth2.core.OAuth2Request)}
     * to remove all the scopes representing a privilege that is not currently
     * assigned to the current user session.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws InvalidScopeException
     * @throws ServerException
     */
    @Override
    public Set<String> validateAccessTokenScope(
            ClientRegistration clientRegistration,
            Set<String> scope,
            OAuth2Request request) throws InvalidScopeException, ServerException {
        DEBUG.message("validateAccessTokenScope request:" + request.getBody() + ", scope: " + scope);
        final Set<String> validatedScope = openAMScopeValidator.validateAccessTokenScope(clientRegistration, scope, request);
        addScopesBasedOnPrivileges(validatedScope, request);
        return validatedScope;
    }

    /**
     * Provided as an extension point to allow the OAuth2 provider to customize
     * the scope requested when a refresh token is requested. First calls
     * {@link OpenAMScopeValidator}'s implementation, then calls
     * {@link #addScopesBasedOnPrivileges(java.util.Set, org.forgerock.oauth2.core.OAuth2Request)}
     * to remove all the scopes representing a privilege that is not currently
     * assigned to the current user session.
     *
     * @param clientRegistration The client registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope from the access token.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws InvalidScopeException
     * @throws ServerException
     */
    @Override
    public Set<String> validateRefreshTokenScope(
            ClientRegistration clientRegistration,
            Set<String> requestedScope,
            Set<String> tokenScope,
            OAuth2Request request) throws ServerException, InvalidScopeException {
        DEBUG.message("validateRefreshTokenScope " + request);
        final Set<String> validatedScope = openAMScopeValidator.validateRefreshTokenScope(clientRegistration, requestedScope, tokenScope, request);
        addScopesBasedOnPrivileges(validatedScope, request);
        return validatedScope;
    }

    /**
     * Adds special scopes starting with {@code privileges://} based on the
     * current user's session. Privileges are calculated with
     * {@link ContactListPrivilegesEvaluator#evaluatePrivileges(org.forgerock.oauth2.core.OAuth2Request)}
     * and all the values are prefixed with {@link #PRIVILEGE_PREFIX} and these
     * values are added to the {@code scopes} set.
     *
     * @param scopes Modifiable set containing all the scopes. This set will be
     * extended by new elements representing the current user's privileges.
     * @param request Current OAuth2Request.
     * @throws InvalidScopeException
     * @throws ServerException
     */
    void addScopesBasedOnPrivileges(Set<String> scopes, OAuth2Request request)
            throws InvalidScopeException, ServerException {
        try {
            Set<String> privilegeSet
                    = ContactListPrivilegesEvaluator.evaluatePrivileges(request);

            privilegeSet.stream()
                    .map((privilege) -> PRIVILEGE_PREFIX + privilege)
                    .forEach((scope) -> scopes.add(scope));
        } catch (SSOException | EntitlementException ex) {
            DEBUG.error("Exception during determining privileges", ex);
        }

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
     *
     * In addition to the behavior of
     * {@link OpenAMScopeValidator#getUserInfo(ClientRegistration, AccessToken, OAuth2Request)}
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
        // At this point OpenAMScopeValidator's default behaviour is inadequate
        // about handling our special scopes prefixed with {@code PRIVILEGE_PREFIX}:
        // there are key-value pairs for these scopes, where key=scope name 
        // and value is null.
        //
        // Removing all the entries from the tokenInfo map where the key is a 
        // privilege name (starts with PRIVILEGE_PREFIX).
        privileges.stream()
                .forEach(privilege -> tokenInfo.remove(PRIVILEGE_PREFIX + privilege));
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
            DEBUG.message("additionalDataToReturnFromTokenEndpoint " + request.getBody() + ", token: " + token);
            openAMScopeValidator.additionalDataToReturnFromTokenEndpoint(token, request);
            DEBUG.message("additionalDataToReturnFromTokenEndpoint accessToken: " + token.toMap());
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
        DEBUG.message("additionalDataToReturnFromAuthorizeEndpoint " + request.getBody() + ", tokens: " + tokens);
        return openAMScopeValidator.additionalDataToReturnFromAuthorizeEndpoint(tokens, request);
    }

    @Override
    public Set<String> validateBackChannelAuthorizationScope(ClientRegistration cr, Set<String> set, OAuth2Request oar) throws InvalidScopeException, ServerException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
