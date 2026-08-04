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

import java.util.List;

@Mapper
public interface BusinessApprovalPolicyMapper extends BaseMapperX<BusinessApprovalPolicyDO> {

    String EDHR_ROUTE_FORM_OBJECT_TYPE = "EDHR_ROUTE_FORM";
    String MES_EDHR_ROUTE_FORM_FILL_EXECUTOR = "MES_EDHR_ROUTE_FORM_FILL";

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
            queryWrapper.ne("object_type", EDHR_ROUTE_FORM_OBJECT_TYPE)
                    .and(wrapper -> wrapper.isNull("effect_executor_code")
                            .or()
                            .ne("effect_executor_code", MES_EDHR_ROUTE_FORM_FILL_EXECUTOR));
        }
        if (reqVO.getStatus() == null) {
            queryWrapper.inSql("id", LATEST_POLICY_VERSION_ID_SQL);
        }
        return selectPage(reqVO, queryWrapper.orderByDesc("id"));
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
