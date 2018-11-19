var serverStatusURI = "http://app.test:8080/status.json";

/**
 * Helper function which logs the HTTP response sent in as a parameter.
 */
function logResponse(response) {
    logger.message("HTTP response received. Status: " + response.getStatusCode() + ", Body: " + response.getEntity());
}

/**
 * Gets the app.test server status. Returns with a parsed JSON structure
 * that is returned from http://app.test:8080/status.json
 *
 * @returns Server status JSON structure.
 */
function getServerStatus() {
    logger.message("Checking server status by sending a request to " + serverStatusURI);
  
    response = httpClient.get(serverStatusURI, {cookies: [], headers: []});
  
    logResponse(response);

    var result = JSON.parse(response.getEntity());
  
    return result;
}

var serverStatus = getServerStatus();

if (serverStatus.maintenance) {
    authorized = true;
    responseAttributes.put("maintenanceMessage",[serverStatus.message]);
    responseAttributes.put("maintenanceMode",["true"]);
}