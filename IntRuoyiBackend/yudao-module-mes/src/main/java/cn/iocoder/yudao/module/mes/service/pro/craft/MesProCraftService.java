package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version.MesProRouteVersionCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import com.alibaba.fastjson.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;
import static cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl.*;

/** 工艺库、无损初始化、整批审批发布。所有写入均在同一租户工艺锁下串行。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MesProCraftService {
    @Resource private MesProCraftMapper craftMapper;
    @Resource private MesProCraftChangeMapper changeMapper;
    @Resource private MesProProcessMapper processMapper;
    @Resource private MesProRouteVersionMapper versionMapper;
    @Resource private ObjectProvider<MesProRouteCandidateConfigService> candidateConfig;
    @Resource private ObjectProvider<MesProRouteVersionWorkflowService> workflow;
    @Resource private ObjectProvider<MesProRouteVersionBusinessApprovalSubmitService> submitService;
    @Resource private ObjectProvider<MesProRouteVersionLifecycleService> lifecycle;
    @Resource private MesProRouteControlledContentAdapter platform;
    @Resource private cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi bpmApi;

    public void lockCatalog() {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        craftMapper.ensureSequence(tenant);
        Objects.requireNonNull(craftMapper.lockSequence(tenant), "工艺编号序列缺失");
    }

    @Transactional(readOnly = true)
    public List<MesProCraftDO> list(Long processId) {
        return craftMapper.selectList(new LambdaQueryWrapperX<MesProCraftDO>()
                .eqIfPresent(MesProCraftDO::getProcessId, processId).orderByAsc(MesProCraftDO::getId));
    }
    @Transactional(readOnly = true)
    public List<MesProCraftChangeDO> changes() {
        return changeMapper.selectList(new LambdaQueryWrapperX<MesProCraftChangeDO>()
                .orderByDesc(MesProCraftChangeDO::getId));
    }
    public MesProCraftDO require(Long id) {
        MesProCraftDO craft = craftMapper.selectById(id);
        if (craft == null) throw invalid("工艺不存在：" + id);
        return craft;
    }

    public MesProCraftDO create(MesProCraftSaveReqVO request) {
        lockCatalog();
        if (request.getId() != null) throw invalid("新增工艺不能指定编号");
        requireProcess(request.getProcessId());
        JSONArray parameters = normalize(request.getParameters());
        validateDefinitions(request.getProcessId(), parameters);
        MesProCraftDO craft = insertCraft(request.getName(), request.getProcessId(), parameters);
        MesProCraftChangeDO change = newChange(craft.getId(), "CREATE", null, JSON.toJSONString(craft), request.getReason());
        change.setStatus("APPLIED");
        if (changeMapper.insert(change) != 1) throw invalid("工艺变更记录保存失败");
        return craft;
    }
    private void requireProcess(Long processId) {
        if (processId == null || processMapper.selectById(processId) == null)
            throw invalid("所属基础工序不存在");
    }
    private MesProCraftDO insertCraft(String name, Long processId, JSONArray parameters) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        long next = Math.addExact(craftMapper.lockSequence(tenant), 1L);
        if (craftMapper.advanceSequence(tenant, next) != 1) throw invalid("工艺编号分配失败");
        MesProCraftDO craft = new MesProCraftDO();
        craft.setCode(String.format(Locale.ROOT, "GY-%04d", next));
        craft.setName(name);
        craft.setProcessId(processId);
        craft.setParametersJson(parameters.toJSONString());
        craft.setFingerprint(rowFingerprint(name, processId, parameters));
        if (craftMapper.insert(craft) != 1) throw invalid("工艺保存失败");
        return craft;
    }
    static void validateDefinitions(Long processId, JSONArray parameters) {
        // 使用现有生产配置校验器验证参数类型、范围、选项和默认值，避免建立第二套校验语义。
        JSONObject config = new JSONObject(true);
        config.put("routeProcessId", 1L); config.put("processId", processId);
        config.put("overagePercent", 0); config.put("lossReasons", new JSONArray());
        config.put("deviceSelectionGroups", JSON.parseArray("[{\"deviceGroupKey\":\"validation\",\"selectionMode\":\"SINGLE\",\"deviceIds\":[1]}]"));
        config.put("parameterRules", expand(config, parameters, List.of(1L)));
        JSONObject configs = new JSONObject(true);
        configs.put("productionProcessConfigSchemaVersion", 1);
        configs.put("productionProcessConfigs", List.of(config));
        configs.put("flowGraph", Map.of("nodes", List.of(Map.of("routeProcessId", 1L, "processId", processId))));
        MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(0L, JSON.parseObject(configs.toJSONString()));
    }

    public Map<String, Object> preview(MesProCraftSaveReqVO request) {
        lockCatalog();
        MesProCraftDO craft = require(request.getId());
        requireUnchanged(craft, request.getExpectedFingerprint());
        if (!Objects.equals(craft.getProcessId(), request.getProcessId())) throw invalid("工艺所属工序不可变更");
        validateDefinitions(craft.getProcessId(), normalize(request.getParameters()));
        assertNoPending(craft.getId());
        List<String> blockers = new ArrayList<>();
        List<MesProRouteVersionDO> refs = references(craft.getId(), blockers);
        JSONArray impacts = impacts(refs, craft.getId());
        String token = token(craft, request, impacts, blockers);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", JSON.parseArray(craft.getParametersJson()));
        result.put("after", normalize(request.getParameters()));
        result.put("beforeName", craft.getName()); result.put("afterName", request.getName());
        result.put("routes", impacts); result.put("blockers", blockers); result.put("impactToken", token);
        return result;
    }
    private String token(MesProCraftDO craft, MesProCraftSaveReqVO request, JSONArray impacts, List<String> blockers) {
        JSONObject object = new JSONObject(true);
        object.put("before", craft.getFingerprint()); object.put("name", request.getName());
        object.put("parameters", normalize(request.getParameters()));
        object.put("routes", impacts); object.put("blockers", blockers);
        return new MesProRouteSnapshotCanonicalizer().sha256(object.toJSONString());
    }
    private void requireUnchanged(MesProCraftDO craft, String expected) {
        if (!Objects.equals(craft.getFingerprint(), expected)) throw invalid("工艺已被修改，请重新加载后确认");
    }

    public MesProCraftChangeDO updateAll(MesProCraftSaveReqVO request) {
        lockCatalog();
        Map<String, Object> preview = preview(request);
        if (!Objects.equals(request.getImpactToken(), preview.get("impactToken"))) throw invalid("影响范围已变化，请重新预览");
        @SuppressWarnings("unchecked") List<String> blockers = (List<String>) preview.get("blockers");
        if (!blockers.isEmpty()) throw invalid(String.join("；", blockers));
        MesProCraftDO before = require(request.getId());
        MesProCraftDO after = JSON.parseObject(JSON.toJSONString(before), MesProCraftDO.class);
        after.setName(request.getName()); after.setParametersJson(normalize(request.getParameters()).toJSONString());
        after.setFingerprint(rowFingerprint(after.getName(), after.getProcessId(), request.getParameters()));
        MesProCraftChangeDO change = newChange(before.getId(), "UPDATE", JSON.toJSONString(before),
                JSON.toJSONString(after), request.getReason());
        if (changeMapper.insert(change) != 1) throw invalid("工艺变更记录保存失败");
        List<MesProRouteVersionDO> refs = references(before.getId(), new ArrayList<>());
        for (MesProRouteVersionDO active : refs) {
            MesProRouteVersionDO candidate = newCandidate(active, "工艺 " + before.getCode() + " 全量升级：" + request.getReason());
            JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
            for (JSONObject config : configs(snapshot)) {
                if (Objects.equals(before.getId(), config.getLong("craftId"))) {
                    List<Long> ids = config.getJSONArray("craftDeviceIds").toJavaList(Long.class);
                    bind(config, after.getId(), after.getCode(), JSON.parseArray(after.getParametersJson()), ids);
                }
            }
            saveGroupedCandidate(change, candidate, snapshot);
        }
        submitGroup(change);
        return changeMapper.selectById(change.getId());
    }

    private List<MesProRouteVersionDO> references(Long craftId, List<String> blockers) {
        List<MesProRouteVersionDO> result = new ArrayList<>();
        for (MesProRouteVersionDO active : versionMapper.selectActiveListWithExistingRoutes()) {
            boolean used = bindingConfigs(JSON.parseObject(active.getRouteSnapshotJson())).stream()
                    .anyMatch(config -> Objects.equals(craftId, config.getLong("craftId")));
            MesProRouteVersionDO open = versionMapper.selectOpenCandidateByRouteId(active.getRouteId());
            boolean draftUsed = open != null && bindingConfigs(JSON.parseObject(open.getRouteSnapshotJson())).stream()
                    .anyMatch(config -> Objects.equals(craftId, config.getLong("craftId")));
            if (used) result.add(active);
            if ((used || draftUsed) && open != null)
                blockers.add(routeLabel(active) + " 已有未完成的候选版本，请先处理");
        }
        result.sort(Comparator.comparing(MesProRouteVersionDO::getRouteId));
        return result;
    }
    private JSONArray impacts(List<MesProRouteVersionDO> refs, Long craftId) {
        JSONArray result = new JSONArray();
        for (MesProRouteVersionDO version : refs) {
            JSONObject row = JSON.parseObject(version.getRouteSnapshotJson());
            JSONObject impact = new JSONObject(true);
            impact.put("routeId", version.getRouteId()); impact.put("routeCode", row.getString("routeCode"));
            impact.put("routeName", row.getString("routeName")); impact.put("routeVersionId", version.getId());
            impact.put("versionNo", version.getVersionNo()); impact.put("snapshotSha256", version.getRouteSnapshotSha256());
            impact.put("nodes", configs(row).stream().filter(c -> Objects.equals(craftId, c.getLong("craftId")))
                    .map(c -> Map.of("routeProcessId", c.getLong("routeProcessId"), "processId", c.getLong("processId"))).toList());
            result.add(impact);
        }
        return result;
    }

    public Map<String, Object> initializationPreview() {
        lockCatalog();
        List<String> blockers = new ArrayList<>();
        JSONArray sources = new JSONArray();
        int empty = 0;
        for (MesProRouteVersionDO active : versionMapper.selectActiveListWithExistingRoutes()) {
            JSONObject snapshot = JSON.parseObject(active.getRouteSnapshotJson());
            JSONObject data = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
            if (data == null || data.getJSONArray("productionProcessConfigs") == null) {
                blockers.add(routeLabel(active) + " 缺少正式生产配置，请先完成现有配置迁移");
                continue;
            }
            boolean eligible = false;
            for (JSONObject config : configs(snapshot)) {
                if (config.getLong("craftId") != null) continue;
                if (config.getJSONArray("parameterRules") != null && config.getJSONArray("parameterRules").isEmpty()) { empty++; continue; }
                eligible = true;
                try {
                    requireProcess(config.getLong("processId"));
                    Extracted extracted = extract(config);
                    validateDefinitions(config.getLong("processId"), extracted.parameters());
                    // 按现有设备身份重新展开后必须与原参数语义完全一致。
                    JSONArray expanded = expand(config, extracted.parameters(), extracted.deviceIds());
                    if (!sameRules(config.getJSONArray("parameterRules"), expanded)) throw invalid("参数展开结果与原配置不一致");
                    JSONObject source = new JSONObject(true);
                    source.put("routeId", active.getRouteId()); source.put("routeVersionId", active.getId());
                    source.put("routeName", snapshot.getString("routeName"));
                    source.put("snapshotSha256", active.getRouteSnapshotSha256());
                    source.put("routeProcessId", config.getLong("routeProcessId"));
                    source.put("processId", config.getLong("processId")); source.put("processName", config.getString("processName"));
                    source.put("parameters", extracted.parameters()); source.put("deviceIds", extracted.deviceIds());
                    sources.add(source);
                } catch (cn.iocoder.yudao.framework.common.exception.ServiceException
                         | com.alibaba.fastjson.JSONException | IllegalArgumentException ex) {
                    blockers.add(routeLabel(active) + "，工序 " + config.getLong("routeProcessId") + "：" + ex.getMessage());
                }
            }
            if (eligible && versionMapper.selectOpenCandidateByRouteId(active.getRouteId()) != null)
                blockers.add(routeLabel(active) + " 已有未完成候选版本");
        }
        JSONObject signature = new JSONObject(true);
        signature.put("sources", sources); signature.put("blockers", blockers);
        return Map.of("sources", sources, "blockers", blockers, "emptyNodeCount", empty,
                "impactToken", new MesProRouteSnapshotCanonicalizer().sha256(signature.toJSONString()));
    }

    public MesProCraftChangeDO initialize(String impactToken) {
        lockCatalog();
        Map<String, Object> preview = initializationPreview();
        if (!Objects.equals(impactToken, preview.get("impactToken"))) throw invalid("初始化来源已变化，请重新预览");
        List<?> blockers = (List<?>) preview.get("blockers");
        if (!blockers.isEmpty()) throw invalid("初始化存在冲突：" + blockers);
        JSONArray sources = (JSONArray) preview.get("sources");
        if (sources.isEmpty()) throw invalid("没有需要初始化的工序参数");
        MesProCraftChangeDO change = newChange(null, "INITIALIZE", sources.toJSONString(), "[]", "从现有工艺流程无损初始化");
        if (changeMapper.insert(change) != 1) throw invalid("工艺变更记录保存失败");
        Map<String, MesProCraftDO> byFingerprint = new LinkedHashMap<>();
        list(null).forEach(c -> byFingerprint.putIfAbsent(fingerprint(c.getProcessId(), JSON.parseArray(c.getParametersJson())), c));
        Map<Long, List<JSONObject>> byRoute = new TreeMap<>();
        for (Object raw : sources) {
            JSONObject source = (JSONObject) raw;
            byRoute.computeIfAbsent(source.getLong("routeId"), ignored -> new ArrayList<>()).add(source);
        }
        JSONArray importedCraftIds = new JSONArray();
        for (Map.Entry<Long, List<JSONObject>> entry : byRoute.entrySet()) {
            MesProRouteVersionDO active = versionMapper.selectActiveByRouteId(entry.getKey());
            MesProRouteVersionDO candidate = newCandidate(active, change.getReason());
            JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
            for (JSONObject source : entry.getValue()) {
                JSONObject config = configs(snapshot).stream().filter(c -> Objects.equals(c.getLong("routeProcessId"),
                        source.getLong("routeProcessId"))).findFirst().orElseThrow(() -> invalid("初始化工序身份已变化"));
                if (!sameRules(config.getJSONArray("parameterRules"), expand(config,
                        source.getJSONArray("parameters"), source.getJSONArray("deviceIds").toJavaList(Long.class))))
                    throw invalid("候选版本参数与初始化预览不一致");
                String fingerprint = fingerprint(source.getLong("processId"), source.getJSONArray("parameters"));
                MesProCraftDO craft = byFingerprint.get(fingerprint);
                if (craft == null) {
                    craft = insertCraft(source.getString("processName") + "工艺",
                            source.getLong("processId"), source.getJSONArray("parameters"));
                    byFingerprint.put(fingerprint, craft);
                    importedCraftIds.add(craft.getId());
                }
                bind(config, craft.getId(), craft.getCode(), JSON.parseArray(craft.getParametersJson()),
                        source.getJSONArray("deviceIds").toJavaList(Long.class));
            }
            saveGroupedCandidate(change, candidate, snapshot);
        }
        change.setAfterJson(importedCraftIds.toJSONString());
        saveChange(change);
        submitGroup(change);
        return changeMapper.selectById(change.getId());
    }

    private MesProRouteVersionDO newCandidate(MesProRouteVersionDO active, String reason) {
        if (versionMapper.selectOpenCandidateByRouteId(active.getRouteId()) != null)
            throw invalid(routeLabel(active) + " 已有未完成候选版本");
        MesProRouteVersionCreateReqVO request = new MesProRouteVersionCreateReqVO();
        request.setRouteId(active.getRouteId()); request.setSourceRouteVersionId(active.getId()); request.setChangeReason(reason);
        return workflow.getObject().createCandidate(request);
    }
    private MesProCraftChangeDO newChange(Long craftId, String type, String before, String after, String reason) {
        MesProCraftChangeDO c = new MesProCraftChangeDO();
        c.setCraftId(craftId); c.setChangeType(type); c.setStatus("PREPARING");
        c.setBeforeJson(before); c.setAfterJson(after); c.setReason(reason);
        c.setCandidateIdsJson("[]"); c.setCandidateHashesJson("{}");
        return c;
    }
    private void saveGroupedCandidate(MesProCraftChangeDO change, MesProRouteVersionDO candidate, JSONObject snapshot) {
        JSONObject data = snapshot.getJSONObject("configSnapshots");
        candidateConfig.getObject().saveConfigSnapshots(candidate.getId(), candidate.getRouteSnapshotSha256(),
                Map.of("productionProcessConfigSchemaVersion", 1, "productionProcessConfigs", data.getJSONArray("productionProcessConfigs")));
        MesProRouteVersionDO saved = versionMapper.selectById(candidate.getId());
        JSONObject grouped = JSON.parseObject(saved.getRouteSnapshotJson());
        grouped.getJSONObject("configSnapshots").put("craftChangeId", change.getId());
        MesProRouteVersionSnapshotIdentityWriter.apply(saved, grouped.toJSONString());
        if (versionMapper.updateById(saved) != 1) throw invalid("关联流程候选版本保存失败");
        JSONArray ids = JSON.parseArray(change.getCandidateIdsJson()); ids.add(saved.getId());
        JSONObject hashes = JSON.parseObject(change.getCandidateHashesJson());
        hashes.put(saved.getId().toString(), saved.getRouteSnapshotSha256());
        MesProRouteVersionDO source = versionMapper.selectById(saved.getSourceRouteVersionId());
        if (source == null || source.getRouteSnapshotSha256() == null) throw invalid("关联流程正式快照身份缺失");
        hashes.put(saved.getId() + ":source", source.getRouteSnapshotSha256());
        change.setCandidateIdsJson(ids.toJSONString()); change.setCandidateHashesJson(hashes.toJSONString());
        saveChange(change);
        var blockers = workflow.getObject().getPublishBlockers(saved.getId());
        if (!Boolean.TRUE.equals(blockers.getPublishable())) throw invalid("关联流程不能升级：" + blockers.getBlockers());
    }
    private void submitGroup(MesProCraftChangeDO change) {
        List<Long> ids = JSON.parseArray(change.getCandidateIdsJson()).toJavaList(Long.class);
        for (Long id : ids) submitService.getObject().submitAndPublishCandidate(id);
        change.setStatus("PENDING"); saveChange(change);
        if (ids.isEmpty()) applyCraft(change);
        else onRouteReady(versionMapper.selectById(ids.get(0)), SecurityFrameworkUtils.getLoginUserId());
    }

    /** 生命周期发布入口调用：未全部通过审批时保持 READY，最后一个审批统一发布整批。 */
    public boolean onRouteReady(MesProRouteVersionDO candidate, Long publisherId) {
        Long groupId = groupId(candidate);
        if (groupId == null) return false;
        lockCatalog();
        MesProCraftChangeDO change = changeMapper.selectById(groupId);
        if (change == null) throw invalid("工艺关联升级记录缺失");
        if ("APPLYING".equals(change.getStatus())) return false;
        if ("PREPARING".equals(change.getStatus())) return true;
        if (!"PENDING".equals(change.getStatus())) throw invalid("本次工艺修改已作废或已完成");
        List<Long> ids = JSON.parseArray(change.getCandidateIdsJson()).toJavaList(Long.class);
        List<MesProRouteVersionDO> versions = ids.stream().map(versionMapper::selectById).toList();
        if (versions.stream().anyMatch(v -> v == null)) throw invalid("关联流程候选版本缺失");
        if (versions.stream().anyMatch(v -> !STATUS_READY_TO_PUBLISH.equals(v.getLifecycleStatus()))) return true;
        JSONObject hashes = JSON.parseObject(change.getCandidateHashesJson());
        for (MesProRouteVersionDO v : versions) {
            if (!Objects.equals(hashes.getString(v.getId().toString()), v.getRouteSnapshotSha256()))
                throw invalid("待升级流程内容已变化，整次工艺修改不能生效");
            MesProRouteVersionDO active = versionMapper.selectActiveByRouteIdForUpdate(v.getRouteId());
            if (active == null || !Objects.equals(active.getId(), v.getSourceRouteVersionId()))
                throw invalid("关联流程的正式版本已变化，整次工艺修改不能生效");
            if (hashes.getString(v.getId() + ":source") == null
                    || !Objects.equals(hashes.getString(v.getId() + ":source"), active.getRouteSnapshotSha256()))
                throw invalid("关联流程的正式参数已变化，整次工艺修改不能生效");
        }
        change.setStatus("APPLYING"); saveChange(change);
        for (Long id : ids) lifecycle.getObject().publishCandidate(id, publisherId);
        applyCraft(change);
        return true;
    }
    private void saveChange(MesProCraftChangeDO change) {
        if (changeMapper.updateById(change) != 1) throw invalid("工艺变更状态保存失败");
    }
    private void applyCraft(MesProCraftChangeDO change) {
        if ("UPDATE".equals(change.getChangeType())) {
            MesProCraftDO after = JSON.parseObject(change.getAfterJson(), MesProCraftDO.class);
            MesProCraftDO update = new MesProCraftDO();
            update.setId(after.getId()); update.setName(after.getName());
            update.setParametersJson(after.getParametersJson()); update.setFingerprint(after.getFingerprint());
            if (craftMapper.updateById(update) != 1) throw invalid("工艺修改保存失败");
        }
        change.setStatus("APPLIED"); saveChange(change);
    }
    public Long groupId(MesProRouteVersionDO version) {
        if (version == null || version.getRouteSnapshotJson() == null) return null;
        JSONObject root = JSON.parseObject(version.getRouteSnapshotJson());
        JSONObject data = root.getJSONObject("configSnapshots");
        return data == null ? null : data.getLong("craftChangeId");
    }
    public boolean cancelledBatch(MesProRouteVersionDO version) {
        Long group = groupId(version);
        if (group == null) return false;
        MesProCraftChangeDO change = changeMapper.selectById(group);
        if (change == null) throw invalid("工艺整批升级记录缺失");
        return "CANCELLED".equals(change.getStatus());
    }
    public void assertEditable(MesProRouteVersionDO version) {
        if (groupId(version) != null) throw invalid("该候选版本属于工艺整批升级，请在工艺库处理整次修改");
    }
    private void assertNoPending(Long craftId) {
        for (MesProCraftChangeDO change : changes()) {
            if (!Set.of("PREPARING", "PENDING", "APPLYING").contains(change.getStatus())) continue;
            if (Objects.equals(craftId, change.getCraftId()) || "INITIALIZE".equals(change.getChangeType()))
                throw invalid("该工艺存在尚未完成的关联升级");
        }
    }

    public void cancelChange(Long id) {
        lockCatalog();
        MesProCraftChangeDO change = changeMapper.selectById(id);
        if (change == null) throw invalid("工艺修改记录不存在");
        if ("CANCELLED".equals(change.getStatus())) return;
        if (!"PENDING".equals(change.getStatus())) throw invalid("当前修改不能取消");
        abort(change, SecurityFrameworkUtils.getLoginUserId());
    }
    public void abortRoute(MesProRouteVersionDO route, Long actorId) {
        if (groupId(route) == null) return;
        lockCatalog();
        MesProCraftChangeDO change = changeMapper.selectById(groupId(route));
        if (change == null) throw invalid("工艺修改记录不存在");
        if ("CANCELLED".equals(change.getStatus())) return;
        if (!Set.of("PREPARING", "PENDING").contains(change.getStatus())) throw invalid("工艺修改当前不能作废");
        abort(change, actorId);
    }
    private void abort(MesProCraftChangeDO change, Long actorId) {
        change.setStatus("CANCELLED"); saveChange(change);
        for (Long id : JSON.parseArray(change.getCandidateIdsJson()).toJavaList(Long.class)) {
            MesProRouteVersionDO version = versionMapper.selectById(id);
            if (version == null || Boolean.TRUE.equals(version.getActive())) throw invalid("关联升级状态不一致，无法作废");
            if (STATUS_PENDING_APPROVAL.equals(version.getLifecycleStatus())) {
                if (version.getApprovalProcessInstanceId() != null)
                    bpmApi.cancelProcessInstance(actorId, version.getApprovalProcessInstanceId(), "工艺修改整批作废");
                platform.recordWithdrawn(version, actorId);
                version.setLifecycleStatus(STATUS_DRAFT);
            }
            platform.recordCancelled(version, actorId);
            version.setLifecycleStatus(STATUS_CANCELLED);
            if (versionMapper.updateById(version) != 1) throw invalid("关联流程作废失败");
        }
        if ("INITIALIZE".equals(change.getChangeType())) {
            for (Long id : JSON.parseArray(change.getAfterJson()).toJavaList(Long.class)) {
                if (craftMapper.deleteById(id) != 1) throw invalid("初始化工艺作废失败");
            }
        }
    }

    public void bindCandidate(MesProCraftBindReqVO request) {
        lockCatalog();
        MesProCraftDO craft = require(request.getCraftId());
        assertNoPending(craft.getId());
        MesProRouteVersionDO version = versionMapper.selectById(request.getRouteVersionId());
        if (version == null) throw invalid("流程版本不存在");
        assertEditable(version);
        JSONObject root = JSON.parseObject(version.getRouteSnapshotJson());
        JSONObject config = configs(root).stream().filter(c -> Objects.equals(request.getRouteProcessId(),
                c.getLong("routeProcessId"))).findFirst().orElseThrow(() -> invalid("流程工序不存在"));
        if (!Objects.equals(craft.getProcessId(), config.getLong("processId"))) throw invalid("只能选择当前工序所属的工艺");
        List<Long> deviceIds = devices(config);
        // 已有逐设备参数时保留原参数设备集合，绝不给原本无参数的设备增加填写项。
        if (!config.getJSONArray("parameterRules").isEmpty()) deviceIds = extract(config).deviceIds();
        bind(config, craft.getId(), craft.getCode(), JSON.parseArray(craft.getParametersJson()), deviceIds);
        candidateConfig.getObject().saveConfigSnapshots(version.getId(), request.getExpectedRouteSnapshotSha256(),
                Map.of("productionProcessConfigSchemaVersion", 1,
                        "productionProcessConfigs", root.getJSONObject("configSnapshots").getJSONArray("productionProcessConfigs")));
    }

    /** 所有路线候选配置写入口校验引用和展开结果，防止绕过工艺库单独修改参数。 */
    public void validateBindings(JSONObject snapshots) {
        JSONArray values = snapshots.getJSONArray("productionProcessConfigs");
        if (values == null) return;
        for (Object raw : values) {
            JSONObject config = (JSONObject) raw;
            Long id = config.getLong("craftId");
            if (id == null) continue; // 未迁移的既有配置仍按既有规则处理，不改变生产链路。
            MesProCraftDO craft = require(id);
            JSONArray parameters = JSON.parseArray(craft.getParametersJson());
            // 整批构建期间采用待确认内容，正式工艺直到全部发布完成才更新。
            for (MesProCraftChangeDO change : changes()) {
                if (Objects.equals(id, change.getCraftId()) && Set.of("PREPARING", "APPLYING").contains(change.getStatus())) {
                    parameters = JSON.parseArray(JSON.parseObject(change.getAfterJson()).getString("parametersJson"));
                }
            }
            if (!Objects.equals(craft.getProcessId(), config.getLong("processId"))
                    || !Objects.equals(fingerprint(craft.getProcessId(), parameters), config.getString("craftFingerprint")))
                throw invalid("工艺归属或参数内容已变化，请重新选择工艺");
            JSONArray deviceIds = config.getJSONArray("craftDeviceIds");
            if (deviceIds == null || !sameRules(config.getJSONArray("parameterRules"),
                    expand(config, parameters, deviceIds.toJavaList(Long.class))))
                throw invalid("流程参数必须与所选工艺及设备关联一致");
        }
    }
    /** 普通配置保存不能摘除已有引用；删除整个工序节点仍由原有路线编辑规则控制。 */
    public void validateBindingTransitions(JSONObject previous, JSONObject next) {
        if (previous == null || previous.getJSONArray("productionProcessConfigs") == null) return;
        Map<Long, JSONObject> nodes = new HashMap<>();
        JSONArray values = next.getJSONArray("productionProcessConfigs");
        if (values != null) for (Object raw : values) {
            JSONObject node = (JSONObject) raw;
            nodes.put(node.getLong("routeProcessId"), node);
        }
        for (Object raw : previous.getJSONArray("productionProcessConfigs")) {
            JSONObject old = (JSONObject) raw, current = nodes.get(old.getLong("routeProcessId"));
            if (old.getLong("craftId") != null && current != null && current.getLong("craftId") == null)
                throw invalid("已绑定工艺的工序不能移除工艺引用，请选择对应工序的工艺");
        }
    }

    // 尚无正式生产配置的历史版本不可能通过本模块绑定工艺，因此不参与引用扫描。
    private List<JSONObject> bindingConfigs(JSONObject root) {
        if (root == null) throw invalid("流程版本快照缺失");
        JSONObject data = root.getJSONObject("configSnapshots");
        if (data == null || !data.containsKey("productionProcessConfigs")) return List.of();
        return configs(root);
    }
    private boolean sameRules(JSONArray left, JSONArray right) {
        if (left == null || right == null) return false;
        Comparator<JSONObject> order = Comparator.comparing((JSONObject p) -> p.getLong("deviceId"))
                .thenComparing(p -> p.getString("parameterCode"));
        List<JSONObject> a = new ArrayList<>(), b = new ArrayList<>();
        for (Object raw : left) a.add(normalizedExecutionRule((JSONObject) raw));
        for (Object raw : right) b.add(normalizedExecutionRule((JSONObject) raw));
        a.sort(order); b.sort(order);
        return canonical(a).equals(canonical(b));
    }
    private JSONObject normalizedExecutionRule(JSONObject rule) {
        JSONObject result = new JSONObject(true);
        JSONObject definition = new JSONObject(true);
        for (String key : List.of("parameterCode","parameterName","unit","valueType","standardText",
                "lowerLimit","upperLimit","defaultValue","optionValuesJson","defaultText","decimalScale","sort"))
            definition.put(key, rule.get(key));
        result.putAll((JSONObject) normalize(new JSONArray(List.of(definition))).get(0));
        for (String key : List.of("deviceId","routeProcessId","processId")) result.put(key, rule.get(key));
        return result;
    }
    private List<JSONObject> configs(JSONObject root) {
        JSONObject data = root == null ? null : root.getJSONObject("configSnapshots");
        JSONArray configs = data == null ? null : data.getJSONArray("productionProcessConfigs");
        if (configs == null) throw invalid("流程缺少正式生产配置，请先完成现有配置迁移");
        return configs.toJavaList(JSONObject.class);
    }
    private String routeLabel(MesProRouteVersionDO version) {
        JSONObject root = JSON.parseObject(version.getRouteSnapshotJson());
        return root.getString("routeName") + "（" + version.getRouteId() + "）";
    }
}
