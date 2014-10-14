package com.ft.up.apipolicy.resources;

import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.google.common.base.Preconditions;

public class KnownEndpoint implements Comparable<KnownEndpoint> {

	private final String uriRegex;
	private final HttpPipeline httpPipeline;

	public KnownEndpoint(String uriRegex, HttpPipeline httpPipeline) {
		Preconditions.checkNotNull(uriRegex, "uriRegex must not me null");
		Preconditions.checkNotNull(httpPipeline, "httpPipeline must not me null");

		this.uriRegex = uriRegex;
		this.httpPipeline = httpPipeline;
	}

	public String getUriRegex() {
		return uriRegex;
	}

	public HttpPipeline getPipeline() {
		return httpPipeline;
	}

	@Override
	public int compareTo(KnownEndpoint knownEndpoint) {
		return knownEndpoint.getUriRegex().compareTo(uriRegex);
	}
}
