package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Product catalog identity is the uniqueness boundary, including pending uploads. */
@Service
public class DccRegistrationCertificateProductBindingService {
    private final JdbcTemplate jdbc;

    public DccRegistrationCertificateProductBindingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Occupancy(Long certificateId, String certificateNo, String state) { }

    public Occupancy occupancy(Long tenantId, Long productId) {
        var rows = jdbc.query("""
                SELECT b.certificate_id, v.certificate_no, b.state
                FROM dcc_registration_certificate_product b
                JOIN dcc_registration_certificate c ON c.id=b.certificate_id AND c.tenant_id=b.tenant_id
                LEFT JOIN dcc_registration_certificate_version v
                  ON v.certificate_id=c.id AND v.tenant_id=c.tenant_id AND v.deleted=0
                  AND v.id=COALESCE(c.current_version_id,c.pending_version_id,
                    (SELECT MAX(v2.id) FROM dcc_registration_certificate_version v2
                     WHERE v2.certificate_id=c.id AND v2.tenant_id=c.tenant_id AND v2.deleted=0))
                WHERE b.tenant_id=? AND b.product_catalog_id=?
                """, (rs, n) -> new Occupancy(rs.getLong(1), rs.getString(2), rs.getString(3)), tenantId, productId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void assertAvailable(Long tenantId, Long productId, Long certificateId, String name) {
        var occupied = occupancy(tenantId, productId);
        if (occupied != null && !Objects.equals(occupied.certificateId(), certificateId)) {
            throw invalidParamException(conflictMessage(name, occupied));
        }
    }

    public static String conflictMessage(String name, Occupancy occupied) {
        return "产品「" + name + "」已" + ("RESERVED".equals(occupied.state()) ? "被审批中的注册证占用" : "绑定注册证")
                + "（" + (occupied.certificateNo() == null ? "编号待确认，ID=" + occupied.certificateId() : occupied.certificateNo()) + "）";
    }

    @Transactional(rollbackFor = Exception.class)
    public void reserve(Long tenantId, Long certificateId, List<DccRegistrationCertificateProjectProduct> products, Long actorId) {
        // Stable product order also keeps concurrent multi-product requests from taking locks in reverse order.
        for (var product : products.stream().sorted(java.util.Comparator.comparing(DccRegistrationCertificateProjectProduct::productCatalogId)).toList()) {
            assertAvailable(tenantId, product.productCatalogId(), certificateId, product.productName());
            try {
                jdbc.update("""
                        INSERT INTO dcc_registration_certificate_product
                          (tenant_id,product_catalog_id,certificate_id,project_code_id,product_code,product_name,state,created_by)
                        VALUES (?,?,?,?,?,?,'RESERVED',?)
                        """, tenantId, product.productCatalogId(), certificateId, product.projectCodeId(), product.productCode(), product.productName(), actorId);
            } catch (DuplicateKeyException ex) {
                // A competing insert can win after the availability check; the unique key is authoritative.
                var rows = jdbc.queryForList("SELECT certificate_id,project_code_id FROM dcc_registration_certificate_product WHERE tenant_id=? AND product_catalog_id=? FOR UPDATE",
                        tenantId, product.productCatalogId());
                if (rows.size()!=1 || !Objects.equals(((Number) rows.get(0).get("certificate_id")).longValue(), certificateId)
                        || !Objects.equals(((Number) rows.get(0).get("project_code_id")).longValue(), product.projectCodeId())) {
                    throw invalidParamException("产品「" + product.productName() + "」的注册证关联已被其他申请占用，请刷新后重试");
                }
            }
        }
    }

    public void releasePending(Long tenantId, Long certificateId) {
        jdbc.update("DELETE FROM dcc_registration_certificate_product WHERE tenant_id=? AND certificate_id=? AND state='RESERVED'", tenantId, certificateId);
    }

    public void confirm(Long tenantId, Long certificateId, List<DccRegistrationCertificateProjectProduct> products, Long actorId) {
        reserve(tenantId, certificateId, products, actorId);
        jdbc.update("UPDATE dcc_registration_certificate_product SET state='BOUND' WHERE tenant_id=? AND certificate_id=? AND state='RESERVED'", tenantId, certificateId);
    }

    public boolean containsProduct(Long tenantId, Long certificateId, Long productId) {
        var occupied=occupancy(tenantId,productId);
        return occupied!=null && "BOUND".equals(occupied.state()) && Objects.equals(certificateId,occupied.certificateId());
    }

    public Occupancy projectOccupancy(Long tenantId, Long projectId) {
        var ids=jdbc.queryForList("SELECT product_catalog_id FROM dcc_registration_certificate_product WHERE tenant_id=? AND project_code_id=?",Long.class,tenantId,projectId);
        return ids.isEmpty()?null:occupancy(tenantId,ids.get(0));
    }

    public boolean containsProject(Long tenantId, Long certificateId, Long projectId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product WHERE tenant_id=? AND certificate_id=? AND project_code_id=? AND state='BOUND'",
                Long.class, tenantId, certificateId, projectId) > 0;
    }
}
