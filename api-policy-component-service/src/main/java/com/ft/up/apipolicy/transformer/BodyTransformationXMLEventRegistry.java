package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.xml.eventhandlers.*;
import com.ft.up.apipolicy.transformer.xmlhandler.AttributeValue;
import com.ft.up.apipolicy.transformer.xmlhandler.StripIfSpecificAttributes;

import java.util.Arrays;
import java.util.Collections;

public class BodyTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

    private static final String IMAGE_SET_CLASS_URI = "http://www.ft.com/ontology/content/ImageSet";
    private static final String MEDIA_RESOURCE_CLASS_URI = "http://www.ft.com/ontology/content/MediaResource";
    private static final String FT_CONTENT = "ft-content";
    private static final String[] elementsToStrip = {"pull-quote", "promo-box", "ft-related", "timeline", "ft-timeline", "table", "big-number", "img"};

    public BodyTransformationXMLEventRegistry() {

        /* default is to keep events but leave content, including "body" tags - anything not configured below will be handled via this */
        registerDefaultEventHandler(new RetainXMLEventHandler());

        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), elementsToStrip);
		    registerStartAndEndElementEventHandler(new StripElementByClassEventHandler("twitter-tweet", new RetainXMLEventHandler()), "blockquote");
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
                Collections.singletonMap("type", MEDIA_RESOURCE_CLASS_URI), new RetainXMLEventHandler());
        final XMLEventHandler removeImageSet = new StripElementIfSpecificAttributesXmlEventHandler(
                Collections.singletonMap("type", IMAGE_SET_CLASS_URI), removeMediaResouce);
        registerStartAndEndElementEventHandler(removeImageSet, FT_CONTENT);
    }
}