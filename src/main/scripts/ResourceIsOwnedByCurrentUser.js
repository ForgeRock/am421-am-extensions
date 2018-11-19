/**
 * Extracts resource owner's identity from the resourceURI.
 * This function uses the pattern: 
 * rest://owned-groups/<resource owner id> or
 * rest://owned-groups/<resource owner id>/*
 *
 * @returns {String} Returns the resource owner's universalId
 *                   or null if the resourceURI does not match the pattern.
 */
function extractResourceOwnerUniversalId(resourceURI) {
    var regex = /^rest:\/\/owned-groups\/([^\/]+)($|\/.*$)/;
    logger.message("ResourceURI: " + resourceURI);
    var result = regex.exec(resourceURI);
    if (result) {
        var uid = result[1];
        logger.message("uid is: " + uid);
        var universalId = "id=" + uid + ",ou=user,dc=openam,dc=forgerock,dc=org";
        logger.message("Resource owner universalId is: " + universalId);
        
        return universalId;
    }
    return null;
}

authorized = (username == extractResourceOwnerUniversalId(resourceURI));

logger.message("Resource is owned by current user (" + username + ") uri: " + authorized);