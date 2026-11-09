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

    public void validate() {
        if (!Set.of("DOMESTIC", "CE", "FDA").contains(certificateType == null ? "" : certificateType))
            throw invalidParamException("请选择国内、CE或FDA注册证类型");
        if (ownProduct == null) throw invalidParamException("请选择是否为瑛泰产品");
        if (blank(classification)) throw invalidParamException("请填写产品分类");
        if ("FDA".equals(certificateType)) {
            if (certificateStatus != null || expiryDate != null || effectiveDate != null || firstObtainedDate != null)
                throw invalidParamException("FDA请使用批准信函及工厂登记状态，不得携带国内证件状态或有效期字段");
            if (!Set.of("EXEMPT", "PMA", "TRADITIONAL_510K", "SPECIAL_510K", "ABBREVIATED_510K").contains(submissionType == null ? "" : submissionType))
                throw invalidParamException("FDA注册提交类型无效");
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
        if ("CE".equals(certificateType) && ceCertificates != null) {
            Set<String> revisions = new java.util.HashSet<>();
            for (CeCertificate certificate : ceCertificates) {
                if (certificate == null || blank(certificate.revision) || !revisions.add(certificate.revision.trim()))
                    throw invalidParamException("CE证书记录须填写不重复的Rev.版本；编号及签发日期可单独留空");
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
    private static boolean blank(String text) { return text == null || text.isBlank(); }
}
