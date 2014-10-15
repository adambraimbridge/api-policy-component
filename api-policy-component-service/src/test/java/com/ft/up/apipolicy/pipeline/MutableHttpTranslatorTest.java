package com.ft.up.apipolicy.pipeline;

import com.google.common.base.Charsets;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * MutableHttpTranslatorTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class MutableHttpTranslatorTest {

    @Mock
    HttpServletRequest request;

    @Before
    public void setup() {

        Answer<java.util.Enumeration<java.lang.String> > answerWithPrefixedName = new Answer<java.util.Enumeration<java.lang.String> >() {
            @Override
            public java.util.Enumeration<java.lang.String>  answer(InvocationOnMock invocationOnMock) throws Throwable {
                Vector<String> strings = new Vector<>();

                strings.add("Value of " + invocationOnMock.getArguments()[0]);

                return strings.elements();
            }
        };

        when(request.getHeaders(anyString())).then(answerWithPrefixedName);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://example.com:89999/test"));

    }

    @Test
    public void shouldCopyRequestHeadersToMutableRequest() {

        Vector<String> headersPresent = new Vector<>();
        headersPresent.add("Foo");
        headersPresent.add("Bar");
        headersPresent.add("Baz");

        when(request.getHeaderNames()).thenReturn(headersPresent.elements());

        MutableHttpTranslator translator = new MutableHttpTranslator();

        MutableRequest result = translator.translateFrom(request);

        assertThat(result.getHeaders().keySet(), hasItems("Foo","Bar","Baz"));

        assertThat(result.getHeaders().getFirst("Foo"),is("Value of Foo"));
        assertThat(result.getHeaders().getFirst("Bar"),is("Value of Bar"));
        assertThat(result.getHeaders().getFirst("Baz"),is("Value of Baz"));


    }

    @Test
    public void shouldExcludeBlacklistedRequestHeadersFromMutableRequest() {
        Vector<String> headersPresent = new Vector<>();
        headersPresent.add("Foo");
        headersPresent.add("Bar");
        headersPresent.add("Host");

        when(request.getHeaderNames()).thenReturn(headersPresent.elements());

        MutableHttpTranslator translator = new MutableHttpTranslator();

        MutableRequest result = translator.translateFrom(request);

        assertThat(result.getHeaders().keySet(), hasItems("Foo","Bar"));
        assertThat(result.getHeaders().keySet(), not(hasItems("Host")));

        assertThat(result.getHeaders().getFirst("Foo"),is("Value of Foo"));
        assertThat(result.getHeaders().getFirst("Bar"),is("Value of Bar"));
        assertThat(result.getHeaders().getFirst("Host"), nullValue());
    }


    @Test
    public void shouldCopyResponseHeadersToMutableResponse() {

        MultivaluedMap<String,String> headersPresent = new MultivaluedMapImpl();
        headersPresent.putSingle("Foo","01");
        headersPresent.putSingle("Bar","02");
        headersPresent.putSingle("Baz","03");

        MutableResponse inputResponse = new MutableResponse(headersPresent,"hello".getBytes(Charsets.UTF_8));

        MutableHttpTranslator translator = new MutableHttpTranslator();

        Response response =  translator.translateTo(inputResponse);

        assertThat((String) response.getMetadata().getFirst("Foo"), is("01"));
        assertThat((String) response.getMetadata().getFirst("Bar"), is("02"));
        assertThat((String) response.getMetadata().getFirst("Baz"), is("03"));

    }

    @Test
    public void shouldExcludeBlacklistedResponseHeadersFromMutableResponse() {
        MultivaluedMap<String,String> headersPresent = new MultivaluedMapImpl();
        headersPresent.putSingle("Foo","01");
        headersPresent.putSingle("Bar","02");
        headersPresent.putSingle("Host","varnishnode01.ft.com");

        MutableResponse inputResponse = new MutableResponse(headersPresent,"hello".getBytes(Charsets.UTF_8));


        MutableHttpTranslator translator = new MutableHttpTranslator();

        Response response = translator.translateTo(inputResponse);

        assertThat((String)response.getMetadata().getFirst("Foo"),is("01"));
        assertThat((String) response.getMetadata().getFirst("Bar"), is("02"));

        assertThat(response.getMetadata().containsKey("Host"),is(false));

    }


    @Test
    public void shouldRecordPathElementOfOriginalURI() {
        MutableHttpTranslator translator = new MutableHttpTranslator();

        MutableRequest result = translator.translateFrom(request);


        assertThat(result.getAbsolutePath(),is("/test"));
    }



}
