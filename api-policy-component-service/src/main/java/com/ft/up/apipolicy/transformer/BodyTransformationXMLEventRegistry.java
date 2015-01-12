package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class BodyTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

    public BodyTransformationXMLEventRegistry() {

        //default is to skip events but leave content - anything not configured below will be handled via this
        registerDefaultEventHandler(new RetainXMLEventHandler());
        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());
        // want to be sure to keep the wrapping node
        registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), "pull-quote");

    }
}