package cn.iocoder.yudao.module.showroom.foundation.meta;

import cn.iocoder.yudao.module.showroom.foundation.enums.ShowroomFieldTierEnum;

import java.util.Map;

public final class ShowroomFieldCatalog {

    private static final Map<String, ShowroomFieldDefinition> PRODUCT_FIELDS = Map.ofEntries(
            basic("name_cn", true),
            basic("name_en", true),
            basic("owner_company_id", false),
            basic("product_owner_type", false),
            basic("lifecycle_stage", false),
            basic("target_market", false),
            basic("target_market_en", false),
            basic("pipeline_layout", false),
            basic("pipeline_layout_en", false),
            basic("indication_content", false),
            basic("indication_content_en", false),
            basic("core_selling_points", false),
            basic("core_selling_points_en", false),
            basic("model_specification", false),
            basic("model_specification_en", false),
            basic("cover_image", false),
            advanced("registration_certificate"),
            advanced("registration_certificate_en"),
            advanced("clinical_effect"),
            advanced("clinical_effect_en"),
            advanced("fim_status"),
            advanced("fim_status_en")
    );

    private ShowroomFieldCatalog() {
    }

    public static ShowroomFieldDefinition productField(String fieldCode) {
        ShowroomFieldDefinition definition = PRODUCT_FIELDS.get(fieldCode);
        if (definition == null) {
            throw new IllegalArgumentException("SHOWROOM_TARGET_NOT_FOUND: unknown product field " + fieldCode);
        }
        return definition;
    }

    public static Map<String, ShowroomFieldDefinition> productFields() {
        return PRODUCT_FIELDS;
    }

    private static Map.Entry<String, ShowroomFieldDefinition> basic(String code, boolean publishRequired) {
        return Map.entry(code, new ShowroomFieldDefinition(code, ShowroomFieldTierEnum.BASIC, publishRequired));
    }

    private static Map.Entry<String, ShowroomFieldDefinition> advanced(String code) {
        return Map.entry(code, new ShowroomFieldDefinition(code, ShowroomFieldTierEnum.ADVANCED, false));
    }

}
