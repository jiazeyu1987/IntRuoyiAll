package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImpl;
import cn.iocoder.yudao.module.dcc.service.file.DccExternalFileReviewServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import org.springframework.stereotype.Component;

@Component
public class DccSignatureGovernancePortalAdapter implements SignatureGovernancePortalAdapter {

    private final BpmTaskService bpmTaskService;
    private final DccControlledFileSignatureMapper signatureMapper;

    public DccSignatureGovernancePortalAdapter(BpmTaskService bpmTaskService,
                                               DccControlledFileSignatureMapper signatureMapper) {
        if (bpmTaskService == null || signatureMapper == null) {
            throw new IllegalArgumentException("DCC portal adapter requires BPM task service and signature mapper");
        }
        this.bpmTaskService = bpmTaskService;
        this.signatureMapper = signatureMapper;
    }

    @Override
    public SignatureGovernanceModuleCode getModuleCode() {
        return SignatureGovernanceModuleCode.DCC;
    }

    @Override
    public String getModuleName() {
        return "文件签名";
    }

    @Override
    public String getModuleDescription() {
        return "受控文件签名记录、统一审批中心待处理与授权管理";
    }

    @Override
    public String getPrimaryRouteLabel() {
        return "文件签名记录";
    }

    @Override
    public String getPrimaryRoute() {
        return "/signature-governance/file-signatures";
    }

    @Override
    public String getSecondaryRouteLabel() {
        return "用户授权";
    }

    @Override
    public String getSecondaryRoute() {
        return "/signature-governance/authorizations";
    }

    @Override
    public SignatureGovernancePortalMetrics describeMetrics(Long userId) {
        long pendingCount = todoCount(userId, DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY)
                + todoCount(userId, DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        long signatureCount = signatureMapper.selectCount(new LambdaQueryWrapperX<DccControlledFileSignatureDO>()
                .eq(DccControlledFileSignatureDO::getActorId, userId));
        return SignatureGovernancePortalMetrics.of(pendingCount, signatureCount);
    }

    private long todoCount(Long userId, String processDefinitionKey) {
        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);
        reqVO.setProcessDefinitionKey(processDefinitionKey);
        PageResult<?> page = bpmTaskService.getTaskTodoPage(userId, reqVO);
        if (page == null || page.getTotal() == null) {
            throw new IllegalStateException("DCC portal task count response is missing total for " + processDefinitionKey);
        }
        return page.getTotal();
    }
}
