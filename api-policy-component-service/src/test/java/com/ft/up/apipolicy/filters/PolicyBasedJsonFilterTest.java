package com.ft.up.apipolicy.filters;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PolicyBasedJsonFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  
  private MutableRequest request = mock(MutableRequest.class);
  private HttpPipelineChain chain = mock(HttpPipelineChain.class);
  private MutableResponse response = new MutableResponse();
  
  @Test
  public void thatPathMappedToNullPolicyIsWhitelisted() throws Exception {
    Map<String,Object> object = Collections.singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo", null));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }
  
  @Test
  public void thatPathMappedToPresentPolicyIsPreserved() throws Exception {
    Map<String,Object> object = Collections.singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }
  
  @Test
  public void thatPathMappedToAbsentPolicyIsRemoved() throws Exception {
    Map<String,Object> object = new HashMap<>();
    object.put("foo", "bar");
    object.put("fish", "wibble");
    
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(chain.callNextFilter(request)).thenReturn(response);
    
    Map<String,Policy> policy = new HashMap<>();
    policy.put("$.foo", null);
    policy.put("$.fish", Policy.INTERNAL_UNSTABLE);
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(policy);
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    Map<String,Object> expected = Collections.singletonMap("foo", "bar");
    assertThat(actual, equalTo(expected));
  }
  
  @Test
  public void thatNoAllowedPathsReturnsEmptyMap() throws Exception {
    Map<String,Object> object = Collections.singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(0));
  }
  
  @Test
  public void thatNestedPathsAreTraversed() throws Exception {
    Map<String,Object> inner = Collections.singletonMap("bar", "baz");
    Map<String,Object> object = Collections.singletonMap("foo", inner);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo.bar", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }
  
  @Test
  public void thatPathDoesNotDescendIntoNestedObject() throws Exception {
    Map<String,Object> inner = Collections.singletonMap("bar", "baz");
    Map<String,Object> object = Collections.singletonMap("foo", inner);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));
    
    Map<String,Object> foo = (Map)actual.get("foo");
    assertThat(foo.size(), equalTo(0));
  }
  
  @Test
  public void thatWildcardPathMappedToPresentPolicyIsPreserved() throws Exception {
    Map<String,Object> inner = new HashMap<>();
    inner.put("red", "elephant");
    inner.put("blue", "mouse");
    Map<String,Object> middle = Collections.singletonMap("bar", inner);
    Map<String,Object> object = Collections.singletonMap("foo", middle);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo.*.red", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    Map<String,Object> expected = Collections.singletonMap("foo",
        Collections.singletonMap("bar", Collections.singletonMap("red", "elephant")));
    assertThat(actual, equalTo(expected));
  }
  
  @Test
  public void thatWildcardTerminalPreservesDeepObjects() throws Exception {
    Map<String,Object> inner = Collections.singletonMap("baz", "elephant");
    Map<String,Object> middle = Collections.singletonMap("bar", inner);
    Map<String,Object> object = Collections.singletonMap("foo", middle);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo.*", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }
  
  @Test
  public void thatArraysAreTraversed() throws Exception {
    Map<String,Object> first = Collections.singletonMap("bar", "baz");
    Map<String,Object> second = Collections.singletonMap("bar", "wibble");
    List<Object> list = Arrays.asList(first, second);
    Map<String,Object> object = Collections.singletonMap("foo", list);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo[1].bar", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));
    List<Object> foo = (List)actual.get("foo");
    assertThat(foo.size(), equalTo(2));
    
    Map<String,Object> actualFirst = (Map)foo.get(0);
    assertThat(actualFirst.size(), equalTo(0));
    Map<String,Object> actualSecond = (Map)foo.get(1);
    assertThat(actualSecond, equalTo(second));
  }
  
  @Test
  public void thatWildcardIndexesArePreserved() throws Exception {
    Map<String,Object> first = new HashMap<>();
    first.put("bar", "baz");
    first.put("fish", "wibble");
    
    Map<String,Object> second = new HashMap<>();
    second.put("bar", "red");
    second.put("fish", "blue");
    
    List<Object> list = Arrays.asList(first, second);
    Map<String,Object> object = Collections.singletonMap("foo", list);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    
    when(request.getPolicies()).thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);
    
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(Collections.singletonMap("$.foo[*].bar", Policy.INTERNAL_UNSTABLE));
    
    MutableResponse actualResponse = f.processRequest(request, chain);
    
    Map<String,Object> actual = MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));
    List<Object> foo = (List)actual.get("foo");
    assertThat(foo.size(), equalTo(2));
    
    Map<String,Object> actualFirst = (Map)foo.get(0);
    Map<String,Object> expectedFirst = Collections.singletonMap("bar", "baz");
    assertThat(actualFirst, equalTo(expectedFirst));
    
    Map<String,Object> actualSecond = (Map)foo.get(1);
    Map<String,Object> expectedSecond = Collections.singletonMap("bar", "red");
    assertThat(actualSecond, equalTo(expectedSecond));
  }
}
