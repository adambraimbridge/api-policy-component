package com.ft.up.apipolicy.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;


@Path("/{path:.*}")
public class ReaderEndpointsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReaderEndpointsResource.class);

    @GET @Consumes(MediaType.WILDCARD) @Produces(MediaType.WILDCARD)
    public void service(@Context HttpServletRequest request, @Context HttpServletResponse response, @Context UriInfo uriInfo ) {
        LOGGER.info(uriInfo.getRequestUri().toString());
    }

}
