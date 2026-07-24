package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormTaskPermissionCode;

import java.util.List;
import java.util.Set;

public class FormDerivedPermissionService {

    public void grantForActiveTask(FormActionInstance instance, String taskId, List<Long> userIds,
            Set<FormTaskPermissionCode> permissionCodes) {
        for (Long userId : userIds) {
            instance.grantTaskPermissions(taskId, userId, permissionCodes);
        }
    }

    public void revokeForTask(FormActionInstance instance, String taskId) {
        instance.revokeTaskPermissions(taskId);
    }

}
