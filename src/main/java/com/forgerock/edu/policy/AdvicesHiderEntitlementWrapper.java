package com.forgerock.edu.policy;

import com.google.inject.Inject;
import com.sun.identity.entitlement.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import com.sun.identity.entitlement.interfaces.ResourceName;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Entitlement wrapper which delegates all methods to the original entitlement
 * except {@link #hasAdvice()} method which always returns {@code false}.
 *
 * @author vrg
 */
public class AdvicesHiderEntitlementWrapper implements Entitlement {

    @Inject
    private final Entitlement entitlement;


    public AdvicesHiderEntitlementWrapper(Entitlement entitlement) {
        this.entitlement = entitlement;
    }

    @Override
    public void setName(String name) {
        entitlement.setName(name);
    }

    @Override
    public String getName() {
        return entitlement.getName();
    }

    @Override
    public void setResourceNames(Set<String> resourceNames) {
        entitlement.setResourceNames(resourceNames);
    }

    @Override
    public Set<String> getResourceNames() {
        return entitlement.getResourceNames();
    }

    @Override
    public void setResourceName(String resourceName) {
        entitlement.setResourceName(resourceName);
    }

    @Override
    public String getResourceName() {
        return entitlement.getResourceName();
    }

    @Override
    public void setRequestedResourceNames(Set<String> requestedResourceNames) {
        entitlement.setRequestedResourceNames(requestedResourceNames);
    }

    @Override
    public Set<String> getRequestedResourceNames() {
        return entitlement.getRequestedResourceNames();
    }

    @Override
    public void setRequestedResourceName(String requestedResourceName) {
        entitlement.setRequestedResourceName(requestedResourceName);
    }

    @Override
    public String getRequestedResourceName() {
        return entitlement.getRequestedResourceName();
    }

    @Override
    public String getApplicationName() {
        return entitlement.getApplicationName();
    }

    @Override
    public void setApplicationName(String applicationName) {
        entitlement.setApplicationName(applicationName);
    }

    @Override
    public void setActionName(String actionName) {
        entitlement.setActionName(actionName);
    }

    @Override
    public void setActionNames(Set<String> actionNames) {
        entitlement.setActionNames(actionNames);
    }

    @Override
    public void setActionValues(Map<String, Boolean> actionValues) {
        entitlement.setActionValues(actionValues);
    }

    @Override
    public Boolean getActionValue(String name) {
        return entitlement.getActionValue(name);
    }

    @Override
    public Map<String, Boolean> getActionValues() {
        return entitlement.getActionValues();
    }

    @Override
    public Set<Object> getActionValues(String name) {
        return entitlement.getActionValues(name);
    }

    /**
     * This wrapper does not delegate this method call to the wrapped entitlement.
     * @param advices 
     */
    @Override
    public void setAdvices(Map<String, Set<String>> advices) {
    }

    /**
     * Pretends that there is no advice in the entitlement.
     * 
     * @return 
     */
    @Override
    public Map<String, Set<String>> getAdvices() {
        return Collections.EMPTY_MAP;
    }

    /**
     * Pretends that there is no advice in the entitlement.
     * 
     * @return false
     */
    @Override
    public boolean hasAdvice() {
        return false;
    }

    @Override
    public void setAttributes(Map<String, Set<String>> attributes) {
        entitlement.setAttributes(attributes);
    }

    @Override
    public Map<String, Set<String>> getAttributes() {
        return entitlement.getAttributes();
    }

    @Override
    public void setTTL(long ttl) {
        entitlement.setTTL(ttl);
    }

    @Override
    public long getTTL() {
        return entitlement.getTTL();
    }

    @Override
    public Set<String> evaluate(Subject adminSubject, String realm, Subject subject, String applicationName, String resourceName, Set<String> actionNames, Map<String, Set<String>> environment, boolean recursive) throws EntitlementException {
        return entitlement.evaluate(adminSubject, realm, subject, applicationName, resourceName, actionNames, environment, recursive);
    }

    @Override
    public String toString() {
        return entitlement.toString();
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return entitlement.toJSONObject();
    }

    @Override
    public boolean equals(Object obj) {
        return entitlement.equals(obj);
    }

    @Override
    public int hashCode() {
        return entitlement.hashCode();
    }

    @Override
    public ResourceSearchIndexes getResourceSearchIndexes(Subject adminSubject, String realm) throws EntitlementException {
        return entitlement.getResourceSearchIndexes(adminSubject, realm);
    }

    @Override
    public ResourceSaveIndexes getResourceSaveIndexes(Subject adminSubject, String realm) throws EntitlementException {
        return entitlement.getResourceSaveIndexes(adminSubject, realm);
    }

    @Override
    public String getIndexDescriptor(Subject subject, String s) throws EntitlementException {
        return entitlement.getIndexDescriptor(subject, s);
    }

    @Override
    public void clearCache() {
        entitlement.clearCache();
    }

    @Override
    public ResourceName getResourceComparator(Subject subject, String s) throws EntitlementException {
        return null;
    }

    @Override
    public Application getApplication(Subject adminSubject, String realm) throws EntitlementException {
        return entitlement.getApplication(adminSubject, realm);
    }

    @Override
    public void canonicalizeResources(Subject adminSubject, String realm) throws EntitlementException {
        entitlement.canonicalizeResources(adminSubject, realm);
    }
    
    
}
