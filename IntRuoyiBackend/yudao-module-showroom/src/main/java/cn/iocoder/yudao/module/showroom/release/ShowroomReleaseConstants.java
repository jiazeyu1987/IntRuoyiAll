package cn.iocoder.yudao.module.showroom.release;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ShowroomReleaseConstants {

    static final Charset UTF_8 = StandardCharsets.UTF_8;
    static final int SCHEMA_VERSION = 1;
    static final String POINTER_KEY = "current";
    static final int RETAIN_COUNT = 3;
    static final String RELEASE_STATUS_PUBLISHED = "PUBLISHED";
    static final String RELEASE_STATUS_PURGED = "PURGED";
    static final String LEGACY_STATUS_READY = "READY";
    static final String LEGACY_STATUS_BROKEN = "BROKEN";
    static final String RESOURCE_TYPE_RELEASE = "release";
    static final String RESOURCE_TYPE_DOCUMENT = "document";
    static final String RESOURCE_TYPE_ASSET = "asset";
    static final String ASSET_TYPE_IMAGE = "image";
    static final String ASSET_TYPE_AUDIO = "audio";
    static final String ASSET_TYPE_VIDEO = "video";
    static final String ASSET_TYPE_TEXT = "text";
    static final String DOCUMENT_KIND_WEBSITE_INDEX = "website-index";
    static final String DOCUMENT_KIND_PRODUCT_DETAIL = "product-detail";
    static final String DOCUMENT_KIND_AWARD_DETAIL = "award-detail";
    static final String DOCUMENT_ID_WEBSITE_INDEX = "website-index";
    static final String TARGET_COMPANY = "COMPANY";
    static final String TARGET_PRODUCT = "PRODUCT";
    static final String TARGET_AWARD = "AWARD";
    static final String TARGET_HALL = "HALL";
    static final String OWNER_COMPANY_ASSET_ID = "company-home-image";
    static final String OWNER_COMPANY_AUDIO_ZH_ASSET_ID = "company-audio-zh";
    static final String OWNER_COMPANY_AUDIO_EN_ASSET_ID = "company-audio-en";
    static final List<String> COMPANY_WEBSITE_FIELD_ORDER = List.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info"
    );
    static final List<String> PRODUCT_FIELD_ORDER = List.of(
            "owner_company_id",
            "product_owner_type",
            "lifecycle_stage",
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification"
    );
    static final List<String> PRODUCT_TRANSLATABLE_FIELD_KEYS = List.of(
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification",
            "registration_certificate",
            "clinical_effect",
            "fim_status"
    );
    static final DateTimeFormatter RELEASE_ID_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private ShowroomReleaseConstants() {
    }
}
