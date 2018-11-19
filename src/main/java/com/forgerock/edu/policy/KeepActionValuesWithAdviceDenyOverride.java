package com.forgerock.edu.policy;

import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.Entitlement;

/**
 * This EntitlementCombiner keeps the behavior of DenyOverride class except
 * that it keeps action values even if there is advice in the entitlements to combine. 
 * Useful when protecting
 * REST interfaces. Different actions (GET,POST,PUT,DELETE) on the same resource
 * can be assigned to different conditions. DELETE might not be permitted, while GET
 * is permitted. DenyOverride would hide the allow decision for GET action, if DELETE
 * action results in an advice. This EntitlementCombiner retains the actionValues
 * even if there is advice in the combined entitlements.
 *
 * @author vrg
 */
public class KeepActionValuesWithAdviceDenyOverride extends DenyOverride {

    @Override
    protected void mergeActionValues(Entitlement e1, Entitlement e2) {
        super.mergeActionValues(new AdvicesHiderEntitlementWrapper(e1), new AdvicesHiderEntitlementWrapper(e2));
    }

}
