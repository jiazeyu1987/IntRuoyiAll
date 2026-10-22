package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DccDistributionDispatchAttemptService {
    @Resource private DccDistributionBatchMapper batchMapper;
    @Resource private DccDistributionDispatchAttemptMapper attemptMapper;
    @Resource private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private GxpAuditService gxpAuditService;
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    @GxpWriteOperation(operationId="dcc.distribution.attempt.start")
    public DccDistributionDispatchAttemptDO startAttempt(Long batchId,Long jobId) {
        requireTransaction();Long tenant=TenantContextHolder.getRequiredTenantId();
        var batch=batchMapper.lock(tenant,batchId);var job=messageJobMapper.selectByIdAndTenantForUpdate(tenant,jobId);
        if(batch==null || job==null || !Objects.equals(job.getDistributionBatchId(),batchId) || !"DISTRIBUTION".equals(job.getBusinessType()))
            throw new IllegalStateException("发送尝试批次/通知身份或状态不允许");
        if("SENT".equals(job.getStatus()))return null;
        if(!Set.of("DISPATCH_READY","FAILED").contains(batch.getStatus()) || !Set.of("PENDING","FAILED").contains(job.getStatus()))
            throw new IllegalStateException("发送尝试批次/通知状态不允许");
        var existing=attemptMapper.lockJob(tenant,jobId);if(existing==null)throw new IllegalStateException("发送尝试历史读取失败");
        int last=existing.stream().mapToInt(DccDistributionDispatchAttemptDO::getAttemptNo).max().orElse(0);
        if(last==Integer.MAX_VALUE)throw new IllegalStateException("发送尝试编号已达到INT容量，不允许溢出");
        int number=last+1;
        var now=LocalDateTime.now();var row=DccDistributionDispatchAttemptDO.builder().tenantId(tenant).batchId(batchId).messageJobId(jobId).attemptNo(number)
                .businessKey("DCC_MESSAGE_JOB:"+tenant+":"+jobId).recipientUserId(job.getRecipientUserId()).templateCode(job.getTemplateCode()).status("STARTED").startedAt(now).build();
        row.setCreateTime(now);row.setUpdateTime(now);
        if(attemptMapper.insert(row)!=1 || row.getId()==null)throw new IllegalStateException("发送尝试开始事实未持久化");
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.attempt.start",row.getId(),(long)number,"NO_ATTEMPT","STARTED",Map.of(),row,
                "按原通知业务键开始真实发送尝试",row.getBusinessKey()+":START:"+number);
        return row;
    }
    public void lockBatch(Long batchId) {requireTransaction();if(batchMapper.lock(TenantContextHolder.getRequiredTenantId(),batchId)==null)throw new IllegalStateException("发送批次锁定失败");}
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    @GxpWriteOperation(operationId="dcc.distribution.attempt.finish")
    public void finishAttempt(Long batchId,Long attemptId,boolean sent,Long platformMessageId,RuntimeException failure) {
        requireTransaction();Long tenant=TenantContextHolder.getRequiredTenantId();
        var batch=batchMapper.lock(tenant,batchId);var attempt=attemptMapper.lock(tenant,attemptId);
        if(batch==null || attempt==null || !Objects.equals(attempt.getBatchId(),batchId))throw new IllegalStateException("发送尝试终结身份不匹配");
        if(!"STARTED".equals(attempt.getStatus())) {
            String expected=sent?"SENT":"FAILED";
            String error=sent?"":DccDistributionAuditSupport.failure(Objects.requireNonNull(failure,"实际异常不能为空"));
            if(!Objects.equals(attempt.getStatus(),expected) || !Objects.equals(attempt.getPlatformMessageId(),platformMessageId)
                    || !Objects.equals(attempt.getErrorMessage(),error))throw new IllegalArgumentException("已终结发送尝试重放载荷冲突");
            return;
        }
        var before=JsonUtils.toJsonString(DccDistributionAuditSupport.snapshot(attempt));
        if(sent && (platformMessageId==null || platformMessageId<=0))throw new IllegalStateException("发送成功缺平台消息身份");
        attempt.setStatus(sent?"SENT":"FAILED");attempt.setPlatformMessageId(platformMessageId);attempt.setFinishedAt(LocalDateTime.now());
        attempt.setErrorCode(sent?"":"MESSAGE_DELIVERY_FAILED");attempt.setErrorMessage(sent?"":DccDistributionAuditSupport.failure(Objects.requireNonNull(failure,"真实发送失败原因不能为空")));
        if(attemptMapper.updateById(attempt)!=1)throw new IllegalStateException("发送尝试终结事实未持久化");
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.attempt.finish",attemptId,(long)attempt.getAttemptNo(),"STARTED",attempt.getStatus(),
                JsonUtils.parseObject(before,Map.class),attempt,sent?"原平台幂等消息发送已确认":"本次发送尝试失败，完整原因保存在尝试记录",attempt.getBusinessKey()+":FINISH:"+attempt.getAttemptNo());
        aggregate(batch);
    }
    private void aggregate(DccDistributionBatchDO batch) {
        var jobs=messageJobMapper.selectBatchJobsForUpdate(batch.getTenantId(),batch.getId());
        if(jobs==null || jobs.isEmpty())throw new IllegalStateException("发送批次没有正式通知任务");
        String before=JsonUtils.toJsonString(DccDistributionAuditSupport.snapshot(batch));String old=batch.getStatus();
        if(jobs.stream().allMatch(job->"SENT".equals(job.getStatus()) && job.getSentAt()!=null)) {
            batch.setStatus("SENT");batch.setSentAt(jobs.stream().map(DccControlledFileMessageJobDO::getSentAt).max(LocalDateTime::compareTo).orElseThrow());batch.setErrorCode("");batch.setErrorMessage("");
        } else if(jobs.stream().anyMatch(job->"FAILED".equals(job.getStatus()))) {
            batch.setStatus("FAILED");batch.setErrorCode("RECIPIENT_DELIVERY_FAILED");batch.setErrorMessage("部分通知真实发送失败，完整原因见逐人消息和发送尝试");batch.setFailureAt(LocalDateTime.now());
        } else batch.setStatus("DISPATCH_READY");
        if(Objects.equals(old,batch.getStatus()))return;
        batch.setRowVersion(batch.getRowVersion()+1);
        if(batchMapper.updateById(batch)!=1)throw new IllegalStateException("发送批次汇总事实未持久化");
        DccDistributionAuditSupport.append(gxpAuditService,"dcc.distribution.attempt.finish",batch.getId(),batch.getRowVersion(),old,batch.getStatus(),
                JsonUtils.parseObject(before,Map.class),batch,"按当前锁定逐人发送事实汇总批次",batch.getEventKey()+":AGGREGATE:"+batch.getRowVersion());
    }
    private void requireTransaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("发送尝试和审计必须在正式事务中");}
}
