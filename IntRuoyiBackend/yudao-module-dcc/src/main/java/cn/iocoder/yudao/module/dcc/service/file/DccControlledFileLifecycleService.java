package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Master -> version lock order is shared by finalization and scheduled activation. */
@Service
public class DccControlledFileLifecycleService {
    /** Official GxP SYSTEM_ACTOR identity, not a human account or electronic signer. */
    private static final Long SYSTEM_ACTOR_ID = -1L;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource private ApplicationEventPublisher eventPublisher;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource private DccControlledFileVersionPolicy versionPolicy;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private DccObsoleteRetentionService obsoleteRetentionService;
    @Resource private DccWorkflowFileStateAudit fileStateAudit;

    public LocalDateTime currentTime() { return datePolicy.now(); }

    public DccWorkflowDatePolicy.ReminderDates distributionReminderDates() { return datePolicy.reminderDates(); }

    void completeControl(DccControlledFileDO file, DccControlledFileMasterDO master, Long actorId) {
        requireTransaction();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (file == null || master == null || !Objects.equals(file.getMasterId(),master.getId())
                || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(master.getTenantId(),tenant)
                || file.getProcessInstanceId() == null || file.getProcessInstanceId().isBlank())
            throw new IllegalArgumentException("受控文件、轮次及租户身份不匹配");
        if (file.getControlledTime() != null) return;
        datePolicy.requireReviewDate(file.getEffectiveDate());
        LocalDateTime now = datePolicy.now();
        if (master.getLatestControlledFileId() != null && !master.getLatestControlledFileId().equals(file.getId())) {
            var latest = controlledFileMapper.selectByIdAndTenantForUpdate(tenant,master.getLatestControlledFileId());
            if (latest == null || latest.getControlledTime() == null
                    || !Objects.equals(latest.getMasterId(),master.getId()))
                throw new IllegalStateException("最新受控定位无正式受控事实");
            var candidate = versionPolicy.parseStored(file);
            var previous = versionPolicy.parseStored(latest);
            if(candidate==null || previous==null || candidate.compareTo(previous)<=0)
                throw new IllegalStateException("受控版本不得倒退");
        }
        if (file.getPublishedFileId() == null || file.getStampedFileId() == null)
            throw new IllegalStateException("受控文件未生成，不能完成受控");
        var beforeControl = fileStateAudit.capture(file, master);
        closeLowerWorkingIterations(file,master,tenant);
        DccControlledFileDO update = DccControlledFileDO.builder().id(file.getId())
                .controlledTime(now).publishedTime(now).status("CONTROLLED_PENDING_EFFECTIVE")
                .publishedFileId(file.getPublishedFileId()).stampedFileId(file.getStampedFileId())
                .stampedTime(file.getStampedTime()).finalizationError("").build();
        requireOne(controlledFileMapper.updateById(update));
        requireOne(masterMapper.updateById(DccControlledFileMasterDO.builder().id(master.getId())
                .latestControlledFileId(file.getId()).status("ACTIVE_CHAIN").build()));
        file.setControlledTime(now);
        file.setPublishedTime(now);
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");
        master.setLatestControlledFileId(file.getId());
        master.setStatus("ACTIVE_CHAIN");
        fileStateAudit.recordControl(beforeControl, file.getProcessInstanceId());
        emit("CONTROLLED", file, master.getCurrentActiveControlledFileId(), now);
        if (!file.getEffectiveDate().isAfter(now.toLocalDate())) activateLocked(file, master, now);
    }

