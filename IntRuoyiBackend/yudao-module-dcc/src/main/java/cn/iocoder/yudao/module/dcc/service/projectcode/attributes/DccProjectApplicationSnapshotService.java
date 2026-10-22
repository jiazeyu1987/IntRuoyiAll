package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** A在真实申请事务中调用；BPM字符串、文件ID和数字属性轮次不互相推断。 */
@Service
public class DccProjectApplicationSnapshotService {
    @Resource private DccApplicationRoundService rounds;
    @Resource private DccProjectAttributesService attributes;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper files;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masters;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper snapshots;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;

    /** A真实File创建事务立即捕获来源，NULL BPM尚未存在，不能造占位字符串。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO prepareDraft(Long userId,Long projectId,String type,Long fileId,DccProjectAttributes actual) {
        int round=rounds.reserveDraft(projectId,type,fileId);
        var saved=attributes.beginDraft(userId,projectId,type,fileId,round);
        return actual==null?saved:attributes.saveDraft(userId,projectId,type,fileId,round,actual);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO saveReservedDraft(Long userId,Long projectId,String type,Long fileId,DccProjectAttributes actual) {
        int round=rounds.requireDraft(type,fileId);attributes.readSaved(userId,projectId,type,fileId,round);
        return attributes.saveDraft(userId,projectId,type,fileId,round,actual);
    }
    public DccProjectApplicationAttributesDO readReservedDraft(Long userId,Long projectId,String type,Long fileId) {
        return attributes.readSaved(userId,projectId,type,fileId,rounds.requireDraft(type,fileId));
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO restoreReservedDraftDefaults(Long userId,Long projectId,String type,Long fileId,DccProjectAttributes confirmedDefaults) {
        return attributes.restoreDraftDefaults(userId,projectId,type,fileId,rounds.requireDraft(type,fileId),confirmedDefaults);
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO submitReservedDraft(Long userId,Long projectId,String type,Long fileId,Integer attributeRound,String realBpm,DccProjectAttributes actual) {
        int round=rounds.bindReservedDraft(projectId,type,fileId,attributeRound,realBpm);
        var saved=attributes.readSaved(userId,projectId,type,fileId,round);
        String json=attributes.encode(actual);
        if(Boolean.TRUE.equals(saved.getSubmitted())) {
            if(!json.equals(saved.getActualAttributesJson()))throw fail(SNAPSHOT_FROZEN);
            return saved;
        }
        attributes.saveDraft(userId,projectId,type,fileId,round,actual);
        return attributes.freeze(userId,projectId,type,fileId,round);
    }

    /** 未送审检入/INITIAL：调用方先创建真实目标File并预留轮次，整个动作必须处于同一业务事务。 */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO inheritReservedDraftToNewApplication(Long userId,Long projectId,String type,
            Long sourceFileId,Integer sourceAttributeRound,Long targetFileId,Integer targetAttributeRound) {
        if(userId==null || sourceFileId==null || sourceFileId<=0 || targetFileId==null || targetFileId<=0
                || java.util.Objects.equals(sourceFileId,targetFileId))throw fail(SNAPSHOT_INVALID);
        // 与reserve/bind共用项目锁；先加锁，再读源/目标的正式身份、开放轮次及属性。
        rounds.assertAllocatedRound(projectId,type,targetFileId,targetAttributeRound);
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        var first=files.selectByIdAndTenantForUpdate(tenant,Math.min(sourceFileId,targetFileId));
        var second=files.selectByIdAndTenantForUpdate(tenant,Math.max(sourceFileId,targetFileId));
        var source=sourceFileId<targetFileId?first:second;
        var target=sourceFileId<targetFileId?second:first;
        boolean ownerReplacement=source!=null && target!=null && "REVISION".equals(type)
                && "REPLACEMENT".equals(target.getRevisionChangeType())
                && java.util.Objects.equals(sourceFileId,target.getSelectedIterationControlledFileId())
                && java.util.Objects.equals(source.getRevisionBaseActiveControlledFileId(),target.getRevisionSourceControlledFileId());
        if(ownerReplacement && !java.util.Objects.equals(source.getRequesterId(),userId))projectAccess.assertProjectOwner(userId,projectId);
        String expectedChangeType="UPLOAD".equals(type)?"NEW":"REVISION".equals(type)?"REVISION":null;
        if(expectedChangeType==null || source==null || target==null
                || !java.util.Objects.equals(source.getTenantId(),tenant) || !java.util.Objects.equals(target.getTenantId(),tenant)
                || !java.util.Objects.equals(source.getDccProjectCodeId(),projectId) || !java.util.Objects.equals(target.getDccProjectCodeId(),projectId)
                || source.getMasterId()==null || !java.util.Objects.equals(source.getMasterId(),target.getMasterId())
                || !(java.util.Objects.equals(source.getRequesterId(),userId) || ownerReplacement)
                || !java.util.Objects.equals(target.getRequesterId(),userId)
                || !expectedChangeType.equals(source.getChangeType()) || !expectedChangeType.equals(target.getChangeType())
                || !"WORKING".equals(source.getStatus()) || source.getProcessInstanceId()!=null)throw fail(SNAPSHOT_INVALID);
        var master=masters.selectById(source.getMasterId());
        if(master==null || !java.util.Objects.equals(master.getTenantId(),tenant)
                || !java.util.Objects.equals(master.getDccProjectCodeId(),projectId)
                || ownerReplacement && !java.util.Objects.equals(master.getLatestControlledFileId(),target.getRevisionSourceControlledFileId()))throw fail(SNAPSHOT_INVALID);
        if(!java.util.Objects.equals(rounds.requireDraft(type,sourceFileId),sourceAttributeRound))throw fail(SNAPSHOT_INVALID);
        var existing=snapshots.find(type,targetFileId,targetAttributeRound);
        if(existing==null && (!"WORKING".equals(target.getStatus()) || target.getProcessInstanceId()!=null
                || !java.util.Objects.equals(rounds.requireDraft(type,targetFileId),targetAttributeRound)))throw fail(SNAPSHOT_INVALID);
        return attributes.inheritSavedDraftToApplication(userId,projectId,type,sourceFileId,sourceAttributeRound,targetFileId,targetAttributeRound);
    }

    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO forkToNewApplication(Long userId,Long projectId,String type,
            Long sourceFileId,Integer sourceAttributeRound,Long targetFileId,Integer targetAttributeRound) {
        // 先取得与reserve/bind相同项目锁，再检查开放草稿及复制，避免检查后BPM已绑定。
        rounds.assertAllocatedRound(projectId,type,targetFileId,targetAttributeRound);
        rounds.assertBoundRound(projectId,type,sourceFileId,sourceAttributeRound);
        Long tenant=cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        for(Long id:new Long[]{sourceFileId,targetFileId}) {
            var file=id==null?null:files.selectById(id);
            if(file==null || !java.util.Objects.equals(file.getTenantId(),tenant) || !java.util.Objects.equals(file.getDccProjectCodeId(),projectId))throw fail(SNAPSHOT_INVALID);
        }
        if(snapshots.find(type,targetFileId,targetAttributeRound)==null
                && !java.util.Objects.equals(rounds.requireDraft(type,targetFileId),targetAttributeRound))throw fail(SNAPSHOT_INVALID);
        return attributes.forkSavedToApplication(userId,projectId,type,sourceFileId,sourceAttributeRound,targetFileId,targetAttributeRound);
    }

