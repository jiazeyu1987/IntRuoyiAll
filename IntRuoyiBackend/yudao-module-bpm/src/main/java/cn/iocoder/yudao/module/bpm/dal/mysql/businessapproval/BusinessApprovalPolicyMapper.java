package cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicy;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalPolicyMode;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import java.util.Map;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import java.util.List;
import java.util.Set;

@Mapper
public interface BusinessApprovalPolicyMapper extends BaseMapperX<BusinessApprovalPolicyDO> {

    Set<String> APPROVAL_SWITCH_EFFECT_EXECUTOR_CODES = Set.of(
            "DCC_UPLOAD",
            "DCC_PUBLISH",
            "DCC_OBSOLETE",
            "FORM_TEMPLATE_UPGRADE",
            "FORM_TEMPLATE_OBSOLETE",
            "MES_ROUTE_VERSION_PUBLISH",
            "MES_BATCH_RECORD_VERSION_PUBLISH",
            "MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART",
            "EDHR_BATCH_EXECUTION_SUBMIT_REVIEW",
            "EDHR_BATCH_VOID");

    String LATEST_POLICY_VERSION_ID_SQL = "SELECT MAX(latest.id) FROM bpm_business_approval_policy latest "
            + "WHERE latest.deleted = FALSE "
            + "GROUP BY latest.tenant_id, latest.data_domain, latest.system_code, latest.object_type, "
            + "latest.action_code, latest.object_state";

