package com.ft.up.apipolicy.transformer;

import java.util.Arrays;
import java.util.Collections;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementByClassEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementIfSpecificAttributesXmlEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.up.apipolicy.transformer.xmlhandler.AttributeValue;
import com.ft.up.apipolicy.transformer.xmlhandler.StripIfSpecificAttributes;

class BodyTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

    private static final String IMAGE_SET_CLASS_URI = "http://www.ft.com/ontology/content/ImageSet";
    private static final String MEDIA_RESOURCE_CLASS_URI = "http://www.ft.com/ontology/content/MediaResource";
    private static final String FT_CONTENT = "ft-content";

    BodyTransformationXMLEventRegistry() {

        /* default is to keep events but leave content, including "body" tags - anything not configured below will be handled via this */
        registerDefaultEventHandler(new RetainXMLEventHandler());

        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "pull-quote");
        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "promo-box", "ft-related");
		registerStartAndEndElementEventHandler(new StripElementByClassEventHandler("twitter-tweet", new RetainXMLEventHandler()), "blockquote");
		registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "timeline", "ft-timeline", "table", "big-number");
        registerStartAndEndElementEventHandler(
                new StripIfSpecificAttributes(
                        Arrays.asList(
                                new AttributeValue("data-asset-type", "video"),
                                new AttributeValue("data-asset-type", "interactive-graphic")
                        ),
                        new RetainXMLEventHandler()
                ),
                "a"
        );
        final XMLEventHandler removeMediaResouce = new StripElementIfSpecificAttributesXmlEventHandler(
                Collections.singletonMap("type", MEDIA_RESOURCE_CLASS_URI),
                new RetainXMLEventHandler());
        final XMLEventHandler removeImageSet = new StripElementIfSpecificAttributesXmlEventHandler(
                Collections.singletonMap("type", IMAGE_SET_CLASS_URI), removeMediaResouce);
        registerStartAndEndElementEventHandler(removeImageSet, FT_CONTENT);
        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "img");
    }
}