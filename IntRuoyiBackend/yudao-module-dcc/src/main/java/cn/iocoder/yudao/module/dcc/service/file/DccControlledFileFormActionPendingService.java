package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormInstanceStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED;

@Service
public class DccControlledFileFormActionPendingService {

    static final String SYSTEM_CODE = "DCC";
    static final String OBJECT_TYPE_CONTROLLED_FILE = "CONTROLLED_FILE";
    static final String ACTION_OBSOLETE = "OBSOLETE";

    private static final List<String> OPEN_FORM_ACTION_STATUSES = List.of(
            FormInstanceStatus.IN_APPROVAL.name(),
            FormInstanceStatus.REWORKING.name(),
            FormInstanceStatus.PENDING_EFFECT.name(),
            FormInstanceStatus.EFFECT_FAILED_PENDING.name());

    @Resource
    private FormActionInstanceMapper formActionInstanceMapper;

    public boolean hasOpenObsoleteAction(Long controlledFileId) {
        return findOpenObsoleteAction(controlledFileId) != null;
    }

    public FormActionInstanceDO findOpenObsoleteAction(Long controlledFileId) {
        if (controlledFileId == null) {
            return null;
        }
        List<FormActionInstanceDO> instances = formActionInstanceMapper.selectByBusinessActionAndStatuses(
                TenantContextHolder.getRequiredTenantId(), SYSTEM_CODE, OBJECT_TYPE_CONTROLLED_FILE,
                String.valueOf(controlledFileId), ACTION_OBSOLETE, OPEN_FORM_ACTION_STATUSES);
        return instances.isEmpty() ? null : instances.get(0);
    }

    public void requireNoOpenObsoleteAction(Long controlledFileId) {
        if (hasOpenObsoleteAction(controlledFileId)) {
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        }
    }

}
