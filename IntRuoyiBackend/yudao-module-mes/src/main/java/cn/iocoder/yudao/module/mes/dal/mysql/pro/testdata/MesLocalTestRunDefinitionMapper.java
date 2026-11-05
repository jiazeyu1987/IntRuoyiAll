package cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata;

import cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunContracts.ApprovedProduct;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MesLocalTestRunDefinitionMapper {
  @Select(
      """
SELECT DISTINCT q.id requestId,p.id projectId,c.id catalogId,r.id relationId,i.id mesProductId,
q.product_code productCode,q.product_name productName,p.project_code projectCode
FROM dcc_project_product_create_request q
JOIN dcc_project_product_relation r ON r.id=q.relation_id AND r.request_id=q.id AND r.tenant_id=q.tenant_id AND r.deleted=0 AND r.relation_status='ACTIVE'
JOIN dcc_project_code p ON p.id=q.generated_project_code_id AND p.id=r.project_code_id AND p.tenant_id=q.tenant_id AND p.deleted=0 AND p.status='ENABLE'
JOIN dcc_product_catalog c ON c.id=q.generated_product_catalog_id AND c.id=r.product_catalog_id AND c.tenant_id=q.tenant_id AND c.deleted=0 AND c.product_code=q.product_code AND c.product=q.product_name
LEFT JOIN (
 SELECT DISTINCT b.tenant_id,b.dcc_project_code_id,rp.item_id,mi.code item_code,mi.name item_name
 FROM mes_pro_route_dcc_project_binding b
 JOIN mes_pro_route route ON route.id=b.route_id AND route.tenant_id=b.tenant_id AND route.deleted=0
 JOIN mes_pro_route_product rp ON rp.route_id=route.id AND rp.tenant_id=route.tenant_id AND rp.deleted=0
 JOIN mes_md_item mi ON mi.id=rp.item_id AND mi.tenant_id=rp.tenant_id AND mi.deleted=0 AND mi.status=0
 WHERE b.deleted=0
) mapping ON mapping.tenant_id=q.tenant_id AND mapping.dcc_project_code_id=p.id AND mapping.item_code=q.product_code AND mapping.item_name=q.product_name
LEFT JOIN mes_md_item i ON i.id=mapping.item_id AND i.tenant_id=q.tenant_id AND i.deleted=0 AND i.code=q.product_code AND i.name=q.product_name AND i.status=0
WHERE q.tenant_id=#{tenantId} AND q.deleted=0 AND q.status='COMPLETED' ORDER BY q.id,i.id
""")
  List<ApprovedProduct> selectApproved(@Param("tenantId") Long tenantId);

  @Select(
      """
SELECT COUNT(DISTINCT v.id) FROM mes_pro_route_product p
JOIN mes_pro_route r ON r.id=p.route_id AND r.tenant_id=p.tenant_id AND r.deleted=0 AND r.status=0
JOIN mes_pro_route_version v ON v.route_id=r.id AND v.tenant_id=r.tenant_id AND v.deleted=0 AND v.active=1 AND v.lifecycle_status='ACTIVE'
JOIN mes_pro_route_dcc_project_binding b ON b.route_id=r.id AND b.tenant_id=r.tenant_id AND b.deleted=0 AND b.dcc_project_code_id=#{projectId}
WHERE p.tenant_id=#{tenantId} AND p.deleted=0 AND p.item_id=#{itemId}
AND (SELECT COUNT(DISTINCT allp.route_id) FROM mes_pro_route_product allp
 JOIN mes_pro_route allr ON allr.id=allp.route_id AND allr.tenant_id=allp.tenant_id AND allr.deleted=0
 WHERE allp.tenant_id=#{tenantId} AND allp.deleted=0 AND allp.item_id=#{itemId})=1
""")
  int countExactActiveRoute(
      @Param("tenantId") Long tenantId,
      @Param("projectId") Long projectId,
      @Param("itemId") Long itemId);
}
