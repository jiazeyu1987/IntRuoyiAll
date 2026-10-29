package cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess;

import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.MesInProcessOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MesInProcessOrderMapper {
    @Select("""
        <script>
        SELECT a.* FROM mes_pro_process_pool_active_order a
        LEFT JOIN mes_pro_work_order w ON w.id=a.work_order_id AND w.tenant_id=a.tenant_id AND w.deleted=0
        LEFT JOIN mes_md_item p ON p.id=w.product_id AND p.tenant_id=a.tenant_id AND p.deleted=0
        WHERE a.tenant_id=#{tenantId} AND a.deleted=0
          AND a.active_status='ACTIVE' AND a.business_status IN ('ACTIVE','COMPLETED','VERSION_UPGRADE_PENDING')
        <if test="q.workOrderCode != null and q.workOrderCode.trim() != ''">
          AND LOCATE(TRIM(#{q.workOrderCode}),w.code)>0
        </if>
        <if test="q.productName != null and q.productName.trim() != ''">
          AND (LOCATE(TRIM(#{q.productName}),p.name)>0 OR LOCATE(TRIM(#{q.productName}),w.name)>0)
        </if>
        <if test="q.batchCode != null and q.batchCode.trim() != ''">
          AND LOCATE(TRIM(#{q.batchCode}),w.batch_code)>0
        </if>
        ORDER BY a.joined_at DESC,a.id DESC
        </script>
        """)
    List<MesProcessPoolActiveOrderDO> selectCandidates(@Param("tenantId") Long tenantId,
                                                     @Param("q") MesInProcessOrderPageReqVO request);
}
