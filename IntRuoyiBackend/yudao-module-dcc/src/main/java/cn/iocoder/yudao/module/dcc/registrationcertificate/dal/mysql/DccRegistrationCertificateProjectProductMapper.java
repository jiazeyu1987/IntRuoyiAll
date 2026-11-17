package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DccRegistrationCertificateProjectProductMapper {
    record Row(Long tenantId, Long projectCodeId, Long productCatalogId, String productCode,
               String productName, String source, Long relationId) { }

    @Select("""
        SELECT r.tenant_id AS tenantId, r.project_code_id AS projectCodeId,
               r.product_catalog_id AS productCatalogId, c.product_code AS productCode, c.product AS productName,
               r.source, r.relation_id AS relationId
        FROM (
            SELECT tenant_id, project_code_id, product_catalog_id, id AS relation_id, 'CONFIRMED' AS source
            FROM dcc_data_relation
            WHERE tenant_id=#{tenantId} AND project_code_id=#{projectId}
              AND deleted=0 AND relation_status='CONFIRMED'
            UNION ALL
            SELECT r.tenant_id, r.project_code_id, r.product_catalog_id, r.id, 'APPROVED'
            FROM dcc_project_product_relation r
            JOIN dcc_project_product_create_request q ON q.id=r.request_id AND q.tenant_id=r.tenant_id
              AND q.deleted=0 AND q.status='COMPLETED' AND q.generated_project_code_id=r.project_code_id
              AND q.generated_product_catalog_id=r.product_catalog_id AND q.relation_id=r.id
            WHERE r.tenant_id=#{tenantId} AND r.project_code_id=#{projectId}
              AND r.deleted=0 AND r.relation_status='ACTIVE'
        ) r
        LEFT JOIN dcc_product_catalog c ON c.id=r.product_catalog_id AND c.tenant_id=r.tenant_id AND c.deleted=0
        ORDER BY r.source, r.relation_id
        """)
    List<Row> selectBindings(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
}
