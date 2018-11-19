package com.forgerock.edu.util;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.oauth2.core.OAuth2Request;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 *
 * @author vrg
 */
public class OAuth2Util {
    private static final Debug DEBUG = Debug.getInstance("OAuth2Util");

    public static SSOToken extractSSOToken(OAuth2Request request) {
        try {
            final HttpServletRequest httpServletRequest = ServletUtils.getRequest((Request) request.getRequest());
            final SSOTokenManager tokenManager = SSOTokenManager.getInstance();
            //extracting SSOToken from HttpServletRequest:
            return tokenManager.createSSOToken(httpServletRequest);
        } catch (SSOException ex) {
            DEBUG.error("Could not extract SSOToken from request", ex);
            return null;
        }
    }

}
