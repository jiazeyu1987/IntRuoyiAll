package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/** Exact tenant provenance, including the physical catalog tenant omitted by its legacy DO. */
@Mapper
public interface DccApprovedProductIdentityMapper {

    record ApprovedProduct(Long projectId, Long catalogId, Long relationId, Long requestId,
                           String productCode, String productName,String catalogCode,String catalogName) { }

    @Select("""
        SELECT p.id AS projectId,c.id AS catalogId,r.id AS relationId,q.id AS requestId,
               q.product_code AS productCode,q.product_name AS productName,
               c.product_code AS catalogCode,c.product AS catalogName
        FROM dcc_project_code p
        JOIN dcc_project_product_relation r ON r.project_code_id=p.id AND r.tenant_id=p.tenant_id
          AND r.deleted=0 AND r.relation_status='ACTIVE'
        JOIN dcc_project_product_create_request q ON q.id=r.request_id AND q.tenant_id=p.tenant_id
          AND q.deleted=0 AND q.status='COMPLETED'
          AND q.generated_project_code_id=p.id AND q.generated_product_catalog_id=r.product_catalog_id
          AND q.relation_id=r.id
        JOIN dcc_product_catalog c ON c.id=r.product_catalog_id AND c.tenant_id=p.tenant_id AND c.deleted=0
        WHERE p.id=#{projectId} AND p.tenant_id=#{tenantId} AND p.deleted=0 AND p.status='ENABLE'
        """)
    List<ApprovedProduct> selectApprovedProduct(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);

    @Select("""
        SELECT (SELECT COUNT(*) FROM dcc_project_product_create_request
                WHERE tenant_id=#{tenantId} AND generated_project_code_id=#{projectId})
             + (SELECT COUNT(*) FROM dcc_project_product_relation
                WHERE tenant_id=#{tenantId} AND project_code_id=#{projectId})
        """)
    long countDeclaredProductEvidence(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
}
