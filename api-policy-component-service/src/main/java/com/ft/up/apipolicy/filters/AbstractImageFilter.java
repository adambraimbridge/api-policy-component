package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.pipeline.ApiFilter;

import java.util.List;
import java.util.Map;

public abstract class AbstractImageFilter implements ApiFilter {

    protected void applyFilter(String jsonProperty, FieldModifier modifier, Map content) {
        modifier.operation(jsonProperty, content);
        Object mainImageSet = content.get(MAIM_IMAGE);
        if (mainImageSet instanceof Map) {
            Map mainImageSetAsMap = (Map) mainImageSet;
            if (mainImageSetAsMap.size() > 1) {
                applyFilterToFromImageSet(jsonProperty, modifier, mainImageSetAsMap);
            }
        }

        Object embeddedImages = content.get(EMBEDS);
        if (embeddedImages instanceof List) {
            List embeddedImagesAsList = (List) embeddedImages;
            for (Object embeddedImage : embeddedImagesAsList) {
                if (embeddedImage instanceof Map) {
                    Map embeddedImageAsMap = (Map) embeddedImage;
                    applyFilterToFromImageSet(jsonProperty, modifier, embeddedImageAsMap);
                }
            }
        }

        Object alternativeImages = content.get(ALTERNATIVE_IMAGES);
        if (alternativeImages instanceof Map) {
            Map alternativeImagesAsMap = (Map) alternativeImages;
            Object promotionalImage = alternativeImagesAsMap.get(PROMOTIONAL_IMAGE);
            if (promotionalImage instanceof Map) {
                Map promotionalImageAsMap = (Map) promotionalImage;
                if (promotionalImageAsMap.size() > 1) {
                    applyFilterToImageModel(jsonProperty, modifier, promotionalImageAsMap);
                }
            }
        }

		Object leadImages = content.get(LEAD_IMAGES);
		if (leadImages instanceof List) {
			List leadImagesAsList = (List) leadImages;
			for (Object leadImage : leadImagesAsList) {
				if (leadImage instanceof Map) {
					Map leadImageAsMap = (Map) ((Map) leadImage).get(IMAGE);
					applyFilterToImageModel(jsonProperty, modifier, leadImageAsMap);
				}
			}
		}
    }

    private void applyFilterToFromImageSet(String jsonProperty, FieldModifier modifier, Map imageSet) {
        if (imageSet != null) {
            modifier.operation(jsonProperty, imageSet);
            Object members = imageSet.get(MEMBERS);
            if (members instanceof List) {
                List membersAsList = (List) members;
                for (Object member : membersAsList) {
                    if (member instanceof Map) {
                        Map memberAsMap = (Map) member;
                        applyFilterToImageModel(jsonProperty, modifier, memberAsMap);
                    }
                }
            }
        }
    }

    private void applyFilterToImageModel(String jsonProperty, FieldModifier modifier, Map imageModel) {
        if (imageModel != null) {
            modifier.operation(jsonProperty, imageModel);
        }
    }

    interface FieldModifier {
        void operation(String jsonProperty, Map<String, Object> content);
    }
}
