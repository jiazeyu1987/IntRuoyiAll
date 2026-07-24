package cn.iocoder.yudao.module.showroom.foundation.meta;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalPreviewRow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ShowroomFieldDisplaySupport {

    private static final String TARGET_COMPANY = "COMPANY";
    private static final String TARGET_PRODUCT = "PRODUCT";

    private static final Map<String, String> COMPANY_FIELD_LABELS = Map.ofEntries(
            Map.entry("development_history", "发展历程"),
            Map.entry("development_history_en", "发展历程(英文)"),
            Map.entry("park_introduction", "园区介绍"),
            Map.entry("park_introduction_en", "园区介绍(英文)"),
            Map.entry("incubation_platform", "孵化平台"),
            Map.entry("incubation_platform_en", "孵化平台(英文)"),
            Map.entry("subsidiary_overview", "子公司概览"),
            Map.entry("subsidiary_overview_en", "子公司概览(英文)"),
            Map.entry("stock_info", "上市信息"),
            Map.entry("stock_info_en", "上市信息(英文)"),
            Map.entry("cover_image", "公司封面"),
            Map.entry("core_manufacturing_capability", "核心制造能力"),
            Map.entry("core_manufacturing_capability_en", "核心制造能力(英文)"),
            Map.entry("honors_awards", "荣誉资质"),
            Map.entry("honors_awards_en", "荣誉资质(英文)")
    );

    private static final Map<String, String> COMPANY_FIELD_LABELS_EN = Map.ofEntries(
            Map.entry("development_history", "Development History"),
            Map.entry("park_introduction", "Park Introduction"),
            Map.entry("incubation_platform", "Incubation Platform"),
            Map.entry("subsidiary_overview", "Subsidiary Overview"),
            Map.entry("stock_info", "Listing Information"),
            Map.entry("cover_image", "Company Cover"),
            Map.entry("core_manufacturing_capability", "Core Manufacturing Capability"),
            Map.entry("honors_awards", "Honors and Awards")
    );

    private static final Map<String, String> PRODUCT_FIELD_LABELS = Map.ofEntries(
            Map.entry("name_cn", "中文名称"),
            Map.entry("name_en", "英文名称"),
            Map.entry("owner_company_id", "所属公司"),
            Map.entry("product_owner_type", "产品归属/类型"),
            Map.entry("lifecycle_stage", "生命周期"),
            Map.entry("target_market", "在售国家"),
            Map.entry("target_market_en", "在售国家(英文)"),
            Map.entry("pipeline_layout", "BU"),
            Map.entry("pipeline_layout_en", "BU(英文)"),
            Map.entry("indication_content", "适应症"),
            Map.entry("indication_content_en", "适应症(英文)"),
            Map.entry("core_selling_points", "卖点文案"),
            Map.entry("core_selling_points_en", "卖点文案(英文)"),
            Map.entry("model_specification", "型号规格"),
            Map.entry("model_specification_en", "型号规格(英文)"),
            Map.entry("cover_image", "封面"),
            Map.entry("registration_certificate", "注册证"),
            Map.entry("registration_certificate_en", "注册证(英文)"),
            Map.entry("clinical_effect", "临床效果"),
            Map.entry("clinical_effect_en", "临床效果(英文)"),
            Map.entry("fim_status", "FIM状态"),
            Map.entry("fim_status_en", "FIM状态(英文)")
    );

    private static final Map<String, String> PRODUCT_FIELD_LABELS_EN = Map.ofEntries(
            Map.entry("name_cn", "Chinese Name"),
            Map.entry("name_en", "English Name"),
            Map.entry("owner_company_id", "Owner Company"),
            Map.entry("product_owner_type", "Product Ownership / Type"),
            Map.entry("lifecycle_stage", "Lifecycle Stage"),
            Map.entry("target_market", "Countries on Sale"),
            Map.entry("target_market_en", "Countries on Sale (English)"),
            Map.entry("pipeline_layout", "BU"),
            Map.entry("pipeline_layout_en", "BU (English)"),
            Map.entry("indication_content", "Indication"),
            Map.entry("indication_content_en", "Indication (English)"),
            Map.entry("core_selling_points", "Selling Points Copy"),
            Map.entry("core_selling_points_en", "Selling Points Copy (English)"),
            Map.entry("model_specification", "Model Specification"),
            Map.entry("model_specification_en", "Model Specification (English)"),
            Map.entry("cover_image", "Cover Image"),
            Map.entry("registration_certificate", "Registration Certificate"),
            Map.entry("registration_certificate_en", "Registration Certificate (English)"),
            Map.entry("clinical_effect", "Clinical Effect"),
            Map.entry("clinical_effect_en", "Clinical Effect (English)"),
            Map.entry("fim_status", "FIM Status"),
            Map.entry("fim_status_en", "FIM Status (English)")
    );

    private ShowroomFieldDisplaySupport() {
    }

    public static String fieldLabel(String targetType, String fieldCode) {
        if (TARGET_COMPANY.equals(targetType)) {
            String label = COMPANY_FIELD_LABELS.get(fieldCode);
            if (label == null) {
                throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unknown company field " + fieldCode);
            }
            return label;
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            String label = PRODUCT_FIELD_LABELS.get(fieldCode);
            if (label == null) {
                ShowroomFieldCatalog.productField(fieldCode);
                throw new IllegalStateException(
                        "SHOWROOM_TARGET_NOT_FOUND: unsupported product field label " + fieldCode);
            }
            return label;
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported field label target type "
                + targetType);
    }

    public static String fieldLabelEn(String targetType, String fieldCode) {
        if (TARGET_COMPANY.equals(targetType)) {
            String label = COMPANY_FIELD_LABELS_EN.get(fieldCode);
            if (label == null) {
                throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unknown company english field " + fieldCode);
            }
            return label;
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            String label = PRODUCT_FIELD_LABELS_EN.get(fieldCode);
            if (label == null) {
                ShowroomFieldCatalog.productField(fieldCode);
                throw new IllegalStateException(
                        "SHOWROOM_TARGET_NOT_FOUND: unsupported product english field label " + fieldCode);
            }
            return label;
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported english field label target type "
                + targetType);
    }

    public static String formatJsonWrappedFieldValue(String targetType, String fieldCode, String jsonValue,
                                                     ShowroomPersistentContentService contentService) {
        return formatStoredFieldValue(targetType, fieldCode, unwrapJsonFieldValue(fieldCode, jsonValue), contentService);
    }

    public static String formatStoredFieldValue(String targetType, String fieldCode, String rawValue,
                                                ShowroomPersistentContentService contentService) {
        return formatStoredFieldValue(targetType, fieldCode, rawValue, contentService, false);
    }

    public static String formatStoredFieldValue(String targetType, String fieldCode, String rawValue,
                                                ShowroomPersistentContentService contentService,
                                                boolean english) {
        if (!hasText(rawValue)) {
            return "空";
        }
        if (TARGET_COMPANY.equals(targetType)) {
            return rawValue;
        }
        if (TARGET_PRODUCT.equals(targetType)) {
            return formatProductFieldValue(fieldCode, rawValue, contentService, english);
        }
        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported display target type " + targetType);
    }

    public static List<ShowroomApprovalPreviewRow> buildPreviewRows(String targetType, Map<String, String> liveFields,
                                                                    Map<String, String> targetFields,
                                                                    ShowroomPersistentContentService contentService) {
        LinkedHashSet<String> fieldCodes = new LinkedHashSet<>();
        fieldCodes.addAll(targetFields.keySet());
        fieldCodes.addAll(liveFields.keySet());
        return fieldCodes.stream()
                .map(fieldCode -> new ShowroomApprovalPreviewRow(fieldCode,
                        fieldLabel(targetType, fieldCode),
                        formatStoredFieldValue(targetType, fieldCode, liveFields.get(fieldCode), contentService),
                        formatStoredFieldValue(targetType, fieldCode, targetFields.get(fieldCode), contentService)))
                .toList();
    }

    private static String unwrapJsonFieldValue(String fieldCode, String jsonValue) {
        if (!hasText(jsonValue)) {
            return "";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = JsonUtils.parseObject(jsonValue, Map.class);
            if (payload == null || !payload.containsKey("value")) {
                throw new IllegalStateException(
                        "SHOWROOM_REQUIRED_FIELD_MISSING: approval display value missing for field " + fieldCode);
            }
            Object rawValue = payload.get("value");
            return rawValue == null ? "" : String.valueOf(rawValue);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "SHOWROOM_REQUIRED_FIELD_MISSING: invalid approval display json for field " + fieldCode, ex);
        }
    }

    private static String formatProductFieldValue(String fieldCode, String rawValue,
                                                  ShowroomPersistentContentService contentService,
                                                  boolean english) {
        if ("owner_company_id".equals(fieldCode)) {
            try {
                ShowroomCompanySnapshot company = contentService.getCompany(Long.valueOf(rawValue.trim()));
                return english && hasText(company.displayNameEn()) ? company.displayNameEn() : company.displayName();
            } catch (RuntimeException ignored) {
                return rawValue;
            }
        }
        if ("product_owner_type".equals(fieldCode)) {
            if ("YINGTAI".equalsIgnoreCase(rawValue)) {
                return english ? "Yingtai Product" : "盈泰产品";
            }
            if ("SUBSIDIARY".equalsIgnoreCase(rawValue)) {
                return english ? "Subsidiary Product" : "子公司产品";
            }
        }
        if ("lifecycle_stage".equals(fieldCode)) {
            if ("REGISTERED".equalsIgnoreCase(rawValue)) {
                return english ? "Registered" : "已注册";
            }
            if ("R_AND_D".equalsIgnoreCase(rawValue)) {
                return english ? "R&D" : "研发中";
            }
        }
        return rawValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
