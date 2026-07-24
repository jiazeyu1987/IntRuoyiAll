package cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequestStatus;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BusinessApprovalRequestMapper extends BaseMapperX<BusinessApprovalRequestDO> {

    default BusinessApprovalRequestDO selectPendingByBusinessAction(BusinessApprovalContext context) {
        return selectOne(new QueryWrapperX<BusinessApprovalRequestDO>()
                .eq("tenant_id", context.getTenantId())
                .eq("data_domain", context.getDataDomain())
                .eq("system_code", context.getSystemCode())
                .eq("object_type", context.getObjectType())
                .eq("object_id", context.getObjectId())
                .eq("object_version", context.getObjectVersion())
                .eq("action_code", context.getActionCode())
                .eq("request_status", BusinessApprovalRequestStatus.PENDING_BPM.name())
                .last("LIMIT 1"));
    }

    default BusinessApprovalRequestDO selectByProcessInstanceId(String processInstanceId) {
        return selectOne(new QueryWrapperX<BusinessApprovalRequestDO>()
                .eq("process_instance_id", processInstanceId)
                .last("LIMIT 1"));
    }

}
