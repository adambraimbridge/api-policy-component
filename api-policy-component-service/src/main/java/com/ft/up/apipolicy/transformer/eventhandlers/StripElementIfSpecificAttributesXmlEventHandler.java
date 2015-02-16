package com.ft.up.apipolicy.transformer.eventhandlers;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.base.Preconditions;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.Map;

/**
 * StripElementIfSpecificAttributesXmlEventHandler
 *
 * @author Simon.Gibbs
 */
public class StripElementIfSpecificAttributesXmlEventHandler extends BaseXMLEventHandler {

    private Map<String,String> targetedAttributes;
    private XMLEventHandler fallbackHandler;

    public StripElementIfSpecificAttributesXmlEventHandler(Map<String, String> targetedAttrs, XMLEventHandler fallbackHandler) {

        Preconditions.checkArgument(targetedAttrs!=null, "targeted attributes not specified");
        Preconditions.checkArgument(!targetedAttrs.isEmpty(), "targeted attributes not populated");

        this.targetedAttributes = targetedAttrs;
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter, BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        if(isTargetedElement(event)) {
            skipUntilMatchingEndTag(event.getName().getLocalPart(),xmlEventReader);
            return;
        }

        fallbackHandler.handleStartElementEvent(event,xmlEventReader,eventWriter,bodyProcessingContext);

    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        fallbackHandler.handleEndElementEvent(event,xmlEventReader,eventWriter);
    }

    private boolean isTargetedElement(StartElement event) {

        for(Map.Entry<String,String> attr : targetedAttributes.entrySet()) {

            Attribute actualAttr = event.getAttributeByName(QName.valueOf(attr.getKey()));
            if(actualAttr==null) {
                return false;
            }

            if(!attr.getValue().equals(actualAttr.getValue())) {
                return false;
            }

        }

        return true;

    }
}
