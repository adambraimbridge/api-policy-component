package com.ft.up.apipolicy.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.util.FluentLoggingWrapper;

import java.util.*;

import static com.ft.up.apipolicy.util.FluentLoggingWrapper.MESSAGE;
import static org.slf4j.MDC.get;

public class PolicyBrandsResolver {

    private FluentLoggingWrapper log;

    private Map<String, PolicyFilterParameter> policyFilterParameterMap;

    public PolicyBrandsResolver(@JsonProperty("policyFilterParameterList") List<PolicyFilterParameter> policyFilterParameterList) {

        policyFilterParameterMap = new HashMap<String, PolicyFilterParameter>();

        for(PolicyFilterParameter policyFilterParameter : policyFilterParameterList){
            policyFilterParameterMap.put(policyFilterParameter.getPolicy(), policyFilterParameter);
        }

        log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString());
    }

    public Map<String, PolicyFilterParameter> getPolicyFilterParameterMap() {
        return policyFilterParameterMap;
    }

    public void applyQueryParams(MutableRequest request) {

        Set<String> policySet = request.getPolicies();

        log.withMethodName("applyQueryParams")
                .withTransactionId(get("transaction_id"));

        Set<String> notKnownPolicies = new HashSet<String>();
        for (String policy : policySet) {
            PolicyFilterParameter policyFilterParameter = policyFilterParameterMap.get(policy);
            if (policyFilterParameter == null) {
                notKnownPolicies.add(policy);
                continue;
            }
            if (policyFilterParameter.getForBrand() != null) {
                List<String> forBrands = policyFilterParameter.getForBrand();
                for (String brand : forBrands) {
                    request.getQueryParameters().add("forBrand", brand);
                }
            }
            if (policyFilterParameter.getNotForBrand() != null) {
                List<String> notForBrands = policyFilterParameter.getNotForBrand();
                for (String brand : notForBrands) {
                    request.getQueryParameters().add("notForBrand", brand);
                }
            }
        }
        if (!notKnownPolicies.isEmpty()) {
            log.withField(MESSAGE, "A policy in the request did not map to a known policy in the system : "
                    + notKnownPolicies.toString()).build().logWarn();
        }
    }
}
