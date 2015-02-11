package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementByClassEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.up.apipolicy.transformer.eventhandlers.StripElementUnlessSpecificAttributesXmlEventHandler;

import java.util.Collections;

public class BodyTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

    public BodyTransformationXMLEventRegistry() {

        /* default is to keep events but leave content, including "body" tags - anything not configured below will be handled via this */
        registerDefaultEventHandler(new RetainXMLEventHandler());

        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "pull-quote");
		registerStartAndEndElementEventHandler(new StripElementByClassEventHandler("twitter-tweet", new RetainXMLEventHandler()), "blockquote");
		registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "timeline", "table", "big-number");

        registerStartAndEndElementEventHandler(new StripElementUnlessSpecificAttributesXmlEventHandler(
                Collections.singletonMap("data-asset-type","video"),
                new RetainXMLEventHandler()),
            "a"
        );

    }
}