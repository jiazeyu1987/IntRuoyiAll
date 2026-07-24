package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACTION_LOCKED;

@Service
public class DccControlledFilePendingActionGuard {

    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;

    public void assertNoPendingBusinessAction(DccControlledFileDO file) {
        if (file == null || file.getId() == null) {
            throw new IllegalArgumentException("DCC controlled file identity is required for pending action guard");
        }
        FormInstanceRespVO active = formCenterRuntimeService.findActiveBusinessAction(buildContext(file));
        if (active != null) {
            throw exception(CONTROLLED_FILE_ACTION_LOCKED, describe(active));
        }
    }

    private BusinessActionContextReqVO buildContext(DccControlledFileDO file) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId(String.valueOf(file.getId()));
        context.setObjectVersion(file.getVersionNo());
        context.setObjectState(file.getStatus());
        return context;
    }

    private String describe(FormInstanceRespVO active) {
        String actionCode = active.getContext() == null ? null : active.getContext().getActionCode();
        if (actionCode == null || actionCode.isBlank()) {
            actionCode = active.getStatus();
        }
        return actionCode + " / " + active.getInstanceCode();
    }
}
