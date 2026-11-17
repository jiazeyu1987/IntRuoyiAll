package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeviationBatchOptionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDeviationOrderMapper extends BaseMapperX<MesProWorkOrderDO> {
    default PageResult<MesProWorkOrderDO> selectOptionsPage(MesProEdhrDeviationBatchOptionPageReqVO req, Long tenantId) {
        var query = new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getTenantId, tenantId)
                .exists("SELECT 1 FROM mes_pro_process_pool_active_order a WHERE a.tenant_id=mes_pro_work_order.tenant_id "
                        + "AND a.work_order_id=mes_pro_work_order.id AND a.deleted=0 "
                        + "AND a.active_status IN ('ACTIVE','CLOSED') AND a.business_status IN ('ACTIVE','COMPLETED','RELEASED') "
                        + "AND NOT EXISTS (SELECT 1 FROM mes_pro_edhr_batch_execution_origin o "
                        + "JOIN mes_pro_edhr_release_transaction r ON r.tenant_id=o.tenant_id AND r.batch_execution_id=o.batch_execution_id "
                        + "WHERE o.tenant_id=a.tenant_id AND o.active_order_id=a.id AND o.deleted=0 "
                        + "AND o.entry_type IN ('ACTIVE_ORDER_COMPLETION','ACTIVE_ORDER_SCHEDULED','ACTIVE_ORDER_PQC','MANUAL_CONTROLLED_RETRY') "
                        + "AND r.deleted=0 AND r.release_status='RELEASED')")
                .like(StrUtil.isNotBlank(req.getSearch()), MesProWorkOrderDO::getCode, req.getSearch())
                .orderByDesc(MesProWorkOrderDO::getId);
        return selectPage(req, query);
    }
}
