package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;

/** An explicit historical/application read cannot inherit a grant from another version or task. */
@Service
public class DccSelectedDetailReadService {
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileQueryService query;
    @Resource private DccControlledFileDetailAuthorizationGuard guard;
    @Resource private DccControlledFileApplicationEvidenceMapper rounds;
    @Resource private BpmTaskService tasks;
    @Resource private DccOfflineTrainingRecordService offlineTraining;

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public DccControlledFileRespVO read(Long actor, Long id, DccControlledFileBodyReadContext context) {
        return authorizedProjection(actor, requireFile(id), context);
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public DccControlledFileRespVO readApplication(Long actor, Long id, String bpm, String task, String type) {
        var file=requireFile(id);
        if (bpm==null || bpm.isBlank() || !bpm.equals(bpm.strip()))
            throw new IllegalArgumentException("精确详情申请轮次缺失");
        var bound=Objects.requireNonNull(rounds.selectApplicationRounds(file.getTenantId(),file.getDccProjectCodeId(),id),
                "formal detail application mappings").stream().filter(row -> Objects.equals(row.getBpmRound(),bpm)).toList();
        if (bound.size()!=1 || !Objects.equals(bound.get(0).getControlledFileId(),id)
                || type!=null && !Objects.equals(type,bound.get(0).getApplicationType()))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if (task==null && DccOfflineTrainingRecordService.isNativeWaitingFile(file)
                && Objects.equals(file.getProcessInstanceId(),bpm) && offlineTraining.canReadProgress(actor,file)) {
            // The receive-task entry grants metadata only; explicit body reads keep their own guard.
            var result=query.getControlledFile(actor,id);
            if (result!=null && !Objects.equals(result.getProcessInstanceId(),bpm))
                throw new IllegalStateException("详情投影与所选精确申请轮次不一致");
            return selectedProjection(file,result,bpm);
        }
        var intent=task!=null && tasks.getTask(task)!=null ? DccControlledFileBodyReadContext.Intent.APPROVAL_TASK
                : DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY;
        var context=new DccControlledFileBodyReadContext(intent,id.toString(),file.getVersionNo(),
                bound.get(0).getApplicationType(),bpm,task);
        return authorizedProjection(actor,file,context);
    }

    private DccControlledFileDO requireFile(Long id) {
        var file=files.selectById(id);
        if (file==null || !Objects.equals(file.getId(),id) || Boolean.TRUE.equals(file.getDeleted())
                || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        return file;
    }
    private DccControlledFileRespVO authorizedProjection(Long actor,DccControlledFileDO file,DccControlledFileBodyReadContext context) {
        Objects.requireNonNull(context,"explicit detail context").requireFile(file.getId(),file.getVersionNo());
        guard.requireBodyRead(actor,file,context);
        var result=query.getControlledFile(actor,file.getId(),context);
        return selectedProjection(file,result,context.bpmRound());
    }
    private DccControlledFileRespVO selectedProjection(DccControlledFileDO file,DccControlledFileRespVO result,String bpm) {
        if (result==null || !Objects.equals(result.getId(),file.getId()) || !Objects.equals(result.getVersionNo(),file.getVersionNo()))
            throw new IllegalStateException("详情投影与所选精确版本不一致");
        if (bpm!=null && result.getSignatureSummaries()!=null)
            result.setSignatureSummaries(result.getSignatureSummaries().stream()
                    .filter(row -> Objects.equals(row.getProcessInstanceId(),bpm)).toList());
        return result;
    }
}
