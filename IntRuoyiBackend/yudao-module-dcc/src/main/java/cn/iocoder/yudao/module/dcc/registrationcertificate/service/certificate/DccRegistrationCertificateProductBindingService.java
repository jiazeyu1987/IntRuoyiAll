package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** A product may belong to multiple certificates; each certificate keeps its own binding lifecycle. */
@Service
public class DccRegistrationCertificateProductBindingService {
    private final JdbcTemplate jdbc;

    public DccRegistrationCertificateProductBindingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Long> currentCertificateIds(Long tenantId, Long productId) {
        return jdbc.queryForList("""
                SELECT b.certificate_id FROM dcc_registration_certificate_product b
                JOIN dcc_registration_certificate c ON c.id=b.certificate_id AND c.tenant_id=b.tenant_id
                WHERE b.tenant_id=? AND b.product_catalog_id=? AND b.state='BOUND'
                  AND c.deleted=0 AND c.status='ACTIVE'
                ORDER BY b.certificate_id
                """, Long.class, tenantId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reserve(Long tenantId, Long certificateId, List<DccRegistrationCertificateProjectProduct> products, Long actorId) {
        // Stable product order also keeps concurrent multi-product requests from taking locks in reverse order.
        for (var product : products.stream().sorted(java.util.Comparator.comparing(DccRegistrationCertificateProjectProduct::productCatalogId)).toList()) {
            try {
                jdbc.update("""
                        INSERT INTO dcc_registration_certificate_product
                          (tenant_id,product_catalog_id,certificate_id,project_code_id,product_code,product_name,state,created_by)
                        VALUES (?,?,?,?,?,?,'RESERVED',?)
                        """, tenantId, product.productCatalogId(), certificateId, product.projectCodeId(), product.productCode(), product.productName(), actorId);
            } catch (DuplicateKeyException ex) {
                // An identical binding is idempotent; a different project/product within this certificate is a conflict.
                var rows = jdbc.queryForList("SELECT product_catalog_id,project_code_id FROM dcc_registration_certificate_product WHERE tenant_id=? AND certificate_id=? AND (product_catalog_id=? OR project_code_id=?) FOR UPDATE",
                        tenantId, certificateId, product.productCatalogId(), product.projectCodeId());
                if (rows.size()!=1 || !Objects.equals(((Number) rows.get(0).get("product_catalog_id")).longValue(), product.productCatalogId())
                        || !Objects.equals(((Number) rows.get(0).get("project_code_id")).longValue(), product.projectCodeId())) {
                    throw invalidParamException("产品「" + product.productName() + "」在本注册证中的项目与产品关联不一致，请刷新后核对");
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
        return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product WHERE tenant_id=? AND certificate_id=? AND product_catalog_id=? AND state='BOUND'",
                Long.class, tenantId, certificateId, productId) > 0;
    }

    public boolean containsProject(Long tenantId, Long certificateId, Long projectId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product WHERE tenant_id=? AND certificate_id=? AND project_code_id=? AND state='BOUND'",
                Long.class, tenantId, certificateId, projectId) > 0;
    }
}