    private void closeLowerWorkingIterations(DccControlledFileDO controlled,DccControlledFileMasterDO master,Long tenant) {
        var target=versionPolicy.parseStored(controlled);
        if(target==null) throw new IllegalStateException("受控候选缺少正式版本号");
        var chain=controlledFileMapper.selectListByMasterIdForUpdate(master.getId());
        if(chain==null) throw new IllegalStateException("受控版本链缺失");
        var lower=new java.util.ArrayList<DccControlledFileDO>();
        for(var row:chain) {
            if(row==null || !"WORKING".equals(row.getStatus()) || Objects.equals(row.getId(),controlled.getId())) continue;
            if(!Objects.equals(row.getTenantId(),tenant) || !Objects.equals(row.getMasterId(),master.getId()))
                throw new IllegalStateException("工作稿与受控链租户身份不匹配");
            var version=versionPolicy.parseStored(row);
            if(version==null) throw new IllegalStateException("工作稿版本号不合法："+row.getId());
            if(version.compareTo(target)<0) lower.add(row);
        }
        for(var row:lower) requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(row.getId())
                .status("SUPERSEDED").supersededByFileId(controlled.getId()).build()));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean activateDue(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO identity = controlledFileMapper.selectById(id);
        if (identity == null || !Objects.equals(tenantId, identity.getTenantId()))
            throw new IllegalArgumentException("生效文件不存在或租户不匹配");
        DccControlledFileMasterDO master = masterMapper.selectByIdForUpdate(identity.getMasterId());
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, id);
        if (master == null || file == null || !Objects.equals(master.getId(), file.getMasterId())
                || !Objects.equals(master.getTenantId(), tenantId))
            throw new IllegalStateException("生效文件与逻辑文件身份不匹配");
        if("OBSOLETE".equals(file.getStatus())) {
            if(file.getControlledTime()==null || file.getObsoletedTime()==null
                    || Objects.equals(master.getCurrentActiveControlledFileId(),file.getId()))
                throw new IllegalStateException("已作废版本的正式事实或执行定位异常");
            return false;
        }
        if (file.getActivatedTime() != null) return false;
        LocalDateTime now = datePolicy.now();
        if (file.getEffectiveDate() == null || file.getControlledTime() == null
                || !"CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()))
            throw new IllegalStateException("文件未受控或不在待生效状态");
        if (file.getEffectiveDate().isAfter(now.toLocalDate())) return false;
        return activateLocked(file, master, now);
    }

    private boolean activateLocked(DccControlledFileDO file, DccControlledFileMasterDO master, LocalDateTime now) {
        requireTransaction();
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var chain=Objects.requireNonNull(controlledFileMapper.selectListByMasterIdForUpdate(master.getId()),"locked controlled version chain");
        var candidate=versionPolicy.parseStored(file);
        if(candidate==null || candidate.isWorkingIteration() || file.getControlledTime()==null
                || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(file.getMasterId(),master.getId()))
            throw new IllegalStateException("待生效版本缺少正式受控身份");
        Long oldId=master.getCurrentActiveControlledFileId();
        var current=oldId==null?null:chain.stream().filter(row->Objects.equals(row.getId(),oldId)).findFirst()
                .orElseThrow(()->new IllegalStateException("当前执行定位未命中受控链"));
        if(current!=null && (!Objects.equals(current.getTenantId(),tenant) || !"ACTIVE".equals(current.getStatus())
                || current.getControlledTime()==null || current.getActivatedTime()==null))
            throw new IllegalStateException("当前执行版本身份或状态异常");
        if(current!=null && !Objects.equals(oldId,file.getId())) {
            var executing=versionPolicy.parseStored(current);
            if(executing==null || executing.isWorkingIteration())throw new IllegalStateException("当前执行版本号异常");
            if(candidate.compareTo(executing)<0) {
                // A pre-fix pending lower version is retired against the real current execution, never activated backwards.
                var retired=obsoleteLowerControlled(current,master,chain,now);
                for(var before:retired)fileStateAudit.recordAutomaticObsolete(before,current.getProcessInstanceId());
                return false;
            }
            if(candidate.compareTo(executing)==0)throw new IllegalStateException("待生效与执行版本不得重复正式版本号");
        }
        var beforeActivation=fileStateAudit.capture(file,master);
        var beforeObsolete=obsoleteLowerControlled(file,master,chain,now);
        requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId())
                .activatedTime(now).status("ACTIVE").build()));
        requireOne(masterMapper.updateById(DccControlledFileMasterDO.builder().id(master.getId())
                .currentActiveControlledFileId(file.getId()).status("ACTIVE_CHAIN").build()));
        file.setActivatedTime(now);file.setStatus("ACTIVE");master.setCurrentActiveControlledFileId(file.getId());master.setStatus("ACTIVE_CHAIN");
        for(var before:beforeObsolete)fileStateAudit.recordAutomaticObsolete(before,file.getProcessInstanceId());
        fileStateAudit.recordActivation(beforeActivation,file.getProcessInstanceId());
        emit("ACTIVATED",file,oldId,now);
        return true;
    }

    /** All rows are already locked beneath the same Master; dates/artifacts and original approval evidence stay untouched. */
    private List<DccWorkflowFileStateAudit.Snapshot> obsoleteLowerControlled(DccControlledFileDO successor,
            DccControlledFileMasterDO master,List<DccControlledFileDO> chain,LocalDateTime now) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var target=versionPolicy.parseStored(successor);
        if(target==null || target.isWorkingIteration() || successor.getProcessInstanceId()==null || successor.getProcessInstanceId().isBlank())
            throw new IllegalStateException("自动作废缺少真实后继受控版本/审批轮次");
        var lower=new java.util.ArrayList<DccControlledFileDO>();
        for(var row:chain) {
            if(row==null || !Objects.equals(row.getTenantId(),tenant) || !Objects.equals(row.getMasterId(),master.getId()))
                throw new IllegalStateException("受控版本链租户/逻辑身份异常");
            if(!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(row.getStatus()))continue;
            if("ACTIVE".equals(row.getStatus()) && !Objects.equals(row.getId(),master.getCurrentActiveControlledFileId()))
                throw new IllegalStateException("受控版本链有执行定位之外的ACTIVE版本");
            var version=versionPolicy.parseStored(row);
            if(row.getControlledTime()==null || version==null || version.isWorkingIteration())
                throw new IllegalStateException("受控版本链缺少正式受控事实");
            if(version.compareTo(target)<0)lower.add(row);
        }
        var before=new java.util.ArrayList<DccWorkflowFileStateAudit.Snapshot>();
        for(var row:lower)before.add(fileStateAudit.capture(row,master));
        for(var row:lower) {
            requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(row.getId())
                    .status("OBSOLETE").obsoletedTime(now).obsoletedBy(SYSTEM_ACTOR_ID).supersededByFileId(successor.getId())
                    .obsoleteReason("新版生效自动作废").build()));
            requireOne(obsoleteAuditMapper.insert(DccControlledFileObsoleteAuditDO.builder().controlledFileId(row.getId())
                    .operatorId(SYSTEM_ACTOR_ID).obsoleteReason("新版生效自动作废").statusBefore(row.getStatus()).statusAfter("OBSOLETE").build()));
        }
        if(!lower.isEmpty())obsoleteRetentionService.retain(tenant,master.getId(),now);
        return before;
    }

    public List<DccControlledFileDO> pendingDistribution(boolean remindersOnly) {
        return pendingDistribution(remindersOnly,distributionReminderDates());
    }

    public List<DccControlledFileDO> pendingDistribution(boolean remindersOnly,DccWorkflowDatePolicy.ReminderDates dates) {
        if(dates==null) throw new IllegalArgumentException("文控提醒日期上下文缺失");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var query = new LambdaQueryWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getTenantId, tenantId)
                .in(DccControlledFileDO::getStatus, "CONTROLLED_PENDING_EFFECTIVE", "ACTIVE")
                .isNotNull(DccControlledFileDO::getControlledTime)
                .isNull(DccControlledFileDO::getDistributedTime);
        if (remindersOnly) query.le(DccControlledFileDO::getEffectiveDate, dates.reminderThrough());
        return controlledFileMapper.selectList(query.orderByAsc(DccControlledFileDO::getEffectiveDate)
                .orderByAsc(DccControlledFileDO::getId));
    }

    public List<DccControlledFileDO> dueVersions() {
        return controlledFileMapper.selectList(new LambdaQueryWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus, "CONTROLLED_PENDING_EFFECTIVE")
                .le(DccControlledFileDO::getEffectiveDate, datePolicy.now().toLocalDate())
                .orderByAsc(DccControlledFileDO::getEffectiveDate).orderByAsc(DccControlledFileDO::getId));
    }

    private void emit(String type, DccControlledFileDO file, Long oldId, LocalDateTime now) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        String eventKey = "DCC:" + tenant + ":" + file.getId() + ":" + file.getProcessInstanceId() + ":" + type;
        requireOne(jdbcTemplate.update("""
                INSERT INTO dcc_workflow_lifecycle_event
                  (tenant_id,event_key,event_type,master_id,controlled_file_id,previous_active_file_id,
                   version_no,approval_process_instance_id,occurred_at,delivery_status)
                VALUES (?,?,?,?,?,?,?,?,?,'PENDING')
                """, tenant, eventKey, type, file.getMasterId(), file.getId(), oldId,
                file.getVersionNo(), file.getProcessInstanceId(), now));
        eventPublisher.publishEvent(new DccControlledFileLifecycleEvent(
                eventKey,
                type, tenant, file.getMasterId(), file.getId(), oldId, file.getVersionNo(),
                file.getProcessInstanceId(), now));
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("受控与生效必须在正式事务内执行");
    }

    private void requireOne(int rows) {
        if (rows != 1) throw new IllegalStateException("生命周期持久化行数异常：" + rows);
    }
}
