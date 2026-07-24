package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTaskPermissionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FormTaskPermissionMapper extends BaseMapperX<FormTaskPermissionDO> {

    default List<FormTaskPermissionDO> selectActiveByTaskId(Long tenantId, Long instanceId, String taskId) {
        return selectList(new QueryWrapperX<FormTaskPermissionDO>()
                .eq("tenant_id", tenantId)
                .eq("instance_id", instanceId)
                .eq("task_id", taskId)
                .eq("status", FormTaskPermissionDO.STATUS_ACTIVE));
    }

    default List<FormTaskPermissionDO> selectActiveByProcessInstanceId(Long tenantId, String processInstanceId) {
        return selectList(new QueryWrapperX<FormTaskPermissionDO>()
                .eq("tenant_id", tenantId)
                .eq("bpm_process_instance_id", processInstanceId)
                .eq("status", FormTaskPermissionDO.STATUS_ACTIVE));
    }

    default FormTaskPermissionDO selectByTaskIdAndUserId(Long tenantId, Long instanceId, String taskId, Long userId) {
        return selectOne(new QueryWrapperX<FormTaskPermissionDO>()
                .eq("tenant_id", tenantId)
                .eq("instance_id", instanceId)
                .eq("task_id", taskId)
                .eq("user_id", userId)
                .last("LIMIT 1"));
    }

}
