package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordChangeEventMapper extends BaseMapperX<MesProEdhrRecordChangeEventDO> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM mes_pro_edhr_record_change_event WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE")
    MesProEdhrRecordChangeEventDO selectByIdAndTenantForUpdate(@org.apache.ibatis.annotations.Param("id") Long id,
            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

    default PageResult<MesProEdhrRecordChangeEventDO> selectPage(EdhrRecordChangePageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getTargetScope, reqVO.getTargetScope())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MesProEdhrRecordChangeEventDO::getChangeStatus, reqVO.getChangeStatus())
                .orderByDesc(MesProEdhrRecordChangeEventDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("changeCode", text("change_code")),
                java.util.Map.entry("changeType", text("CASE change_type WHEN 'VOID' THEN '作废' ELSE '未知类型' END")),
                java.util.Map.entry("changeStatus", text("CASE change_status WHEN 'DRAFT' THEN '草稿' WHEN 'SUBMITTED' THEN '已提交' WHEN 'APPROVED' THEN '已批准' WHEN 'REJECTED' THEN '已拒绝' WHEN 'EFFECTIVE' THEN '已生效' ELSE '未知状态' END")),
                java.util.Map.entry("targetObject", text("CONCAT(CASE target_scope WHEN 'BATCH' THEN '批次' WHEN 'EXECUTION' THEN '执行记录' ELSE '--' END, ' ', COALESCE(CONCAT('#',batch_execution_id),'--'),' ',COALESCE(CONCAT('#',execution_id),'--'))")),
                java.util.Map.entry("statusChange", text("CONCAT(CASE previous_status WHEN 'DRAFT' THEN '草稿' WHEN 'SUBMITTED' THEN '待审批' WHEN 'APPROVED' THEN '已关闭' WHEN 'REJECTED' THEN '已驳回' WHEN 'VOIDED' THEN '已作废' WHEN 'REOPENED' THEN '已重开' ELSE '未知状态' END, ' ', CASE new_status WHEN 'DRAFT' THEN '草稿' WHEN 'SUBMITTED' THEN '待审批' WHEN 'APPROVED' THEN '已关闭' WHEN 'REJECTED' THEN '已驳回' WHEN 'VOIDED' THEN '已作废' WHEN 'REOPENED' THEN '已重开' ELSE '未知状态' END)")),
                java.util.Map.entry("reason", text("CONCAT(COALESCE(NULLIF(reason_category,''),'--'),' ',COALESCE(NULLIF(reason_text,''),'--'))")),
                java.util.Map.entry("requestedAt", dateTime("requested_at")),
                java.util.Map.entry("effectiveAt", dateTime("effective_at")))));
    }

}
