package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Set;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** A 在同一申请事务中调用；A 必须先校验正式申请归属、动作及轮次。此服务不提供可伪造申请 ID 的写 HTTP 接口。 */
@Service
public class DccProjectAttributesService {
    @Resource private DccProjectCodeMapper projectCodeMapper;
    @Resource private DccProjectApplicationAttributesMapper attributesMapper;
    @Resource private DccProjectAccessService accessService;

    public DccProjectAttributes initialize(Long userId, Long projectId, String applicationType) {
        requireType(applicationType);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        return defaults(requireProject(projectId, false));
    }
    public DccProjectAttributes defaults(DccProjectCodeDO project) {
        if (project.getDefaultAttributesJson() == null || project.getDefaultAttributesJson().isBlank()) {
            throw fail(DEFAULTS_MISSING);
        }
        return readValue(project.getDefaultAttributesJson());
    }
    public DccProjectAttributes readValue(String json) {
        if (json == null || json.isBlank()) throw fail(DEFAULTS_MISSING);
        var value = JsonUtils.parseObject(json, DccProjectAttributes.class);
        if (value == null) throw fail(INVALID);
        return value.validated();
    }
    public String encode(DccProjectAttributes value) {
        if (value == null) throw fail(INVALID);
        return JsonUtils.toJsonString(value.validated());
    }

