package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.transformer.FieldTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.ft.up.apipolicy.EquivalentIgnoringWindowsLineEndings.equivalentToUnixString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BodyPostProcessingFieldTransformerFactoryTest {


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldTransformer bodyTransformer;
    private static final String TRANSACTION_ID = "tid_test";

    @Before
    public void setup() {
        bodyTransformer = new BodyPostProcessingFieldTransformerFactory().newInstance();
    }

    @Test
    public void shouldRemoveInlineRecommended(){
        String original = "<body><p>He sheltered there.</p>\n<p><recommended>\t\t\t\t\t\t\n" +
                "\th\t\n" +
                "\t<p>Intro</p>\t\t\t\t\t\n" +
                "\t<ul>\n" +
                "\t\t<li><a type='http://www.ft.com/ontology/content/Article' url='http://api.ft.com/content/e30ce78c-59fe-11e7-b553-e2df1b0c3220'>Internal articles’s title added by methode automatically</a></li>\n" +
                "\t\t<li><a href='http://ft.com/content/71ece778-5a53-11e7-9bc8-8055f264aa8b'>Manually added FT article’s manual title</a></li>\t\t\n" +
                "\t\t<li><a href='http://example.com/manually/added/document1.pdf'>External link’s manually added title</a></li>\n" +
                "\t</ul>\n" +
                "</recommended>\n</p>\n<p>\"I saw bodies everywhere.\"</p>\n\n\n\n</body>";
        String expected = "<body><p>He sheltered there.</p>\n<p>\"I saw bodies everywhere.\"</p>\n\n\n\n</body>" ;

        checkTransformation(original, expected);
    }

    private void checkTransformation(String originalBody, String expectedTransformedBody) {
        String actualTransformedBody = bodyTransformer.transform(originalBody, TRANSACTION_ID);
        assertThat(actualTransformedBody, is(equivalentToUnixString(expectedTransformedBody)));
    }

}
