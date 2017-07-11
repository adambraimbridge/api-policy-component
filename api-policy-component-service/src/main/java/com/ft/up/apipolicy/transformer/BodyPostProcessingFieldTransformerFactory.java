package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.transformer.FieldTransformer;
import com.ft.bodyprocessing.transformer.FieldTransformerFactory;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;

import java.util.Collections;
import java.util.List;

public class BodyPostProcessingFieldTransformerFactory implements FieldTransformerFactory {

  public BodyPostProcessingFieldTransformerFactory() {

  }

  @Override
  public FieldTransformer newInstance() {
    BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
    return new BodyProcessingFieldTransformer(bodyProcessorChain);
  }

  private List<BodyProcessor> bodyProcessors() {
    return Collections.singletonList(stAXTransformingBodyProcessor());
  }

  private BodyProcessor stAXTransformingBodyProcessor() {
    return new StAXTransformingBodyProcessor(new BodyPostTransformationXMLEventRegistry());
  }

}
