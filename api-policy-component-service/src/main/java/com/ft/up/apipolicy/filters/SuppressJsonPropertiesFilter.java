package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SuppressJsonPropertiesFilter implements ApiFilter {

    private final JsonConverter jsonConverter;
    private final List<String> jsonProperties;

    public SuppressJsonPropertiesFilter(final JsonConverter jsonConverter, final String... jsonProperties) {
        this.jsonConverter = jsonConverter;
        this.jsonProperties = Arrays.asList(jsonProperties);
    }

    @Override
    public MutableResponse processRequest(final MutableRequest request, final HttpPipelineChain chain) {
        final MutableResponse response = chain.callNextFilter(request);
        if (response.getStatus() != 200 || !jsonConverter.isJson(response)) {
            return response;
        }
        final Map<String, Object> content = jsonConverter.readEntity(response);
        jsonProperties.forEach(jsonProperty -> {
            if (shouldPropertyFilteredOut(jsonProperty, request, response)) {
                content.remove(jsonProperty);
                filterOutPropertyFromImages(jsonProperty, content);
                jsonConverter.replaceEntity(response, content);
            }
        });
        return response;
    }

    private void filterOutPropertyFromImages(String jsonProperty, Map content) {
        Object mainImageSet = content.get(MAIM_IMAGE);
        if (mainImageSet != null && mainImageSet instanceof Map) {
            Map mainImageSetAsMap = (Map) mainImageSet;
            if (mainImageSetAsMap.size() > 1) {
                filterOutPropertyFromImageSet(jsonProperty, mainImageSetAsMap);
            }
        }

        Object embeddedImages = content.get(EMBEDS);
        if (embeddedImages != null && embeddedImages instanceof List) {
            List embeddedImagesAsList = (List) embeddedImages;
            for (Object embeddedImage : embeddedImagesAsList) {
                if (embeddedImage instanceof Map) {
                    Map embeddedImageAsMap = (Map) embeddedImage;
                    filterOutPropertyFromImageSet(jsonProperty, embeddedImageAsMap);
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
                    filterOutPropertyFromImageModel(jsonProperty, promotionalImageAsMap);
                }
            }
        }
    }

    private void filterOutPropertyFromImageSet(String jsonProperty, Map imageSet) {
        if (imageSet != null && imageSet.containsKey(jsonProperty)) {
            imageSet.remove(jsonProperty);
            Object members = imageSet.get(MEMBERS);
            if (members != null && members instanceof List) {
                List membersAsList = (List) members;
                for (Object member : membersAsList) {
                    if (member instanceof Map) {
                        Map memberAsMap = (Map) member;
                        filterOutPropertyFromImageModel(jsonProperty, memberAsMap);
                    }
                }
            }
        }
    }

    private void filterOutPropertyFromImageModel(String jsonProperty, Map imageModel) {
        if (imageModel != null && imageModel.containsKey(jsonProperty)) {
            imageModel.remove(jsonProperty);
        }
    }

    protected boolean shouldPropertyFilteredOut(final String jsonProperty, final MutableRequest request, final MutableResponse response) {
        final Map<String, Object> content = jsonConverter.readEntity(response);
        return content.containsKey(jsonProperty);
    }
}
