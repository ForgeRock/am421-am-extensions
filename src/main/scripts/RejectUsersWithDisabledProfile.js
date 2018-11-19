/**
 * Helper function which logs the HTTP response sent in as a parameter.
 */
/*
  - Data made available by nodes that have already executed are available in the sharedState variable.
  - The script should set outcome to either "true" or "false".
 */
function logResponse(response) {
    // Updated for newer APIs
    logger.message("HTTP response received. Status: " + response.getStatus().getCode() + ", Body: " + response.getEntity());
}

// Added the line to get the username from the tree shared state
var username = sharedState.get("username");
var url = 'http://app.test:8080/contactlist/rest/isProfileDisabled/' + encodeURIComponent(username);

logger.message('Sending a GET request to ' + url);

/*
var response = httpClient.get(url, {
        cookies: [],
        headers: []
    });
*/
// Replace the above httpClient code with newer AM 6.0/IDM 6.0 http API
var request = new org.forgerock.http.protocol.Request();

request.setUri(url);
request.setMethod("GET");

var response = httpClient.send(request).get();
logResponse(response);

var disabled = JSON.parse(response.getEntity());

logger.message("User's profile is " + (disabled ? "disabled, failing..." : "active: login success"));

// Use the next line if this is a server-side authentication MODULE
// authState = disabled ? FAILED : SUCCESS;
// Use the following line if this is a server-side scripted decision authentication NODE script
outcome = (disabled ? "true" : "false");