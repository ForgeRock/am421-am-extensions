package com.forgerock.edu.postauth;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Post Authentication Processor implementation that logs login success,
 * login error and logout events to the DEBUG log.
 *
 * @author vrg
 */
public class SelectedRoleLogger implements AMPostAuthProcessInterface {

    private static final Debug DEBUG = Debug.getInstance("SelectedRoleLogger");

    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
        String gotoParam = (String) requestParamsMap.get(ISAuthConstants.GOTO_PARAM);
        DEBUG.message("LOGIN SUCCESS, orig goto=" +  gotoParam);
        gotoParam = "http://other.com/?goto=" + URLEncoder.encode(gotoParam, StandardCharsets.ISO_8859_1);
        DEBUG.message("LOGIN SUCCESS, request.goto = " + gotoParam);
        requestParamsMap.put(ISAuthConstants.GOTO_PARAM, gotoParam);

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
            String gotoParam = request.getParameter(ISAuthConstants.GOTO_PARAM);
            DEBUG.message("LOGOUT, ssoToken." + POST_PROCESS_LOGOUT_URL + " = " + postProcessLogoutURL);
            DEBUG.message("LOGOUT SUCCESS, request.goto = " + gotoParam);
        } catch (SSOException ex) {
            DEBUG.error("LOGOUT SUCCESS, SSOException during reading token param", ex);
        }
    }

}