    default PageResult<BusinessApprovalPolicyDO> selectPage(BusinessApprovalPolicyPageReqVO reqVO) {
        QueryWrapperX<BusinessApprovalPolicyDO> queryWrapper = new QueryWrapperX<BusinessApprovalPolicyDO>()
                .eqIfPresent("tenant_id", reqVO.getTenantId())
                .eqIfPresent("data_domain", reqVO.getDataDomain())
                .eqIfPresent("system_code", reqVO.getSystemCode())
                .eqIfPresent("object_type", reqVO.getObjectType())
                .eqIfPresent("action_code", reqVO.getActionCode())
                .eqIfPresent("object_state", reqVO.getObjectState())
                .eqIfPresent("policy_mode", reqVO.getPolicyMode())
                .eqIfPresent("status", reqVO.getStatus());
        if (Boolean.TRUE.equals(reqVO.getApprovalSwitchScope())) {
            queryWrapper.in("effect_executor_code", APPROVAL_SWITCH_EFFECT_EXECUTOR_CODES);
        }
        if (reqVO.getStatus() == null) {
            queryWrapper.inSql("id", LATEST_POLICY_VERSION_ID_SQL);
        }
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, queryWrapper.orderByDesc("id"), Map.ofEntries(
                Map.entry("dataDomain", text("CASE UPPER(TRIM(data_domain)) WHEN 'MES' THEN '生产执行' WHEN 'FORM_CENTER' THEN '表单中心' WHEN 'DCC' THEN '文控中心' ELSE COALESCE(NULLIF(TRIM(data_domain), ''), '--') END")),
                Map.entry("systemCode", text("CASE UPPER(TRIM(system_code)) WHEN 'MES' THEN '生产执行' WHEN 'FORM_CENTER' THEN '表单中心' WHEN 'DCC' THEN '文控中心' ELSE COALESCE(NULLIF(TRIM(system_code), ''), '--') END")),
                Map.entry("objectType", text("CASE UPPER(TRIM(object_type)) WHEN 'ROUTE_VERSION' THEN '工艺路线版本' WHEN 'FORM_TEMPLATE' THEN '表单模板' WHEN 'CONTROLLED_FILE' THEN '受控文件' WHEN 'BATCH_RECORD_VERSION' THEN '批记录版本' WHEN 'EDHR_BATCH_EXECUTION' THEN '批次执行记录' ELSE COALESCE(NULLIF(TRIM(object_type), ''), '--') END")),
                Map.entry("actionCode", text("CASE UPPER(TRIM(action_code)) WHEN 'PUBLISH' THEN '发布' WHEN 'UPGRADE' THEN '升版' WHEN 'OBSOLETE' THEN '作废' WHEN 'VOID' THEN '作废' WHEN 'SUBMIT_REVIEW' THEN '提交审核' ELSE COALESCE(NULLIF(TRIM(action_code), ''), '--') END")),
                Map.entry("objectState", text("CASE UPPER(TRIM(object_state)) WHEN 'READY_TO_PUBLISH' THEN '待发布' WHEN 'READY' THEN '就绪' WHEN 'DRAFT' THEN '草稿' WHEN 'ACTIVE' THEN '已生效' WHEN 'PRECHECK_PASSED' THEN '预检通过' WHEN 'RELEASED' THEN '已放行' WHEN 'CLOSED' THEN '已关闭' WHEN 'ALL' THEN '所有' WHEN 'REJECTED' THEN '已驳回' WHEN 'PUBLISHED' THEN '已发布' WHEN 'DISABLED' THEN '已禁用' ELSE COALESCE(NULLIF(TRIM(object_state), ''), '--') END")),
                Map.entry("effectExecutorCode", text("CASE UPPER(TRIM(effect_executor_code)) WHEN 'MES_ROUTE_VERSION_PUBLISH' THEN '工艺路线版本发布' WHEN 'FORM_TEMPLATE_UPGRADE' THEN '表单模板升版' WHEN 'FORM_TEMPLATE_OBSOLETE' THEN '表单模板作废' WHEN 'DCC_UPLOAD' THEN '受控文件上传' WHEN 'DCC_PUBLISH' THEN '受控文件发布' WHEN 'DCC_OBSOLETE' THEN '受控文件作废' WHEN 'BATCH_RECORD_VERSION_PUBLISH' THEN '批记录版本发布' WHEN 'MES_BATCH_RECORD_VERSION_PUBLISH' THEN '批记录版本发布' WHEN 'EDHR_BATCH_EXECUTION_SUBMIT_REVIEW' THEN '批次执行提交审核' WHEN 'EDHR_BATCH_VOID' THEN '批次执行作废' ELSE COALESCE(NULLIF(TRIM(effect_executor_code), ''), '--') END")),
                Map.entry("policyMode", text("CASE policy_mode WHEN 'BPM_REQUIRED' THEN 'BPM审批' WHEN 'SIGNATURE_REQUIRED' THEN '历史签名模式' WHEN 'DIRECT' THEN '关闭审批' ELSE policy_mode END")),
                Map.entry("status", text("CASE status WHEN 'DRAFT' THEN '草稿' WHEN 'PUBLISHED' THEN '已发布' WHEN 'DISABLED' THEN '已停用' ELSE status END")),
                Map.entry("approvalSwitch", text("CASE WHEN status = 'DISABLED' THEN '已停用，请复制新增' ELSE '草稿发布后可切换' END")),
                Map.entry("updatedTime", dateTime("update_time"))
        )));
    }

    default List<BusinessApprovalPolicyDO> selectPublishedByAction(BusinessApprovalContext context) {
        return selectPublishedByAction(context.getTenantId(), context.getDataDomain(), context.getSystemCode(),
                context.getObjectType(), context.getActionCode(), context.getObjectState());
    }

    default List<BusinessApprovalPolicyDO> selectPublishedByAction(Long tenantId, String dataDomain, String systemCode,
            String objectType, String actionCode, String objectState) {
        QueryWrapperX<BusinessApprovalPolicyDO> queryWrapper = new QueryWrapperX<BusinessApprovalPolicyDO>()
                .eq("tenant_id", tenantId)
                .eq("data_domain", dataDomain)
                .eq("system_code", systemCode)
                .eq("object_type", objectType)
                .eq("action_code", actionCode)
                .eq("status", BusinessApprovalPolicy.STATUS_PUBLISHED);
        if (!BusinessApprovalPolicy.OBJECT_STATE_ALL.equals(objectState)) {
            queryWrapper.and(wrapper -> wrapper.eq("object_state", objectState)
                    .or()
                    .eq("object_state", BusinessApprovalPolicy.OBJECT_STATE_ALL));
        }
        return selectList(queryWrapper.orderByDesc("id"));
    }

    default BusinessApprovalPolicyDO selectLatestBpmRequiredWithProcessDefinitionKey(Long tenantId, String dataDomain,
            String systemCode, String objectType, String actionCode, String objectState) {
        QueryWrapperX<BusinessApprovalPolicyDO> queryWrapper = new QueryWrapperX<>();
        queryWrapper.eq("tenant_id", tenantId);
        queryWrapper.eq("data_domain", dataDomain);
        queryWrapper.eq("system_code", systemCode);
        queryWrapper.eq("object_type", objectType);
        queryWrapper.eq("action_code", actionCode);
        queryWrapper.eq("object_state", objectState);
        queryWrapper.eq("policy_mode", BusinessApprovalPolicyMode.BPM_REQUIRED.name());
        queryWrapper.isNotNull("process_definition_key");
        queryWrapper.ne("process_definition_key", "");
        queryWrapper.orderByDesc("id");
        queryWrapper.limitN(1);
        return selectOne(queryWrapper);
    }

}
