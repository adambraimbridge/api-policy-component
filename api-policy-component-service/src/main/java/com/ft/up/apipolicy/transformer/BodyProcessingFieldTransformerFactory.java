package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.html.RemoveEmptyElementsBodyProcessor;
import com.ft.bodyprocessing.transformer.FieldTransformer;
import com.ft.bodyprocessing.transformer.FieldTransformerFactory;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;

import java.util.List;

import static java.util.Arrays.asList;

public class BodyProcessingFieldTransformerFactory implements FieldTransformerFactory {

    public BodyProcessingFieldTransformerFactory() {

    }

    @Override
    public FieldTransformer newInstance() {
        BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
        return new BodyProcessingFieldTransformer(bodyProcessorChain);
    }

    private List<BodyProcessor> bodyProcessors() {
        return asList(
                stAXTransformingBodyProcessor(),
                // video and slideshow rich content is returned as empty <a> tags and so gets removed
                new RemoveEmptyElementsBodyProcessor(asList("a"), asList("img"))
        );
    }

    private BodyProcessor stAXTransformingBodyProcessor() {
        return new StAXTransformingBodyProcessor(new BodyTransformationXMLEventRegistry());
    }

}