    /** A 取得正式申请 ID 后立即调用；第一次初始化即持久化默认来源，避免项目修改与首次保存之间的时间窗口。 */
    @Transactional(rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO beginDraft(Long userId, Long projectId, String type, Long id, Integer round) {
        requireKey(type, id, round);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        var project = requireProject(projectId, true);
        var saved = attributesMapper.find(type, id, round);
        if (saved != null) {
            requireSameProject(saved, projectId);
            return saved; // 重开不覆盖，已冻结轮次也只读回显
        }
        return saveDraft(userId, projectId, type, id, round, defaults(project));
    }

    /** 驳回后新轮次复制原申请来源与实际值，不重新初始化当前项目默认。 */
    @Transactional(rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO forkForRework(Long userId, Long projectId, String type, Long id, Integer previousRound) {
        requireKey(type, id, previousRound);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        requireProject(projectId, true);
        var previous = attributesMapper.find(type, id, previousRound);
        if (previous == null || !Boolean.TRUE.equals(previous.getSubmitted())) throw fail(SNAPSHOT_INVALID);
        requireSameProject(previous, projectId);
        var saved = attributesMapper.find(type, id, previousRound + 1);
        if (saved != null) {
            requireSameProject(saved, projectId);
            return saved;
        }
        saved = new DccProjectApplicationAttributesDO();
        saved.setTenantId(TenantContextHolder.getRequiredTenantId());
        saved.setProjectCodeId(projectId); saved.setApplicationType(type); saved.setApplicationId(id);
        saved.setApplicationRound(previousRound + 1); saved.setDefaultSourceJson(previous.getDefaultSourceJson());
        saved.setActualAttributesJson(previous.getActualAttributesJson()); saved.setSubmitted(false);
        if (attributesMapper.insert(saved) != 1) throw fail(SNAPSHOT_INVALID);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO saveDraft(Long userId, Long projectId, String type,
                                                       Long applicationId, Integer round, DccProjectAttributes actual) {
        requireKey(type, applicationId, round);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        var project = requireProject(projectId, true); // 串行化同项目草稿写入及提交
        String actualJson = encode(actual);
        var saved = attributesMapper.find(type, applicationId, round);
        if (saved != null) {
            requireSameProject(saved, projectId);
            if (Boolean.TRUE.equals(saved.getSubmitted())) throw fail(SNAPSHOT_FROZEN);
        } else {
            saved = new DccProjectApplicationAttributesDO();
            saved.setTenantId(TenantContextHolder.getRequiredTenantId());
            saved.setProjectCodeId(projectId);
            saved.setApplicationType(type);
            saved.setApplicationId(applicationId);
            saved.setApplicationRound(round);
            saved.setDefaultSourceJson(encode(defaults(project)));
            saved.setSubmitted(false);
        }
        saved.setActualAttributesJson(actualJson);
        int written = saved.getId() == null ? attributesMapper.insert(saved) : attributesMapper.updateById(saved);
        if (written != 1) throw fail(SNAPSHOT_INVALID);
        return saved;
    }

    /** CC-2跨真实文件返工；Root预留与File身份由唯一桥接入口先验证。 */
    @Transactional(rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO forkSavedToApplication(Long userId,Long projectId,String type,
            Long sourceId,Integer sourceRound,Long targetId,Integer targetRound) {
        return copySavedToApplication(userId,projectId,type,sourceId,sourceRound,targetId,targetRound,true);
    }

    /** 未送审跨File继承，唯一桥接入口校验真实File及NULL BPM预留；不改变源submitted事实。 */
    @Transactional(rollbackFor=Exception.class)
    public DccProjectApplicationAttributesDO inheritSavedDraftToApplication(Long userId,Long projectId,String type,
            Long sourceId,Integer sourceRound,Long targetId,Integer targetRound) {
        return copySavedToApplication(userId,projectId,type,sourceId,sourceRound,targetId,targetRound,false);
    }

    private DccProjectApplicationAttributesDO copySavedToApplication(Long userId,Long projectId,String type,
            Long sourceId,Integer sourceRound,Long targetId,Integer targetRound,boolean sourceSubmitted) {
        requireKey(type,sourceId,sourceRound);requireKey(type,targetId,targetRound);
        if(Objects.equals(sourceId,targetId))throw fail(SNAPSHOT_INVALID);
        accessService.assertProjectEditorOrOwner(userId,projectId);requireProject(projectId,true);
        var source=attributesMapper.find(type,sourceId,sourceRound);
        if(source==null || !Objects.equals(source.getSubmitted(),sourceSubmitted))throw fail(SNAPSHOT_INVALID);
        requireSameProject(source,projectId);readValue(source.getDefaultSourceJson());readValue(source.getActualAttributesJson());
        var saved=attributesMapper.find(type,targetId,targetRound);
        if(saved!=null) {
            requireSameProject(saved,projectId);
            if(!Objects.equals(saved.getSourceApplicationId(),sourceId) || !Objects.equals(saved.getSourceApplicationRound(),sourceRound)
                    || !Objects.equals(saved.getDefaultSourceJson(),source.getDefaultSourceJson()))throw fail(SNAPSHOT_INVALID);
            return saved; // 精确同源回读，不覆盖目标后续实际手改或已冻结轮次
        }
        saved=new DccProjectApplicationAttributesDO();saved.setTenantId(TenantContextHolder.getRequiredTenantId());
        saved.setProjectCodeId(projectId);saved.setApplicationType(type);saved.setApplicationId(targetId);saved.setApplicationRound(targetRound);
        saved.setSourceApplicationId(sourceId);saved.setSourceApplicationRound(sourceRound);
        saved.setDefaultSourceJson(source.getDefaultSourceJson());saved.setActualAttributesJson(source.getActualAttributesJson());saved.setSubmitted(false);
        if(attributesMapper.insert(saved)!=1)throw fail(SNAPSHOT_INVALID);
        return saved;
    }
    /** 显式确认恢复当前默认；A验证正式申请权限后调用，不能由普通保存猜测恢复意图。 */
    @Transactional(rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO restoreDraftDefaults(Long userId, Long projectId, String type,
                                                                 Long id, Integer round, DccProjectAttributes confirmedDefaults) {
        requireKey(type, id, round);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        var project = requireProject(projectId, true);
        var saved = attributesMapper.find(type, id, round);
        if (saved == null) throw fail(SNAPSHOT_INVALID);
        requireSameProject(saved, projectId);
        if (Boolean.TRUE.equals(saved.getSubmitted())) throw fail(SNAPSHOT_FROZEN);
        var current = defaults(project);
        if (confirmedDefaults == null || !current.equals(confirmedDefaults.validated())) throw fail(DEFAULTS_CHANGED);
        String restored = encode(current);
        saved.setDefaultSourceJson(restored);
        saved.setActualAttributesJson(restored);
        if (attributesMapper.updateById(saved) != 1) throw fail(SNAPSHOT_INVALID);
        return saved;
    }
    @Transactional(rollbackFor = Exception.class)
    public DccProjectApplicationAttributesDO freeze(Long userId, Long projectId, String type, Long id, Integer round) {
        requireKey(type, id, round);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        requireProject(projectId, true);
        var saved = attributesMapper.find(type, id, round);
        if (saved == null) throw fail(SNAPSHOT_INVALID);
        requireSameProject(saved, projectId);
        readValue(saved.getDefaultSourceJson());
        readValue(saved.getActualAttributesJson());
        if (!Boolean.TRUE.equals(saved.getSubmitted())) {
            saved.setSubmitted(true);
            if (attributesMapper.updateById(saved) != 1) throw fail(SNAPSHOT_INVALID);
        }
        return saved;
    }
    public DccProjectApplicationAttributesDO readSaved(Long userId, Long projectId, String type, Long id, Integer round) {
        requireKey(type, id, round);
        accessService.assertProjectEditorOrOwner(userId, projectId);
        var saved = attributesMapper.find(type, id, round);
        if (saved == null) throw fail(SNAPSHOT_INVALID);
        requireSameProject(saved, projectId);
        return saved; // 不读取项目当前默认，历史和作废隔离
    }
    private DccProjectCodeDO requireProject(Long id, boolean lock) {
        var project = id == null ? null : (lock ? projectCodeMapper.selectByIdForUpdate(id) : projectCodeMapper.selectById(id));
        if (project == null || !Objects.equals(project.getTenantId(), TenantContextHolder.getRequiredTenantId()) || !DccProjectCodeStatusConstants.ENABLE.equals(project.getStatus())) throw fail(DEFAULTS_MISSING);
        return project;
    }
    private void requireSameProject(DccProjectApplicationAttributesDO saved, Long projectId) {
        if (!Objects.equals(saved.getTenantId(), TenantContextHolder.getRequiredTenantId()) || !Objects.equals(saved.getProjectCodeId(), projectId)) throw fail(SNAPSHOT_INVALID);
    }
    private void requireKey(String type, Long id, Integer round) {
        requireType(type);
        if (id == null || id <= 0 || round == null || round <= 0) throw fail(SNAPSHOT_INVALID);
    }
    private void requireType(String type) {
        if (type == null || !Set.of("UPLOAD", "REVISION", "OBSOLETE").contains(type)) throw fail(SNAPSHOT_INVALID);
    }
}
