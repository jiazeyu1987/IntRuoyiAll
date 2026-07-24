package cn.iocoder.yudao.module.showroom.content.model;

import java.util.List;
import java.util.Map;

public record ShowroomProductDraft(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                   String legacyProductCode, Map<String, String> fields,
                                   List<ShowroomProductAttachment> attachments) {

    public ShowroomProductDraft(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                Map<String, String> fields, List<ShowroomProductAttachment> attachments) {
        this(productId, productMasterId, productCode, nameCn, nameEn, null, fields, attachments);
    }

    public ShowroomProductDraft(Long productId, String productCode, String nameCn, String nameEn,
                                Map<String, String> fields, List<ShowroomProductAttachment> attachments) {
        this(productId, null, productCode, nameCn, nameEn, null, fields, attachments);
    }

    public ShowroomProductDraft(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                Map<String, String> fields) {
        this(productId, productMasterId, productCode, nameCn, nameEn, null, fields, List.of());
    }

    public ShowroomProductDraft(Long productId, Long productMasterId, String productCode, String nameCn, String nameEn,
                                String legacyProductCode, Map<String, String> fields) {
        this(productId, productMasterId, productCode, nameCn, nameEn, legacyProductCode, fields, List.of());
    }

    public ShowroomProductDraft(Long productId, String productCode, String nameCn, String nameEn,
                                Map<String, String> fields) {
        this(productId, null, productCode, nameCn, nameEn, null, fields, List.of());
    }
}
