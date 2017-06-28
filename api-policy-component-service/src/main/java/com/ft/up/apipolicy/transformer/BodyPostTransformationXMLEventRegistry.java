package com.ft.up.apipolicy.transformer;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class BodyPostTransformationXMLEventRegistry extends XMLEventHandlerRegistry {

  private static final String[] elementsToStrip = {"recommended"};

  public BodyPostTransformationXMLEventRegistry() {
        /* default is to keep events but leave content, including "body" tags - anything not configured below will be handled via this */
    registerDefaultEventHandler(new RetainXMLEventHandler());

    registerCharactersEventHandler(new RetainXMLEventHandler());
    registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

    registerStartAndEndElementEventHandler(new StripElementAndContentsXMLEventHandler(), elementsToStrip);
  }
}