    /** 正式申请/BPM草稿创建后立即建立来源，后续普通保存不可随项目修改覆盖。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO begin(Long userId, Long projectId, String type,
                                                   Long controlledFileId, String bpmRound) {
        int round = rounds.bind(projectId, type, controlledFileId, bpmRound);
        return attributes.beginDraft(userId, projectId, type, controlledFileId, round);
    }

    /** 普通草稿保存只用已建立的正式轮次和来源；不能在保存时用今日默认补建来源。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO saveDraft(Long userId, Long projectId, String type,
                                                       Long controlledFileId, String bpmRound, DccProjectAttributes actual) {
        int round = rounds.require(type, controlledFileId, bpmRound);
        attributes.readSaved(userId, projectId, type, controlledFileId, round);
        return attributes.saveDraft(userId, projectId, type, controlledFileId, round, actual);
    }

    /** A确认实际草稿权限/用户恢复意图后调用；确认值已变化或轮次被冻结时明确拒绝。 */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO restoreDraftDefaults(Long userId, Long projectId, String type,
                                                                 Long controlledFileId, String bpmRound,
                                                                 DccProjectAttributes confirmedDefaults) {
        int round = rounds.require(type, controlledFileId, bpmRound);
        return attributes.restoreDraftDefaults(userId, projectId, type, controlledFileId, round, confirmedDefaults);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO submit(Long userId, Long projectId, String type,
                                                    Long controlledFileId, String bpmRound, DccProjectAttributes actual) {
        String payload = attributes.encode(actual);
        var saved = begin(userId, projectId, type, controlledFileId, bpmRound);
        int round = saved.getApplicationRound();
        if (Boolean.TRUE.equals(saved.getSubmitted())) {
            if (!payload.equals(saved.getActualAttributesJson())) throw fail(SNAPSHOT_FROZEN);
            return saved;
        }
        attributes.saveDraft(userId, projectId, type, controlledFileId, round, actual);
        return attributes.freeze(userId, projectId, type, controlledFileId, round);
    }

    /** 只读正式绑定，缺映射明确失败；不读今日默认补历史。 */
    public DccProjectApplicationAttributesDO read(Long userId, Long projectId, String type,
                                                  Long controlledFileId, String bpmRound) {
        int round = rounds.require(type, controlledFileId, bpmRound);
        return attributes.readSaved(userId, projectId, type, controlledFileId, round);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO rework(Long userId, Long projectId, String type, Long controlledFileId,
                                                   String previousBpmRound, String nextBpmRound) {
        int previous = rounds.require(type, controlledFileId, previousBpmRound);
        int next = rounds.bind(projectId, type, controlledFileId, nextBpmRound);
        if (next != previous + 1) throw fail(SNAPSHOT_INVALID);
        return attributes.forkForRework(userId, projectId, type, controlledFileId, previous);
    }
}
