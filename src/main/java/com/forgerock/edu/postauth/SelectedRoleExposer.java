package com.forgerock.edu.postauth;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Post Authentication Processor implementation that logs login success,
 * login error and logout events to the DEBUG log.
 *
 * @author vrg
 */
public class SelectedRoleExposer implements AMPostAuthProcessInterface {

    private static Debug DEBUG = Debug.getInstance("SelectedRoleExposer");

    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
        try {
            String selectedRole = (String) requestParamsMap.get("selectedRole");
            DEBUG.message("LOGIN SUCCESS, selectedRole is " + selectedRole);
            ssoToken.setProperty("selectedRole", selectedRole);
            
        } catch (SSOException ex) {
            DEBUG.error("SSOException during onloginSuccess", ex);
        }

    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response) {
        String gotoParam = request.getParameter(ISAuthConstants.GOTO_PARAM);
        DEBUG.message("LOGIN FAILED request.goto = " + gotoParam);
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
        try {
            String postProcessLogoutURL = ssoToken.getProperty(POST_PROCESS_LOGOUT_URL);
            DEBUG.message("LOGOUT, ssoToken." + POST_PROCESS_LOGOUT_URL + " = " + postProcessLogoutURL);
        } catch (SSOException ex) {
            DEBUG.error("LOGOUT SUCCESS, SSOException during reading token param", ex);
        }
    }

}
