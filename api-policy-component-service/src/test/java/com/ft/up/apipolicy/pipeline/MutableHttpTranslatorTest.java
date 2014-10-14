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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Mock
    HttpServletResponse response;

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

        try {
            when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // not written anywhere, instance acts as a byte sink.
                }
            });
        } catch (IOException e) {
            // should not occur during mock set up
        }
    }

    @Test
    public void shouldCopyRequestHeadersToMutableRequest() {

        Vector<String> headersPresent = new Vector<>();
        headersPresent.add("Foo");
        headersPresent.add("Bar");
        headersPresent.add("Baz");

        when(request.getHeaderNames()).thenReturn(headersPresent.elements());

        MutableHttpToServletsHttpTranslator translator = new MutableHttpToServletsHttpTranslator();

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

        MutableHttpToServletsHttpTranslator translator = new MutableHttpToServletsHttpTranslator();

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

        MutableHttpToServletsHttpTranslator translator = new MutableHttpToServletsHttpTranslator();

        translator.writeMutableResponseIntoActualResponse(inputResponse,response);

        verify(response).addHeader("Foo","01");
        verify(response).addHeader("Bar","02");
        verify(response).addHeader("Baz","03");

    }

    @Test
    public void shouldExcludeBlacklistedResponseHeadersFromMutableResponse() {
        MultivaluedMap<String,String> headersPresent = new MultivaluedMapImpl();
        headersPresent.putSingle("Foo","01");
        headersPresent.putSingle("Bar","02");
        headersPresent.putSingle("Host","varnishnode01.ft.com");

        MutableResponse inputResponse = new MutableResponse(headersPresent,"hello".getBytes(Charsets.UTF_8));


        MutableHttpToServletsHttpTranslator translator = new MutableHttpToServletsHttpTranslator();

        translator.writeMutableResponseIntoActualResponse(inputResponse,response);

        verify(response).addHeader("Foo","01");
        verify(response).addHeader("Bar","02");
        verify(response,never()).addHeader(eq("Host"),anyString());
    }



}
