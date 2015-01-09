package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.pipeline.MutableRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PolicyBrandsResolverTest {

    public static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String ALPHAVILLE_BRAND = "http://api.ft.com/things/89d15f70-640d-11e4-9803-0800200c9a66";
    public static final String BEYONDBRICS_BRAND = "http://api.ft.com/things/3a37a89e-14ce-4ac8-af12-961a9630dce3";

    public static final String FASTFT_CONTENT_ONLY = "FASTFT_CONTENT_ONLY";
    public static final String EXCLUDE_FASTFT_CONTENT = "EXCLUDE_FASTFT_CONTENT";
    public static final String BLOG_CONTENT_ONLY = "BLOG_CONTENT_ONLY";
    public static final String EXCLUDE_BLOG_CONTENT = "EXCLUDE_BLOG_CONTENT";

    public static final String SCOTTS_POLICY = "SCOTTS_POLICY";

    private List<PolicyFilterParameter> policyFilterParameterList;
    private PolicyBrandsResolver policyBrandsResolver;

    private Set<String> policySet;

    @Mock
    private MutableRequest mutableRequest;

    @Mock
    private MultivaluedMap<String, String> queryParameters;

    private PolicyFilterParameter onlyFastFT;
    private PolicyFilterParameter excludeFastFT;
    private PolicyFilterParameter onlyBlogs;
    private PolicyFilterParameter excludeBlogs;

    private List<String> fastFTBrandList = Arrays.asList(FASTFT_BRAND);
    private List<String> blogBrandList = Arrays.asList(ALPHAVILLE_BRAND, BEYONDBRICS_BRAND);

    @Before
    public void setUp() {
        policyFilterParameterList = new ArrayList<>();

        onlyFastFT = new PolicyFilterParameter(FASTFT_CONTENT_ONLY, fastFTBrandList, null);
        excludeFastFT = new PolicyFilterParameter(EXCLUDE_FASTFT_CONTENT, null, fastFTBrandList);
        onlyBlogs = new PolicyFilterParameter(BLOG_CONTENT_ONLY, blogBrandList, null);
        excludeBlogs = new PolicyFilterParameter(EXCLUDE_BLOG_CONTENT, null, blogBrandList);

        policyFilterParameterList.add(0, onlyFastFT);
        policyFilterParameterList.add(1, excludeFastFT);
        policyFilterParameterList.add(2, onlyBlogs);
        policyFilterParameterList.add(3, excludeBlogs);

        policyBrandsResolver = new PolicyBrandsResolver(policyFilterParameterList);
    }

    @Test
    public void noParametersShouldBeAddedWhenNull(){
        policySet = new HashSet<>();
        policySet.add(SCOTTS_POLICY);
        when(mutableRequest.getPolicies()).thenReturn(policySet);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(mutableRequest, never()).getQueryParameters();
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsFastFtContentOnly(){
        policySet = new HashSet<>();
        policySet.add(FASTFT_CONTENT_ONLY);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);
        commonExpectationsAndVerifications(keyArgument, valueArgument);
        verify(mutableRequest, times(1)).getQueryParameters();
        assertEquals("forBrand was not passed in to add method", "forBrand", keyArgument.getValue());
        assertEquals("FASTFT brand was not passed in to add method", FASTFT_BRAND, valueArgument.getValue());
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsExcludeFastFtContent(){
        policySet = new HashSet<>();
        policySet.add(EXCLUDE_FASTFT_CONTENT);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);
        commonExpectationsAndVerifications(keyArgument, valueArgument);
        verify(mutableRequest, times(1)).getQueryParameters();
        assertEquals("notForBrand was not passed in to add method", "notForBrand", keyArgument.getValue());
        assertEquals("FASTFT brand was not passed in to add method", FASTFT_BRAND, valueArgument.getValue());
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsBlogsContentOnly(){
        policySet = new HashSet<>();
        policySet.add(BLOG_CONTENT_ONLY);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);

        when(mutableRequest.getPolicies()).thenReturn(policySet);
        when(mutableRequest.getQueryParameters()).thenReturn(queryParameters);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(mutableRequest, times(2)).getQueryParameters();

        List<String> keyArguments = keyArgument.getAllValues();
        List<String> valueArguments = valueArgument.getAllValues();
        verify(queryParameters,times(2)).add(keyArgument.capture(), valueArgument.capture());
        assertEquals("forBrand was not passed in to add method", "forBrand", keyArguments.get(0));
        assertEquals("forBrand was not passed in to add method", "forBrand", keyArguments.get(1));
        assertEquals("ALPHAVILLE_BRAND brand was not passed in to add method", ALPHAVILLE_BRAND, valueArguments.get(0));
        assertEquals("BEYONDBRICS_BRAND brand was not passed in to add method", BEYONDBRICS_BRAND, valueArguments.get(1));
    }

    @Test
    public void brandParametersShouldBeAddedWhenPolicyIsExcludeBlogsContent(){
        policySet = new HashSet<>();
        policySet.add(EXCLUDE_BLOG_CONTENT);
        ArgumentCaptor<String> keyArgument = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueArgument = ArgumentCaptor.forClass(String.class);


        when(mutableRequest.getPolicies()).thenReturn(policySet);
        when(mutableRequest.getQueryParameters()).thenReturn(queryParameters);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(mutableRequest, times(2)).getQueryParameters();

        List<String> keyArguments = keyArgument.getAllValues();
        List<String> valueArguments = valueArgument.getAllValues();
        verify(queryParameters,times(2)).add(keyArgument.capture(), valueArgument.capture());
        assertEquals("notForBrand was not passed in to add method", "notForBrand", keyArguments.get(0));
        assertEquals("notForBrand was not passed in to add method", "notForBrand", keyArguments.get(1));
        assertEquals("ALPHAVILLE_BRAND brand was not passed in to add method", ALPHAVILLE_BRAND, valueArguments.get(0));
        assertEquals("BEYONDBRICS_BRAND brand was not passed in to add method", BEYONDBRICS_BRAND, valueArguments.get(1));
    }

    public void commonExpectationsAndVerifications(ArgumentCaptor<String> keyArgument, ArgumentCaptor<String> valueArgument) {
        when(mutableRequest.getPolicies()).thenReturn(policySet);
        when(mutableRequest.getQueryParameters()).thenReturn(queryParameters);
        policyBrandsResolver.applyQueryParams(mutableRequest);
        verify(queryParameters).add(keyArgument.capture(), valueArgument.capture());
    }
}
