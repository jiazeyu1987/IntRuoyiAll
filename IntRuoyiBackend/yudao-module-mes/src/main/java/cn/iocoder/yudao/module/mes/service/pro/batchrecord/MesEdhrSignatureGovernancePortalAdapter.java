package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAdapter;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalMetrics;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import org.springframework.stereotype.Component;

@Component
public class MesEdhrSignatureGovernancePortalAdapter implements SignatureGovernancePortalAdapter {

    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProBatchRecordExecutionSignatureMapper signatureMapper;

    public MesEdhrSignatureGovernancePortalAdapter(MesProEdhrWorkTaskMapper workTaskMapper,
                                                   MesProBatchRecordExecutionSignatureMapper signatureMapper) {
        if (workTaskMapper == null || signatureMapper == null) {
            throw new IllegalArgumentException("eDHR portal adapter requires work task and signature mappers");
        }
        this.workTaskMapper = workTaskMapper;
        this.signatureMapper = signatureMapper;
    }

    @Override
    public SignatureGovernanceModuleCode getModuleCode() {
        return SignatureGovernanceModuleCode.EDHR;
    }

    @Override
    public String getModuleName() {
        return "批记录签名";
    }

    @Override
    public String getModuleDescription() {
        return "批记录签名记录、审核工作任务与授权管理";
    }

    @Override
    public String getPrimaryRouteLabel() {
        return "批记录签名记录";
    }

    @Override
    public String getPrimaryRoute() {
        return "/signature-governance/batch-signatures";
    }

    @Override
    public String getSecondaryRouteLabel() {
        return "工作任务";
    }

    @Override
    public String getSecondaryRoute() {
        return "/mes/pro/feedback/edhr-work-task";
    }

    @Override
    public SignatureGovernancePortalMetrics describeMetrics(Long userId) {
        Long assignedTodoCount = workTaskMapper.countMy(userId, null, MesProEdhrWorkTaskStatus.TODO);
        if (assignedTodoCount == null) {
            throw new IllegalStateException("eDHR portal assigned task count is missing");
        }
        Long candidateTodoCount = candidateTodoCount(userId);
        Long signatureCount = signatureMapper.selectCount(
                new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                        .eq(MesProBatchRecordExecutionSignatureDO::getActorId, userId));
        if (signatureCount == null) {
            throw new IllegalStateException("eDHR portal signature count is missing");
        }
        return SignatureGovernancePortalMetrics.of(assignedTodoCount + candidateTodoCount, signatureCount);
    }

    private Long candidateTodoCount(Long userId) {
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);
        PageResult<?> page = workTaskMapper.selectCandidateTodoPage(reqVO, userId, MesProEdhrWorkTaskStatus.TODO);
        if (page == null || page.getTotal() == null) {
            throw new IllegalStateException("eDHR portal candidate task count is missing");
        }
        return page.getTotal();
    }
}
