package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface FormActionInstanceMapper extends BaseMapperX<FormActionInstanceDO> {

    default List<FormActionInstanceDO> selectSameBusinessAction(Long tenantId, String systemCode, String objectType,
            String objectId, String objectVersion, String actionCode) {
        return selectList(new QueryWrapperX<FormActionInstanceDO>()
                .eq("tenant_id", tenantId)
                .eq("system_code", systemCode)
                .eq("object_type", objectType)
                .eq("object_id", objectId)
                .eq("object_version", objectVersion)
                .eq("action_code", actionCode)
                .orderByDesc("id"));
    }

    default FormActionInstanceDO selectByProcessInstanceId(Long tenantId, String processInstanceId) {
        return selectOne(new QueryWrapperX<FormActionInstanceDO>()
                .eq("tenant_id", tenantId)
                .eq("bpm_process_instance_id", processInstanceId)
                .last("LIMIT 1"));
    }

    default List<FormActionInstanceDO> selectByBusinessActionAndStatuses(Long tenantId, String systemCode,
            String objectType, String objectId, String actionCode, Collection<String> statuses) {
        return selectList(new QueryWrapperX<FormActionInstanceDO>()
                .eq("tenant_id", tenantId)
                .eq("system_code", systemCode)
                .eq("object_type", objectType)
                .eq("object_id", objectId)
                .eq("action_code", actionCode)
                .in("status", statuses)
                .orderByDesc("id"));
    }

    default FormActionInstanceDO selectActiveByBusinessObject(Long tenantId, String systemCode, String objectType,
            String objectId) {
        return selectOne(new QueryWrapperX<FormActionInstanceDO>()
                .eq("tenant_id", tenantId)
                .eq("system_code", systemCode)
                .eq("object_type", objectType)
                .eq("object_id", objectId)
                .in("status", FormInstanceStatus.IN_APPROVAL.name(), FormInstanceStatus.REWORKING.name())
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

}
