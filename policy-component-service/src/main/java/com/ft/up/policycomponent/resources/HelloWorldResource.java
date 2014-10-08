package com.ft.up.policycomponent.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class HelloWorldResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";

    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML + CHARSET_UTF_8)
    @Path("index.html")
    public final String getView() {

        return "Hello world";

    }
}
