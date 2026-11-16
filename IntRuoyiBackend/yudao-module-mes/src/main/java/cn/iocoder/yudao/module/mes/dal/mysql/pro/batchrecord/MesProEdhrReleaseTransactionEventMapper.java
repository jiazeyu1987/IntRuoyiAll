package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReleaseTransactionEventMapper extends BaseMapperX<MesProEdhrReleaseTransactionEventDO> {

    default MesProEdhrReleaseTransactionEventDO selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
            Long releaseTransactionId, String eventType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseTransactionEventDO::getEventType, eventType)
                .eq(MesProEdhrReleaseTransactionEventDO::getIdempotencyKey, idempotencyKey));
    }

    default MesProEdhrReleaseTransactionEventDO selectByReleaseTransactionIdAndEventTypeAndIdempotencyKeyForUpdate(
            Long releaseTransactionId, String eventType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseTransactionEventDO::getEventType, eventType)
                .eq(MesProEdhrReleaseTransactionEventDO::getIdempotencyKey, idempotencyKey)
                .last("FOR UPDATE"));
    }

    default PageResult<MesProEdhrReleaseTransactionEventDO> selectPage(MesProEdhrReleaseEventPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, reqVO.getReleaseTransactionId())
                .eqIfPresent(MesProEdhrReleaseTransactionEventDO::getEventType, reqVO.getEventType())
                .orderByDesc(MesProEdhrReleaseTransactionEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrReleaseTransactionEventDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("eventType", text("CASE UPPER(TRIM(event_type)) WHEN 'PRECHECK' THEN '预检' WHEN 'SUBMIT' THEN '提交' WHEN 'APPROVE' THEN '批准' WHEN 'REJECT' THEN '驳回' WHEN 'WITHDRAW' THEN '撤回' ELSE '未知事件' END")),
                java.util.Map.entry("statusChange", text("CONCAT(CASE UPPER(TRIM(from_status)) WHEN 'PRECHECK_REQUIRED' THEN '待预检' WHEN 'PRECHECK_FAILED' THEN '预检失败' WHEN 'PRECHECK_PASSED' THEN '预检通过' WHEN 'PENDING_APPROVAL' THEN '待审批' WHEN 'RELEASED' THEN '已放行' WHEN 'REJECTED' THEN '已驳回' WHEN 'WITHDRAWN' THEN '已撤回' ELSE '未知状态' END,' ',CASE UPPER(TRIM(to_status)) WHEN 'PRECHECK_REQUIRED' THEN '待预检' WHEN 'PRECHECK_FAILED' THEN '预检失败' WHEN 'PRECHECK_PASSED' THEN '预检通过' WHEN 'PENDING_APPROVAL' THEN '待审批' WHEN 'RELEASED' THEN '已放行' WHEN 'REJECTED' THEN '已驳回' WHEN 'WITHDRAWN' THEN '已撤回' ELSE '未知状态' END)")),
                java.util.Map.entry("reasonOpinion", text("COALESCE(NULLIF(reason,''),NULLIF(opinion,''),'--')")),
                java.util.Map.entry("actorUserId", number("actor_user_id")),
                java.util.Map.entry("idempotencyKey", text("idempotency_key")),
                java.util.Map.entry("signoffEvidenceHash", text("signoff_evidence_hash")),
                java.util.Map.entry("occurredAt", dateTime("occurred_at")))));
    }
}
