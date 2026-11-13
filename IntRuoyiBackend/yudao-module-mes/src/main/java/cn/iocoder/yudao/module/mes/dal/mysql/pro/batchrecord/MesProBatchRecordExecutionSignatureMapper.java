package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface MesProBatchRecordExecutionSignatureMapper extends BaseMapperX<MesProBatchRecordExecutionSignatureDO> {

    default List<MesProBatchRecordExecutionSignatureDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionId)
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getSignedAt));
    }

    default List<MesProBatchRecordExecutionSignatureDO> selectListByReviewSource(String reviewSourceType,
                                                                                   Long reviewSourceId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getReviewSourceType, reviewSourceType)
                .eq(MesProBatchRecordExecutionSignatureDO::getReviewSourceId, reviewSourceId)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getSignedAt)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getId));
    }

    default List<MesProBatchRecordExecutionSignatureDO> selectTimelineListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getSignedAt)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getId));
    }

    default List<MesProBatchRecordExecutionSignatureDO> selectResponsibilityListByIds(
            Collection<Long> signatureIds) {
        if (signatureIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .in(MesProBatchRecordExecutionSignatureDO::getId, signatureIds)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getId));
    }

    default PageResult<MesProBatchRecordExecutionSignatureDO> selectPage(MesProBatchRecordExecutionSignaturePageReqVO reqVO) {
        return selectPage(reqVO, (Collection<Long>) null);
    }

    default List<Long> selectExecutionIdsByActorName(String actorName) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                        .likeIfPresent(MesProBatchRecordExecutionSignatureDO::getActorName, actorName))
                .stream()
                .map(MesProBatchRecordExecutionSignatureDO::getExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    default PageResult<MesProBatchRecordExecutionSignatureDO> selectPage(MesProBatchRecordExecutionSignaturePageReqVO reqVO,
                                                                         Collection<Long> executionIds) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getExecutionId, reqVO.getExecutionId())
                .inIfPresent(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionIds)
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getActionType, reqVO.getActionType())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getActorId, reqVO.getActorId())
                .likeIfPresent(MesProBatchRecordExecutionSignatureDO::getActorName, reqVO.getActorName())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getProcessInstanceId, reqVO.getProcessInstanceId())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getBpmTaskId, reqVO.getBpmTaskId())
                .betweenIfPresent(MesProBatchRecordExecutionSignatureDO::getSignedAt,
                        reqVO.getSignedAtStart() == null ? reqVO.getBeginSignedAt() : reqVO.getSignedAtStart(),
                        reqVO.getSignedAtEnd() == null ? reqVO.getEndSignedAt() : reqVO.getSignedAtEnd())
                .apply(cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getExecutionCode()), "EXISTS (SELECT 1 FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_signature.execution_id AND e.tenant_id = mes_pro_batch_record_execution_signature.tenant_id AND e.deleted = 0 AND e.execution_code LIKE {0})", "%" + reqVO.getExecutionCode() + "%")
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getSignedAt)
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("execution", text("COALESCE((SELECT e.execution_code FROM mes_pro_batch_record_execution e WHERE e.id = mes_pro_batch_record_execution_signature.execution_id AND e.deleted = 0 AND e.tenant_id = mes_pro_batch_record_execution_signature.tenant_id), CONCAT('执行 #', execution_id))")),
                java.util.Map.entry("actionType", text("CASE action_type WHEN 'FIELD_CHANGE' THEN '字段变更' WHEN 'FORM_REVIEW' THEN '表单复核' WHEN 'SUBMIT' THEN '提交审批' WHEN 'REVIEW_APPROVE' THEN '审核签名' WHEN 'APPROVE' THEN '最终批准' WHEN 'REJECT' THEN '审批驳回' WHEN 'ARCHIVE_SEAL' THEN '归档封存' ELSE COALESCE(action_type, '') END")),
                java.util.Map.entry("meaningText", text("CASE action_type WHEN 'FIELD_CHANGE' THEN '字段变更' WHEN 'FORM_REVIEW' THEN '表单复核' WHEN 'SUBMIT' THEN '提交审批' WHEN 'REVIEW_APPROVE' THEN '审核签名' WHEN 'APPROVE' THEN '最终批准' WHEN 'REJECT' THEN '审批驳回' WHEN 'ARCHIVE_SEAL' THEN '归档封存' ELSE COALESCE(action_type, '') END")),
                java.util.Map.entry("actorName", text("COALESCE(NULLIF(actor_name,''), '--')")),
                java.util.Map.entry("actorUsernameSnapshot", text("COALESCE(NULLIF(actor_username_snapshot,''), '--')")),
                java.util.Map.entry("actorDeptPost", text("CONCAT(COALESCE(NULLIF(actor_dept_name_snapshot,''), '--'), ' ', COALESCE(NULLIF(actor_post_names_snapshot,''), '--'))")),
                java.util.Map.entry("actorRoleNamesSnapshot", text("COALESCE(NULLIF(actor_role_names_snapshot,''), '--')")),
                java.util.Map.entry("signaturePurpose", text("COALESCE(NULLIF(signature_purpose,''), CASE action_type WHEN 'FIELD_CHANGE' THEN '字段变更' WHEN 'FORM_REVIEW' THEN '表单复核' WHEN 'SUBMIT' THEN '提交审批' WHEN 'REVIEW_APPROVE' THEN '审核签名' WHEN 'APPROVE' THEN '最终批准' WHEN 'REJECT' THEN '审批驳回' WHEN 'ARCHIVE_SEAL' THEN '归档封存' ELSE COALESCE(action_type, '') END)")),
                java.util.Map.entry("signatureConfirm", text("CONCAT(CASE signature_mode WHEN 'PASSWORD' THEN '密码签名' ELSE COALESCE(signature_mode,'') END, ' ', CASE WHEN password_verified THEN '通过' ELSE '失败' END)")),
                java.util.Map.entry("comment", text("COALESCE(NULLIF(reason,''), NULLIF(comment,''), '--')")),
                java.util.Map.entry("signedAt", dateTime("signed_at")))));
    }
}
