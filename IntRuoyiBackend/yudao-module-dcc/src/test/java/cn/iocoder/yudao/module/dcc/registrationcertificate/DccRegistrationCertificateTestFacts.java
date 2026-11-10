package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;

/** Explicit domestic facts for existing domestic test fixtures; never used by production reads. */
final class DccRegistrationCertificateTestFacts {
    static String domestic(JdbcTemplate jdbc, Long versionId) {
        return jdbc.queryForObject("""
            SELECT v.certificate_no,v.approval_date,v.effective_date,v.expiry_date,v.classification,v.remark,
                   c.first_obtained_date,c.product_master_id
              FROM dcc_registration_certificate_version v
              JOIN dcc_registration_certificate c ON c.id=v.certificate_id AND c.tenant_id=v.tenant_id
             WHERE v.id=?
            """,(rs,n)-> {
                var facts=new DccRegistrationCertificateFacts();
                facts.setCertificateType("DOMESTIC");facts.setCertificateStatus("VALID");
                facts.setOwnProduct(rs.getObject("product_master_id")!=null);
                facts.setCertificateNo(rs.getString("certificate_no"));facts.setClassification(rs.getString("classification"));
                facts.setRemark(rs.getString("remark"));
                facts.setFirstObtainedDate(rs.getObject("first_obtained_date",LocalDate.class));
                facts.setApprovalDate(rs.getObject("approval_date",LocalDate.class));
                facts.setEffectiveDate(rs.getObject("effective_date",LocalDate.class));
                facts.setExpiryDate(rs.getObject("expiry_date",LocalDate.class));
                return facts.toJson();
            },versionId);
    }
    static String domestic(DccRegistrationCertificateVersionDO version,LocalDate firstObtainedDate) {
        var facts=new DccRegistrationCertificateFacts();
        facts.setCertificateType("DOMESTIC");facts.setCertificateStatus("VALID");facts.setOwnProduct(true);
        facts.setCertificateNo(version.getCertificateNo());facts.setClassification(version.getClassification());
        facts.setRemark(version.getRemark());facts.setFirstObtainedDate(firstObtainedDate);
        facts.setApprovalDate(version.getApprovalDate());facts.setEffectiveDate(version.getEffectiveDate());
        facts.setExpiryDate(version.getExpiryDate());return facts.toJson();
    }
    static String portableMapperFixture() {
        var facts=new DccRegistrationCertificateFacts();
        facts.setCertificateType("DOMESTIC");facts.setCertificateStatus("VALID");facts.setOwnProduct(true);
        facts.setCertificateNo("PORTABLE-001");facts.setClassification("II");
        facts.setFirstObtainedDate(LocalDate.of(2026,1,1));facts.setEffectiveDate(LocalDate.of(2026,8,17));
        facts.setExpiryDate(LocalDate.of(2031,8,17));return facts.toJson();
    }
}
