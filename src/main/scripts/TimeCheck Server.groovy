import java.util.Random

// Added the next line to use this code in a Scripted Decision Node of a tree
// the value in the get parameter can be changed to match the client side hidden value ID.
def clientScriptOutputData = sharedState.get("output")
def clientEpoch = Double.parseDouble(clientScriptOutputData)
def epoch = System.currentTimeMillis() / 1000
def range = 2 + new Random().nextFloat()
logger.message("[TimeCheck Server] Received clientScriptOutputData: " + clientScriptOutputData)
logger.message("[TimeCheck Server] Epochs are " + epoch + " and " + clientEpoch + ", range " + range + " difference: " + Math.abs(epoch - clientEpoch))

// Added the code for use as a server-side authentication node script
if (Math.abs(epoch - clientEpoch) < range) {
  outcome = "true"
} else {
  outcome = "false"
}
/*
 * Use the following code if this is a server-side authentication module
if (Math.abs(epoch - clientEpoch) < range) {
  authState = SUCCESS
} else {
  authState = FAILED
}
*/