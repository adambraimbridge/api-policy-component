package com.ft.up.apipolicy.filters;

import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class SuppressRichContentMarkupFilter implements ApiFilter {

    public static final String BODY_XML_KEY = "bodyXML";
	public static final URIImpl BODY_PREDICATE = new URIImpl("http://www.ft.com/ontology/content/body");
	private final JsonConverter jsonConverter;
    private BodyProcessingFieldTransformer transformer;

    public SuppressRichContentMarkupFilter(JsonConverter jsonConverter, BodyProcessingFieldTransformer transformer) {
        this.jsonConverter = jsonConverter;
        this.transformer = transformer;
    }

    @Override
    public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

        MutableResponse response = chain.callNextFilter(request);

		if(request.policyIs("INCLUDE_RICH_CONTENT")) {
			return response;
		}

		String body = null;
		if(response.getContentType().startsWith("application/ld+json")) {
			Model content = null;
			try {
				content = Rio.parse(response.getEntityAsStream(), "", RDFFormat.JSONLD);
			} catch (IOException | RDFParseException e) {
				throw new BodyProcessingException(e);
			}


			Statement bodyStatement = content.filter(null, BODY_PREDICATE,null).iterator().next();

			body = bodyStatement.getObject().stringValue();

			if(body == null) {
				return response;
			}

			body = transformer.transform(body, "TODO");
			//TODO add transactionID

			content.remove(bodyStatement);
			content.add(bodyStatement.getSubject(),BODY_PREDICATE, ValueFactoryImpl.getInstance().createLiteral(body));

			ByteArrayOutputStream entityBytes = new ByteArrayOutputStream();
			try {
				Rio.write(content,entityBytes,RDFFormat.JSONLD);
			} catch (RDFHandlerException e) {
				throw new BodyProcessingException(e);
			}

			response.setEntity(entityBytes.toByteArray());

		} else {
			HashMap<String, Object> content = jsonConverter.readEntity(response);
			body = ((String)content.get(BODY_XML_KEY));

			if(body == null) {
				return response;
			}

			body = transformer.transform(body, "TODO");
			//TODO add transactionID

			content.put(BODY_XML_KEY, body);

			jsonConverter.replaceEntity(response, content);
		}


        return response;
    }
}
