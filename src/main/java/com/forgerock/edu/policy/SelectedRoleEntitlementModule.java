package com.forgerock.edu.policy;

import org.forgerock.openam.entitlement.EntitlementModule;
import org.forgerock.openam.entitlement.EntitlementRegistry;


public class SelectedRoleEntitlementModule implements EntitlementModule {
    @Override
    public void registerCustomTypes(EntitlementRegistry entitlementRegistry) {
        entitlementRegistry.registerConditionType("SelectedRoleCondition", SelectedRoleCondition.class);
        entitlementRegistry.registerDecisionCombiner("KeepActionValuesWithAdviceDenyOverride", KeepActionValuesWithAdviceDenyOverride.class);

    }
}
