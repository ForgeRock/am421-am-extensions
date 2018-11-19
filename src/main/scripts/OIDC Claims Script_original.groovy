/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014-2016 ForgeRock AS.
*/
import com.iplanet.sso.SSOException
import com.sun.identity.idm.IdRepoException
import org.forgerock.oauth2.core.UserInfoClaims
//TODO lab11: import static getContactListPrivilegesArray method in ContactListPrivilegesEvaluator as getContactListPrivileges

/*
* Defined variables:
* logger - always presents, the "OAuth2Provider" debug logger instance
* claims - always present, default server provided claims
* session - present if the request contains the session cookie, the user's session object
* identity - always present, the identity of the resource owner
* scopes - always present, the requested scopes
* requestedClaims - Map<String, Set<String>>
*                  always present, not empty if the request contains a claims parameter and server has enabled
*                  claims_parameter_supported, map of requested claims to possible values, otherwise empty,
*                  requested claims with no requested values will have a key but no value in the map. A key with
*                  a single value in its Set indicates this is the only value that should be returned.
* Required to return a Map of claims to be added to the id_token claims
*
* Expected return value structure:
* UserInfoClaims {
*    Map<String, Object> values; // The values of the claims for the user information
*    Map<String, List<String>> compositeScopes; // Mapping of scope name to a list of claim names.
* }
*/

// user session is not null when the consent screen is displayed, and null when the user info endpoint is used
boolean sessionPresent = session != null

def fromSet = { claim, attr ->
    if (attr != null && attr.size() == 1){
        attr.iterator().next()
    } else if (attr != null && attr.size() > 1){
        attr
    } else if (logger.warningEnabled()) {
        logger.warning("OpenAMScopeValidator.getUserInfo(): Got an empty result for claim=$claim");
    }
}

attributeRetriever = { attribute, claim, identity, requested ->
    if (requested == null || requested.isEmpty()) {
        fromSet(claim, identity.getAttribute(attribute))
    } else if (requested.size() == 1) {
        requested.iterator().next()
    } else {
        throw new RuntimeException("No selection logic for $claim defined. Values: $requested")
    }
}

// [ {claim}: {attribute retriever}, ... ]
claimAttributes = [
        "email": attributeRetriever.curry("mail"),
        "address": { claim, identity, requested -> [ "formatted" : attributeRetriever("postaladdress", claim, identity, requested) ] },
        "phone_number": attributeRetriever.curry("telephonenumber"),
        "given_name": attributeRetriever.curry("givenname"),
        "zoneinfo": attributeRetriever.curry("preferredtimezone"),
        "family_name": attributeRetriever.curry("sn"),
        "locale": attributeRetriever.curry("preferredlocale"),
        "name": attributeRetriever.curry("cn"),
        //This custom claim exposes the selectedRole session property
        "selectedRole" : { claim, identity, requested -> 
          if (sessionPresent) {
            session.getProperty("selectedRole")
          } else {
            "N/A"
          }
        },
        //This custom claim exposes the contactlist related privileges
        "contactlist-privileges" : { claim, identity, requested -> 
            logger.message("Claim: " + claim + ", requested: " + requested);
            if (requested != null) {
                //The values defined for the contactlist-privileges claim in the claims parameter 
                //is received in the requested variable
                //The ContactListTokenResponseTypeHandler injects the calculated privileges
                //into the claims parameter before persisting it in the access token.
                requested 
            } else if (sessionPresent) {
                //TODO lab11: call the imported getContactListPrivileges method with the tokenID string of the current session instead returning with this empty array
                //TODO lab11: Hint: you can get the string version of the token id just like this: session.getTokenID().toString()
                []
            } else {
                []
            }
        }
]

// {scope}: [ {claim}, ... ]
scopeClaimsMap = [
        "email": [ "email" ],
        "address": [ "address" ],
        "phone": [ "phone_number" ],
        "profile": [ "given_name", "zoneinfo", "family_name", "locale", "name" ],
        "selectedRole" : [ "selectedRole" ]
        //TODO lab11: Add the mapping: "contactlist-privileges" scope has the claim named "contactlist-privileges"
        //TODO lab11: Optionally also add the "selectedRole" claim to the "contactlist-privileges" scope
]

if (logger.messageEnabled()) {
    scopes.findAll { s -> !("openid".equals(s) || scopeClaimsMap.containsKey(s)) }.each { s ->
        logger.message("OpenAMScopeValidator.getUserInfo()::Message: scope not bound to claims: $s")
    }
}

def computeClaim = { claim, requestedValues ->
    try {
        [ claim, claimAttributes.get(claim)(claim, identity, requestedValues) ]
    } catch (IdRepoException e) {
        if (logger.warningEnabled()) {
            logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=$attribute", e);
        }
    } catch (SSOException e) {
        if (logger.warningEnabled()) {
            logger.warning("OpenAMScopeValidator.getUserInfo(): Unable to retrieve attribute=$attribute", e);
        }
    }
}

def computedClaims = scopes.findAll { s -> !"openid".equals(s) && scopeClaimsMap.containsKey(s) }.inject(claims) { map, s ->
    scopeClaims = scopeClaimsMap.get(s)
    map << scopeClaims.findAll { c -> !requestedClaims.containsKey(c) }.collectEntries([:]) { claim -> computeClaim(claim, null) }
}.findAll { map -> map.value != null } << requestedClaims.collectEntries([:]) { claim, requestedValue ->
    computeClaim(claim, requestedValue)
}

def compositeScopes = scopeClaimsMap.findAll { scope ->
    scopes.contains(scope.key)
}

return new UserInfoClaims((Map)computedClaims, (Map)compositeScopes)
