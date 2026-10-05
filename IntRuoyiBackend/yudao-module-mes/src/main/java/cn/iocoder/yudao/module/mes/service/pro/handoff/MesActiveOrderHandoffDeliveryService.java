package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationPlatformSender;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

@Service
@Slf4j
public class MesActiveOrderHandoffDeliveryService {
    @Resource private MesActiveOrderHandoffDeliveryMapper mapper;
    @Resource private MesActiveOrderHandoffTaskMapper tasks;
    @Resource private MesActiveOrderHandoffDeliveryTransactionService transactions;
    @Resource private MesReleaseTaskNotificationPlatformSender platformSender;
    @Resource private MesActiveOrderHandoffAudit audit;

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void schedule(MesActiveOrderHandoffTaskDO task) {
        require(TransactionSynchronizationManager.isSynchronizationActive(),"通知意图必须加入正式交接事务");
        validate(task); audit.lock(); Long currentTenant=tenant();
        String params=JsonUtils.toJsonString(Map.of("activeOrderId",task.getActiveOrderId().toString(),
                "taskName",label(task.getTaskType()),"actionUrl",task.getActionUrl(),"reason",task.getReason(),"handoffTaskId",task.getId().toString(),"handoffType",task.getTaskType()));
        var recipients=candidates(task.getCandidateUserSnapshot());
        var existing=mapper.forTask(currentTenant,task.getId());
        if(!existing.isEmpty()) {
            require(existing.size()==recipients.size()&&existing.stream().allMatch(row->recipients.contains(row.getUserId())
                    &&Objects.equals(params,row.getTemplateParamsJson())),"既有通知冻结参数或收件人冲突");
            return;
        }
        for(Long user:recipients) {
            var row=new MesActiveOrderHandoffDeliveryDO().setHandoffTaskId(task.getId()).setUserId(user)
                    .setBusinessKey("MES_ACTIVE_HANDOFF:"+currentTenant+":"+task.getActiveOrderId()+":"+task.getId()+":USER:"+user)
                    .setTemplateParamsJson(params).setStatus("PENDING").setAttemptCount(0).setRowVersion(0);
            row.setTenantId(currentTenant);
            require(mapper.insert(row)==1&&row.getId()!=null,"交接通知意图未完整持久化");
            audit.append("delivery-created",MesActiveOrderHandoffDeliveryService.class.getName()+"#schedule","MES_HANDOFF_DELIVERY",row.getId(),0,null,row,"正式交接冻结收件人");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                Long previous=TenantContextHolder.getTenantId();
                try { TenantContextHolder.setTenantId(currentTenant); dispatchPending(task.getId()); }
                catch(RuntimeException failure) { log.error("Committed handoff notification incomplete: tenant={},task={},failure={}",currentTenant,task.getId(),errorSummary(failure)); }
                finally { if(previous==null)TenantContextHolder.clear();else TenantContextHolder.setTenantId(previous); }
            }
        });
    }
    public List<MesActiveOrderHandoffDeliveryDO> ownReceipts(Long taskId,Long actor) {
        access(tasks.selectById(taskId),actor);
        return mapper.forTask(tenant(),taskId).stream().filter(row->Objects.equals(actor,row.getUserId())).toList();
    }
    public void retry(Long id,Integer version,Long actor,String reason) {
        require(reason!=null&&!reason.isBlank()&&reason.length()<=1000,"请填写通知重试原因");
        var row=mapper.exact(tenant(),id); require(row!=null,"通知投递不存在");var task=tasks.selectById(row.getHandoffTaskId());access(task,actor);
        require(!"CANCELED".equals(task.getStatus()),"旧周期交接已取消，禁止重试办理通知");
        require(Objects.equals(actor,row.getUserId()),"仅原收件人可以重试本人通知");
        require(Objects.equals(version,row.getRowVersion())&&Set.of("PENDING","FAILED").contains(row.getStatus()),"通知已发送或版本变化，请刷新");
        dispatch(row,reason.trim());
    }
    private void dispatchPending(Long taskId) {
        for(var row:mapper.forTask(tenant(),taskId)) {
            if(!"PENDING".equals(row.getStatus()))continue;
            try { dispatch(row,"正式业务提交后自动投递交接通知"); }
            catch(RuntimeException failure) { log.error("Handoff delivery remains recoverable: tenant={},task={},delivery={},failure={}",tenant(),taskId,row.getId(),errorSummary(failure)); }
        }
    }
    private void dispatch(MesActiveOrderHandoffDeliveryDO row,String reason) {
        var attempt=transactions.recordAttempt(row.getId(),row.getRowVersion(),reason);
        try {
            var request=new NotifySendSingleToUserIdempotentReqDTO();request.setUserId(attempt.getUserId());request.setTemplateCode(TEMPLATE);request.setBusinessKey(attempt.getBusinessKey());
            Map<String,Object> params=JsonUtils.parseObject(attempt.getTemplateParamsJson(),Map.class);
            require(params!=null&&!params.isEmpty()&&params.values().stream().allMatch(String.class::isInstance),"交接通知冻结参数无效");
            request.setTemplateParams(params);Long message=platformSender.send(request);
            transactions.recordSent(attempt.getId(),attempt.getRowVersion(),message,reason);
        } catch(RuntimeException failure) {
            try { transactions.recordFailed(attempt.getId(),attempt.getRowVersion(),errorSummary(failure),reason); }
            catch(RuntimeException ack) { log.error("Handoff failure ACK rejected: delivery={},failure={}",attempt.getId(),errorSummary(ack));failure.addSuppressed(ack); }
            throw failure("通知投递未完成，请刷新回执后重试（"+errorSummary(failure)+"）");
        }
    }
}
