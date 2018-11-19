var epoch = new Date().getTime() / 1000;
console.log("Current epoch is " + epoch);
// Use the following two lines for custom authentication node to receive the value
document.getElementById("output").value = epoch;
document.getElementById("loginButton_0").click();
// Use the following line for a standard scripted authentication module return value
// output.value = epoch;