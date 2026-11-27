package cn.iocoder.yudao.module.dcc.registrationcertificate.domain;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** The certificate's stated facts, kept with each immutable approval snapshot. */
@Data
public class DccRegistrationCertificateFacts {
    private DccRegistrationCertificateProjectProduct projectProduct;
    private List<DccRegistrationCertificateProjectProduct> projectProducts;
    private String historicalSheet;
    private Integer historicalRow;
    private java.util.Map<String, String> historicalValues;
    private String certificateType;
    private String certificateStatus;
    private String registrationLink;
    private String certificateHolder;
    private String productNameEn;
    private String productCode;
    private String projectCode;
    private String tradeName;
    private String notifiedBodyCode;
    private List<CeCertificate> ceCertificates;
    private List<String> modelSpecifications;
    private String submissionType;
    private String kpNumber;
    private String regulationCode;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate approvalDate;
    private String listingNumber;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate listingDate;
    private Boolean customerHeld;
    private String approvalLetterStatus;
    private String factoryRegistrationStatus;
    private Boolean ownProduct;
    private String changeDescription;
    private String otherContentDescription;
    private String certificateNo;
    private String classification;
    private String remark;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate firstObtainedDate;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate effectiveDate;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate expiryDate;

    @Data
    public static class CeCertificate {
        private String revision;
        private String certificateNo;
        @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,pattern="yyyy-MM-dd")
    private LocalDate issueDate;
    }

    public static DccRegistrationCertificateFacts parse(String json) {
        if (json == null || json.isBlank()) throw invalidParamException("注册证完整信息缺失，请先执行注册证字段迁移");
        DccRegistrationCertificateFacts facts = JsonUtils.parseObject(json, DccRegistrationCertificateFacts.class);
        if (facts == null) throw invalidParamException("注册证完整信息格式无效");
        return facts;
    }

    public void validateCreation() {
        if (changeDescription != null) throw invalidParamException("新建注册证不支持变更描述字段");
        if (!"FDA".equals(certificateType) && approvalDate != null)
            throw invalidParamException("国内和CE新建注册证不支持批准日期字段");
        if ("CE".equals(certificateType) && (firstObtainedDate != null || modelSpecifications != null))
            throw invalidParamException("CE新建注册证不支持首次获证日期和型号规格字段");
        validate();
    }

    public void validateChangeFrom(DccRegistrationCertificateFacts before) {
        validate();
        boolean modelsChanged = "CE".equals(certificateType) && !java.util.Objects.equals(before.modelSpecifications, modelSpecifications);
        boolean approvalChanged = isFda() && (!java.util.Objects.equals(before.submissionType, submissionType)
                || !java.util.Objects.equals(before.kpNumber, kpNumber) || !java.util.Objects.equals(before.regulationCode, regulationCode)
                || !java.util.Objects.equals(before.productCode, productCode) || !java.util.Objects.equals(before.approvalDate, approvalDate)
                || !java.util.Objects.equals(before.listingNumber, listingNumber) || !java.util.Objects.equals(before.listingDate, listingDate));
        if ((modelsChanged || approvalChanged) && (blank(changeDescription) || java.util.Objects.equals(before.changeDescription == null ? null : before.changeDescription.trim(), changeDescription == null ? null : changeDescription.trim())))
            throw invalidParamException(modelsChanged ? "CE型号调整必须填写本次调整说明" : "新增或替代FDA批准信息必须填写本次变更描述");
    }

    public void validate() {
        if (!Set.of("DOMESTIC", "CE", "FDA").contains(certificateType == null ? "" : certificateType))
            throw invalidParamException("请选择国内、CE或FDA注册证类型");
        if (ownProduct == null) throw invalidParamException("请选择是否为瑛泰产品");
        if (blank(classification)) throw invalidParamException("请填写产品分类");
        validateLength(certificateNo, 128, "注册证号");
        validateLength(classification, 64, "产品分类");
        validateLength(remark, 1024, "备注");
        if ("FDA".equals(certificateType)) {
            if (certificateStatus != null || expiryDate != null || effectiveDate != null || firstObtainedDate != null)
                throw invalidParamException("FDA请使用批准信函及工厂登记状态，不得携带国内证件状态或有效期字段");
            if (!Set.of("EXEMPT", "PMA", "TRADITIONAL_510K", "SPECIAL_510K", "ABBREVIATED_510K").contains(submissionType == null ? "" : submissionType))
                throw invalidParamException("FDA注册提交类型无效");
            if ("TRADITIONAL_510K".equals(submissionType) && blank(kpNumber))
                throw invalidParamException("请填写K/P号");
            if (!Set.of("VALID", "REVOKED").contains(approvalLetterStatus == null ? "" : approvalLetterStatus)
                    || !Set.of("ACTIVE", "INACTIVE").contains(factoryRegistrationStatus == null ? "" : factoryRegistrationStatus)
                    || customerHeld == null) throw invalidParamException("请填写FDA批准信函状态、工厂登记状态及是否客户持证");
        } else {
            Set<String> statuses = "CE".equals(certificateType) ? Set.of("VALID", "SUSPENDED", "REVOKED", "EXPIRED") : Set.of("VALID", "EXPIRED", "REVOKED");
            if (!statuses.contains(certificateStatus == null ? "" : certificateStatus)) throw invalidParamException("证件状态无效");
            if (expiryDate == null) throw invalidParamException("请填写有效期至");
            if ("DOMESTIC".equals(certificateType) && (blank(certificateNo) || firstObtainedDate == null || effectiveDate == null))
                throw invalidParamException("请填写国内注册证号、首次获证日期和生效日期");
        }
        if ("CE".equals(certificateType) && modelSpecifications != null) {
            Set<String> models = new java.util.HashSet<>();
            for (String model : modelSpecifications) {
                if (blank(model) || !models.add(model.trim()))
                    throw invalidParamException("型号规格须填写且不能重复");
            }
            modelSpecifications = modelSpecifications.stream().map(String::trim).toList();
        }
        if ("CE".equals(certificateType) && ceCertificates != null) {
            Set<String> revisions = new java.util.HashSet<>();
            for (int index = 0; index < ceCertificates.size(); index++) {
                CeCertificate certificate = ceCertificates.get(index);
                if (certificate == null || blank(certificate.revision) || !revisions.add(certificate.revision.trim()))
                    throw invalidParamException("CE证书记录须填写不重复的Rev.版本；编号及签发日期可单独留空");
                validateLength(certificate.certificateNo, 128, "第" + (index + 1) + "条CE证书编号");
            }
        }
        if (effectiveDate != null && expiryDate != null && !effectiveDate.isBefore(expiryDate)) throw invalidParamException("生效日期必须早于有效期至");
        if (firstObtainedDate != null && effectiveDate != null && firstObtainedDate.isAfter(effectiveDate)) throw invalidParamException("首次获证日期不能晚于生效日期");
        if (registrationLink != null && !registrationLink.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(registrationLink);
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException error) { throw invalidParamException("注册证链接须为有效HTTP或HTTPS地址"); }
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isFda() { return "FDA".equals(certificateType); }
    public boolean isExpired(LocalDate at) { return !isFda() && ("EXPIRED".equals(certificateStatus) || (expiryDate != null && expiryDate.isBefore(at))); }
    public String toJson() { return JsonUtils.toJsonString(this); }
    private static void validateLength(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw invalidParamException(label + "长度不能超过" + max + "个字符");
        }
    }
    private static boolean blank(String text) { return text == null || text.isBlank(); }
}
