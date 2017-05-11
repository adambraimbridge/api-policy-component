package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;

public class AddSyndication implements ApiFilter {

    private static final String CAN_BE_SYNDICATED_KEY = "canBeSyndicated";
    private static final String CAN_BE_SYNDICATED_VERIFY = "verify";

    private JsonConverter jsonConverter;
    private Policy policy;

    public AddSyndication(final JsonConverter jsonConverter, final Policy policy) {
        this.jsonConverter = jsonConverter;
        this.policy = policy;
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse originalResponse = chain.callNextFilter(request);
        if (!isEligibleForSyndicationField(originalResponse)) {
            return originalResponse;
        }
        final Map<String, Object> content = jsonConverter.readEntity(originalResponse);
        FieldModifier fieldModifier;
        if (request.policyIs(policy)) {
            fieldModifier = c -> {
                if (!c.containsKey(CAN_BE_SYNDICATED_KEY)) {
                    c.put(CAN_BE_SYNDICATED_KEY, CAN_BE_SYNDICATED_VERIFY);
                }
            };
        } else {
            fieldModifier = c -> {
                if (c.containsKey(CAN_BE_SYNDICATED_KEY)) {
                    c.remove(CAN_BE_SYNDICATED_KEY);
                }
            };
        }
        handleCanBeSyndicatedFiled(content, fieldModifier);
        handleCanBeSyndicatedFiledFromImages(content, fieldModifier);
        jsonConverter.replaceEntity(originalResponse, content);
        return originalResponse;
    }

    @SuppressWarnings("unchecked")
    private void handleCanBeSyndicatedFiled(Map content, FieldModifier fieldModifier) {
        fieldModifier.operation(content);
    }

    private void handleCanBeSyndicatedFiledFromImages(Map content, FieldModifier fieldModifier) {
        Object mainImage = content.get(MAIM_IMAGE);
        if (mainImage != null && mainImage instanceof Map) {
            Map mainImageAsMap = (Map) mainImage;
            if (mainImageAsMap.size() > 1) {
                handleCanBeSyndicatedFieldFromImageSets(mainImageAsMap, fieldModifier);
            }
        }

        Object embeddedImages = content.get(EMBEDS);
        if (embeddedImages != null && embeddedImages instanceof List) {
            List embeddedImagesAsList = (List) embeddedImages;
            for (Object embeddedImage : embeddedImagesAsList) {
                if (embeddedImage instanceof Map) {
                    Map embeddedImageAsMap = (Map) embeddedImage;
                    handleCanBeSyndicatedFieldFromImageSets(embeddedImageAsMap, fieldModifier);
                }
            }
        }

        Object alternativeImages = content.get(ALTERNATIVE_IMAGES);
        if (alternativeImages != null && alternativeImages instanceof Map) {
            Map alternativeImagesAsMap = (Map) alternativeImages;
            Object promotionalImage = alternativeImagesAsMap.get(PROMOTIONAL_IMAGE);
            if (promotionalImage != null && promotionalImage instanceof Map) {
                Map promotionalImageAsMap = (Map) promotionalImage;
                if (promotionalImageAsMap.size() > 1) {
                    handleCanBeSyndicatedFiled(promotionalImageAsMap, fieldModifier);
                }
            }
        }
    }

    private void handleCanBeSyndicatedFieldFromImageSets(Map mainImage, FieldModifier fieldModifier) {
        handleCanBeSyndicatedFiled(mainImage, fieldModifier);
        Object members = mainImage.get(MEMBERS);
        if (members != null && members instanceof List) {
            List membersAsList = (List) members;
            for (Object member : membersAsList) {
                if (member instanceof Map) {
                    Map memberAsMap = (Map) member;
                    handleCanBeSyndicatedFiled(memberAsMap, fieldModifier);
                }
            }
        }
    }

    private boolean isEligibleForSyndicationField(final MutableResponse response) {
        return OK.getStatusCode() == response.getStatus() && jsonConverter.isJson(response);
    }

    private interface FieldModifier {
        void operation(Map<String, Object> content);
    }

}
