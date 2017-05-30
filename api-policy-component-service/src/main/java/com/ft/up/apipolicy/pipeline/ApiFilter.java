package com.ft.up.apipolicy.pipeline;

/**
 * ApiFilter
 *
 * @author Simon.Gibbs
 */
public interface ApiFilter {

    String MAIM_IMAGE = "mainImage";
    String EMBEDS = "embeds";
    String ALTERNATIVE_IMAGES = "alternativeImages";
    String PROMOTIONAL_IMAGE = "promotionalImage";
    String MEMBERS = "members";
	String LEAD_IMAGES = "leadImages";

    MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain);

}
