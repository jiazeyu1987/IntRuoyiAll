package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesSharedChangeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteSnapshotCanonicalizer;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigServiceImpl;
import com.alibaba.fastjson.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesSharedRouteSnapshotChange.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class MesProSharedChangeService implements cn.iocoder.yudao.module.mes.service.common.MesSharedMasterDataChangeGuard {
    @Resource private MesProCraftService crafts;
    @Resource private MesProCraftMapper craftMapper;
    @Resource private MesProCraftChangeMapper changeMapper;
    @Resource private MesProProcessMapper processMapper;
    @Resource private MesDvMachineryMapper machineryMapper;
    @Resource private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Resource private MesProRouteVersionMapper versionMapper;
    @Resource private ObjectProvider<MesProProcessService> processes;
    @Resource private ObjectProvider<MesDvMachineryService> machinery;
    @Resource private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();
    private final ThreadLocal<String> applyingSource = new ThreadLocal<>();

    @Override
    public void assertDirectUpdateAllowed(String type, Long id) {
        crafts.lockCatalog();
        if (Objects.equals(type + ":" + id, applyingSource.get())) return;
        assertNoPending(type, id, null);
        JSONObject source = readSource(type, id);
        Set<Long> deviceIds = "DEVICE".equals(type) ? executionIds(source.getString("code")) : Set.of();
        if (!references(type, id, deviceIds).isEmpty()) throw invalid("该资料已被工艺路线引用，请通过影响确认提交变更");
        for (MesProRouteVersionDO open : versionMapper.selectList(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .in(MesProRouteVersionDO::getLifecycleStatus, List.of("DRAFT", "PENDING_APPROVAL", "READY_TO_PUBLISH")))) {
            if (uses(JSON.parseObject(open.getRouteSnapshotJson()), type, id, deviceIds))
                throw invalid("该资料被未完成的路线版本引用，请先处理候选版本");
        }
    }

    public Map<String, Object> source(String type, Long id) {
        JSONObject data = readSource(type, id);
        return Map.of("data", data, "fingerprint", hash(data));
    }

    public List<JSONObject> options(String type, Long sourceId) {
        JSONObject source = readSource(type, sourceId);
        List<?> rows = switch (type) {
            case "PROCESS" -> processMapper.selectList(new LambdaQueryWrapperX<cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO>()
                    .eq(cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO::getStatus, 0));
            case "DEVICE" -> machineryMapper.selectList();
            case "CRAFT" -> crafts.list(source.getLong("processId"));
            default -> throw invalid("变更对象类型无效");
        };
        List<JSONObject> result = new ArrayList<>();
        for (Object raw : rows) {
            JSONObject row = (JSONObject) JSON.toJSON(raw);
            if (!Objects.equals(sourceId, row.getLong("id"))) result.add(row);
        }
        return result;
    }

    public List<MesProCraftDO> parameterOptions(Long processId) { return crafts.list(processId); }

    private JSONObject readSource(String type, Long id) {
        Object row = switch (type) {
            case "PROCESS" -> processMapper.selectById(id);
            case "CRAFT" -> craftMapper.selectById(id);
            case "DEVICE" -> machineryMapper.selectById(id);
            default -> throw invalid("变更对象类型无效");
        };
        if (row == null) throw invalid("变更资料不存在，请刷新");
        return (JSONObject) JSON.toJSON(row);
    }

    public Map<String, Object> preview(MesSharedChangeReqVO request) {
        crafts.lockCatalog();
        Plan plan = plan(request);
        return plan.result;
    }

    private Plan plan(MesSharedChangeReqVO request) {
        JSONObject before = readSource(request.getSubjectType(), request.getSourceId());
        if (!Objects.equals(request.getExpectedFingerprint(), hash(before))) throw invalid("资料已变化，请重新加载后确认");
        JSONObject after;
        if ("REPLACE".equals(request.getAction())) {
            if (request.getTargetId() == null || Objects.equals(request.getSourceId(), request.getTargetId()))
                throw invalid("请选择其他替换对象");
            after = readSource(request.getSubjectType(), request.getTargetId());
        } else if ("UPDATE".equals(request.getAction())) {
            Object update = switch (request.getSubjectType()) {
                case "PROCESS" -> request.getProcess();
                case "DEVICE" -> request.getDevice();
                case "CRAFT" -> throw invalid("工艺修改请使用工艺库的影响确认入口");
                default -> throw invalid("变更对象类型无效");
            };
            if (update == null) throw invalid("缺少修改内容");
            after = JSON.parseObject(before.toJSONString());
            JSONObject fields = (JSONObject) JSON.toJSON(update);
            if (!Objects.equals(request.getSourceId(), fields.getLong("id"))) throw invalid("修改对象身份不一致");
            after.putAll(fields);
        } else throw invalid("变更方式无效");
        if ("CRAFT".equals(request.getSubjectType()) && !Objects.equals(before.getLong("processId"), after.getLong("processId")))
            throw invalid("替换工艺必须属于同一工序");
        List<String> blockers = new ArrayList<>();
        assertNoPending(request.getSubjectType(), request.getSourceId(), request.getTargetId());
        Set<Long> deviceIds = "DEVICE".equals(request.getSubjectType()) ? executionIds(before.getString("code")) : Set.of();
        Long targetDeviceId = null;
        if ("DEVICE".equals(request.getSubjectType()) && "REPLACE".equals(request.getAction())) {
            Set<Long> targets = executionIds(after.getString("code"));
            if (targets.size() != 1) throw invalid("目标设备须有唯一的执行设备身份，请先维护设备关联");
            targetDeviceId = targets.iterator().next();
            var device = deviceMapper.selectById(targetDeviceId);
            if (!Boolean.TRUE.equals(device.getEnabled()) || !"ENABLED".equals(device.getDeviceStatus()))
                throw invalid("目标执行设备未启用");
        }
        List<MesProRouteVersionDO> refs = references(request.getSubjectType(), request.getSourceId(), deviceIds);
        if (!refs.isEmpty() && "PROCESS".equals(request.getSubjectType()) && !Objects.equals(after.getInteger("status"), 0))
            throw invalid("关联流程的工序必须启用，请先替换引用后再停用");
        JSONArray impacts = new JSONArray();
        for (MesProRouteVersionDO version : refs) {
            JSONObject snapshot = JSON.parseObject(version.getRouteSnapshotJson());
            JSONObject impact = new JSONObject(true);
            impact.put("routeId", version.getRouteId()); impact.put("routeName", snapshot.getString("routeName"));
            impact.put("routeCode", snapshot.getString("routeCode")); impact.put("routeVersionId", version.getId());
            impact.put("versionNo", version.getVersionNo()); impact.put("snapshotSha256", version.getRouteSnapshotSha256());
            String maxVersion = versionMapper.selectMaxVersionNoByRouteId(version.getRouteId());
            if (maxVersion == null || !maxVersion.matches("V[0-9]+")) throw invalid("路线版本编号无效");
            impact.put("nextVersionNo", "V" + Math.addExact(Integer.parseInt(maxVersion.substring(1)), 1));
            impact.put("nodes", affectedNodes(snapshot, request, deviceIds));
            impacts.add(impact);
        }
        // 检查所有开放候选，包含尚无正式版本的路线；不覆盖用户已有草稿。
        for (MesProRouteVersionDO open : versionMapper.selectList(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .in(MesProRouteVersionDO::getLifecycleStatus, List.of("DRAFT", "PENDING_APPROVAL", "READY_TO_PUBLISH")))) {
            boolean activeUsed = refs.stream().anyMatch(v -> Objects.equals(v.getRouteId(), open.getRouteId()));
            if (activeUsed || uses(JSON.parseObject(open.getRouteSnapshotJson()), request.getSubjectType(), request.getSourceId(), deviceIds))
                blockers.add("路线 " + open.getRouteId() + " 已有未完成版本 " + open.getVersionNo());
        }
        Set<String> selected = new HashSet<>();
        for (var node : request.getNodes()) {
            if (!selected.add(node.getRouteId() + ":" + node.getRouteProcessId())) throw invalid("工序参数选择重复");
            boolean found = impacts.toJavaList(JSONObject.class).stream().anyMatch(r -> Objects.equals(r.getLong("routeId"), node.getRouteId())
                    && r.getJSONArray("nodes").toJavaList(JSONObject.class).stream().anyMatch(n -> Objects.equals(n.getLong("routeProcessId"), node.getRouteProcessId())));
            if (!found) throw invalid("所选工序已不在影响范围，请重新预览");
        }
        Plan plan = new Plan(request, before, after, deviceIds, targetDeviceId, refs, new LinkedHashMap<>());
        Map<Long, JSONObject> workshops = workshops(impacts);
        for (JSONObject impact : impacts.toJavaList(JSONObject.class)) for (JSONObject node : impact.getJSONArray("nodes").toJavaList(JSONObject.class)) {
            Long workstationId = node.getLong("routeProcessWorkstationId");
            JSONObject workshop = workstationId == null ? null : workshops.get(workstationId);
            if (workshop != null) node.putAll(workshop);
        }
        JSONObject transformedHashes = new JSONObject(true);
        int pendingSelectionCount = 0;
        for (MesProRouteVersionDO version : refs) {
            try {
                JSONObject copy = JSON.parseObject(version.getRouteSnapshotJson());
                int missing = missingSelections(plan, version.getRouteId(), copy);
                pendingSelectionCount += missing;
                if (missing > 0) continue;
                transform(plan, version.getRouteId(), copy);
                MesProRouteCandidateConfigServiceImpl.validateProductionProcessConfigs(version.getId(), copy.getJSONObject("configSnapshots"));
                transformedHashes.put(version.getRouteId().toString(), hash(copy));
            } catch (cn.iocoder.yudao.framework.common.exception.ServiceException error) {
                blockers.add("路线 " + version.getRouteId() + "：" + error.getMessage());
            }
        }
        plan.result.put("before", before); plan.result.put("after", after);
        plan.result.put("routes", impacts); plan.result.put("blockers", blockers);
        plan.result.put("pendingSelectionCount", pendingSelectionCount);
        JSONObject identity = new JSONObject(true);
        JSONObject previewRequest = requestContent(request);
        previewRequest.remove("reason");
        identity.put("request", previewRequest); identity.put("preview", new LinkedHashMap<>(plan.result));
        identity.put("candidateHashes", transformedHashes);
        plan.result.put("impactToken", hash(identity));
        return plan;
    }

    private int missingSelections(Plan plan, Long routeId, JSONObject snapshot) {
        if (!"REPLACE".equals(plan.request.getAction())) return 0;
        int missing = 0;
        for (JSONObject config : configurations(snapshot)) {
            boolean required = switch (plan.request.getSubjectType()) {
                case "PROCESS" -> Objects.equals(plan.request.getSourceId(), config.getLong("processId"))
                        && !config.getJSONArray("parameterRules").isEmpty();
                case "DEVICE" -> devices(config).stream().anyMatch(plan.deviceIds::contains);
                default -> false;
            };
            if (required && plan.request.getNodes().stream().noneMatch(n -> Objects.equals(routeId, n.getRouteId())
                    && Objects.equals(config.getLong("routeProcessId"), n.getRouteProcessId()))) missing++;
        }
        return missing;
    }

    private Map<Long, JSONObject> workshops(JSONArray impacts) {
        Set<Long> ids = new HashSet<>();
        for (JSONObject impact : impacts.toJavaList(JSONObject.class)) for (JSONObject node : impact.getJSONArray("nodes").toJavaList(JSONObject.class)) {
            Long id = node.getLong("routeProcessWorkstationId"); if (id != null) ids.add(id);
        }
        if (ids.isEmpty()) return Map.of();
        List<JSONObject> rows = jdbc.query("""
                SELECT s.id,w.id workshop_id,w.name workshop_name FROM mes_md_workstation s
                JOIN mes_md_workshop w ON w.id=s.workshop_id AND w.tenant_id=s.tenant_id AND w.deleted=0
                WHERE s.tenant_id=:tenantId AND s.deleted=0 AND s.id IN (:ids)
                """, Map.of("tenantId", cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId(), "ids", ids), (rs, i) -> {
            JSONObject row = new JSONObject(true); row.put("workstationId", rs.getLong("id"));
            row.put("workshopId", rs.getLong("workshop_id")); row.put("workshopName", rs.getString("workshop_name")); return row;
        });
        return rows.stream().collect(Collectors.toMap(r -> r.getLong("workstationId"), r -> r));
    }

    public MesProCraftChangeDO submit(MesSharedChangeReqVO request) {
        crafts.lockCatalog();
        if (request.getReason() == null || request.getReason().isBlank()) throw invalid("请填写变更原因");
        try { UUID.fromString(request.getRequestId()); } catch (IllegalArgumentException e) { throw invalid("变更请求编号无效"); }
        String requestHash = hash(requestContent(request));
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        for (MesProCraftChangeDO previous : sharedChanges()) {
            JSONObject metadata = JSON.parseObject(previous.getAfterJson());
            if (Objects.equals(metadata.getString("requestId"), request.getRequestId())) {
                if (!Objects.equals(actor, metadata.getLong("actorId")) || !Objects.equals(requestHash, metadata.getString("requestHash")))
                    throw invalid("请求编号已用于其他变更");
                return previous;
            }
        }
        Plan plan = plan(request);
        if (!Objects.equals(request.getImpactToken(), plan.result.get("impactToken"))) throw invalid("影响范围已变化，请重新确认");
        if ((Integer) plan.result.get("pendingSelectionCount") > 0) throw invalid("请选择全部工序的替换参数");
        @SuppressWarnings("unchecked") List<String> blockers = (List<String>) plan.result.get("blockers");
        if (!blockers.isEmpty()) throw invalid(String.join("；", blockers));
        JSONObject proposal = new JSONObject(true);
        proposal.put("request", request); proposal.put("requestId", request.getRequestId());
        proposal.put("requestHash", requestHash); proposal.put("actorId", actor);
        proposal.put("data", plan.after); proposal.put("routes", plan.result.get("routes"));
        proposal.put("sourceDeviceIds", plan.deviceIds);
        proposal.put("targetDeviceId", plan.targetDeviceId);
        return crafts.submitShared(plan.before.toJSONString(), proposal, request.getReason(), plan.references,
                (active, snapshot) -> transform(plan, active.getRouteId(), snapshot));
    }

    private JSONObject requestContent(MesSharedChangeReqVO request) {
        JSONObject content = (JSONObject) JSON.toJSON(request);
        content.remove("impactToken");
        return content;
    }

    private void transform(Plan plan, Long routeId, JSONObject snapshot) {
        var request = plan.request;
        if ("PROCESS".equals(request.getSubjectType())) {
            List<JSONObject> configs = configurations(snapshot).stream()
                    .filter(c -> Objects.equals(request.getSourceId(), c.getLong("processId"))).toList();
            process(snapshot, request.getSourceId(), plan.after);
            for (JSONObject node : nodes(snapshot)) if (Objects.equals(plan.after.getLong("id"), node.getLong("processId"))) {
                node.put("processName", plan.after.getString("name")); node.put("processCode", plan.after.getString("code"));
                node.put("attention", plan.after.get("attention")); node.put("sopFileUrl", plan.after.get("sopFileUrl"));
            }
            if ("REPLACE".equals(request.getAction())) for (JSONObject config : configs) {
                if (config.getJSONArray("parameterRules").isEmpty()) continue;
                var selection = requireSelection(request, routeId, config.getLong("routeProcessId"));
                List<Binding> bindings = new ArrayList<>();
                for (var item : selection.getCrafts()) {
                    MesProCraftDO target = crafts.require(item.getCraftId());
                    if (!Objects.equals(target.getProcessId(), plan.after.getLong("id"))) throw invalid("请选择新工序所属工艺");
                    bindings.add(new Binding(target.getId(), target.getCode(), JSON.parseArray(target.getParametersJson()), item.getDeviceIds()));
                }
                processBindings(config, bindings);
            }
        } else if ("CRAFT".equals(request.getSubjectType())) {
            for (JSONObject config : configurations(snapshot)) if (usesCraft(config, request.getSourceId()))
                craft(config, request.getSourceId(), plan.after.toJavaObject(MesProCraftDO.class));
        } else if ("REPLACE".equals(request.getAction())) {
            for (JSONObject config : configurations(snapshot)) if (devices(config).stream().anyMatch(plan.deviceIds::contains)) {
                var selection = requireSelection(request, routeId, config.getLong("routeProcessId"));
                MesProCraftDO target = selection.getDeviceCraftId() == null ? null : crafts.require(selection.getDeviceCraftId());
                device(config, plan.deviceIds, plan.targetDeviceId, target, selection.isWithoutParameters());
            }
        }
        JSONObject data = snapshot.getJSONObject("configSnapshots");
        JSONObject sources = data.getJSONObject("sharedSourceSnapshots");
        if (sources == null) { sources = new JSONObject(true); data.put("sharedSourceSnapshots", sources); }
        sources.put(request.getSubjectType() + ":" + request.getSourceId(), plan.after);
    }

    private MesSharedChangeReqVO.NodeSelection requireSelection(MesSharedChangeReqVO request, Long routeId, Long nodeId) {
        return request.getNodes().stream().filter(n -> Objects.equals(routeId, n.getRouteId()) && Objects.equals(nodeId, n.getRouteProcessId()))
                .findFirst().orElseThrow(() -> invalid("请选择工序 " + nodeId + " 的替换参数"));
    }

    private JSONArray affectedNodes(JSONObject snapshot, MesSharedChangeReqVO request, Set<Long> deviceIds) {
        JSONArray result = new JSONArray();
        for (JSONObject node : nodes(snapshot)) {
            JSONObject config = configurations(snapshot).stream().filter(c -> Objects.equals(c.getLong("routeProcessId"), node.getLong("routeProcessId"))).findFirst().orElse(null);
            boolean used = switch (request.getSubjectType()) {
                case "PROCESS" -> Objects.equals(request.getSourceId(), node.getLong("processId"));
                case "CRAFT" -> config != null && usesCraft(config, request.getSourceId());
                case "DEVICE" -> config != null && devices(config).stream().anyMatch(deviceIds::contains);
                default -> false;
            };
            if (used) {
                JSONObject row = JSON.parseObject(node.toJSONString());
                if (config != null && "DEVICE".equals(request.getSubjectType())) {
                    List<Long> affectedIds = devices(config).stream().filter(deviceIds::contains).toList();
                    row.put("affectedDeviceIds", affectedIds);
                    JSONArray selectedRules = new JSONArray();
                    for (Object raw : config.getJSONArray("parameterRules")) {
                        JSONObject rule = (JSONObject) raw;
                        if (affectedIds.contains(rule.getLong("deviceId"))) selectedRules.add(raw);
                    }
                    JSONObject projected = JSON.parseObject(config.toJSONString());
                    projected.put("parameterRules", selectedRules);
                    row.put("productionConfig", projected);
                } else row.put("productionConfig", config);
                result.add(row);
            }
        }
        return result;
    }

    private Set<Long> executionIds(String code) {
        if (code == null || code.isBlank()) throw invalid("设备编码缺失");
        return deviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                .eq(MesProcessPoolTeamDeviceDO::getDeviceCode, code.trim())).stream()
                .map(MesProcessPoolTeamDeviceDO::getId).collect(Collectors.toCollection(TreeSet::new));
    }

    private List<MesProRouteVersionDO> references(String type, Long id, Set<Long> devices) {
        return versionMapper.selectActiveListWithExistingRoutes().stream()
                .filter(v -> uses(JSON.parseObject(v.getRouteSnapshotJson()), type, id, devices))
                .sorted(Comparator.comparing(MesProRouteVersionDO::getRouteId)).toList();
    }

    private List<MesProCraftChangeDO> sharedChanges() {
        return changeMapper.selectList(new LambdaQueryWrapperX<MesProCraftChangeDO>().eq(MesProCraftChangeDO::getChangeType, "SHARED"));
    }

    public void assertNoPending(String type, Long sourceId, Long targetId) {
        for (MesProCraftChangeDO change : sharedChanges()) {
            if (!Set.of("PREPARING", "PENDING", "APPLYING").contains(change.getStatus())) continue;
            var request = JSON.parseObject(change.getAfterJson()).getJSONObject("request");
            if (!Objects.equals(type, request.getString("subjectType"))) continue;
            Set<Long> affected = new HashSet<>(Arrays.asList(request.getLong("sourceId"), request.getLong("targetId")));
            if (affected.contains(sourceId) || targetId != null && affected.contains(targetId)) throw invalid("该资料已有未完成的整批变更");
        }
        if ("CRAFT".equals(type)) for (MesProCraftChangeDO change : crafts.changes()) {
            if (Set.of("PREPARING", "PENDING", "APPLYING").contains(change.getStatus())
                    && (Objects.equals(sourceId, change.getCraftId()) || targetId != null && Objects.equals(targetId, change.getCraftId())))
                throw invalid("该工艺已有未完成的整批变更");
        }
    }

    public void verifyBeforePublish(MesProCraftChangeDO change) {
        JSONObject proposal = JSON.parseObject(change.getAfterJson());
        MesSharedChangeReqVO request = proposal.getObject("request", MesSharedChangeReqVO.class);
        if (!Objects.equals(hash(JSON.parseObject(change.getBeforeJson())), hash(readSource(request.getSubjectType(), request.getSourceId()))))
            throw invalid("原资料已变化，请作废本次变更并重新确认");
        if ("REPLACE".equals(request.getAction()) && !Objects.equals(hash(proposal.getJSONObject("data")), hash(readSource(request.getSubjectType(), request.getTargetId()))))
            throw invalid("替换资料已变化，请作废本次变更并重新确认");
        Set<Long> devices = new HashSet<>(proposal.getJSONArray("sourceDeviceIds").toJavaList(Long.class));
        Set<Long> expected = proposal.getJSONArray("routes").toJavaList(JSONObject.class).stream().map(r -> r.getLong("routeVersionId")).collect(Collectors.toSet());
        Set<Long> actual = references(request.getSubjectType(), request.getSourceId(), devices).stream().map(MesProRouteVersionDO::getId).collect(Collectors.toSet());
        if (!expected.equals(actual)) throw invalid("引用范围已变化，请作废本次变更并重新确认");
        if ("DEVICE".equals(request.getSubjectType()) && !devices.equals(executionIds(JSON.parseObject(change.getBeforeJson()).getString("code"))))
            throw invalid("原设备执行身份已变化，请重新确认");
        if ("DEVICE".equals(request.getSubjectType()) && "REPLACE".equals(request.getAction())) {
            Long targetDeviceId = proposal.getLong("targetDeviceId");
            Set<Long> currentTargets = executionIds(proposal.getJSONObject("data").getString("code"));
            if (targetDeviceId == null || !currentTargets.equals(Set.of(targetDeviceId)))
                throw invalid("目标设备执行身份已变化，请重新确认");
            var targetDevice = deviceMapper.selectById(targetDeviceId);
            if (targetDevice == null || !Boolean.TRUE.equals(targetDevice.getEnabled())
                    || !"ENABLED".equals(targetDevice.getDeviceStatus()))
                throw invalid("目标执行设备未启用，不能发布本次变更");
        }
        Set<Long> ownCandidates = new HashSet<>(JSON.parseArray(change.getCandidateIdsJson()).toJavaList(Long.class));
        for (MesProRouteVersionDO open : versionMapper.selectList(new LambdaQueryWrapperX<MesProRouteVersionDO>()
                .in(MesProRouteVersionDO::getLifecycleStatus, List.of("DRAFT", "PENDING_APPROVAL", "READY_TO_PUBLISH")))) {
            if (!ownCandidates.contains(open.getId())
                    && uses(JSON.parseObject(open.getRouteSnapshotJson()), request.getSubjectType(), request.getSourceId(), devices))
                throw invalid("路线 " + open.getRouteId() + " 新增了引用该资料的未完成版本，请重新确认");
        }
    }

    public void apply(MesProCraftChangeDO change) {
        JSONObject proposal = JSON.parseObject(change.getAfterJson());
        MesSharedChangeReqVO request = proposal.getObject("request", MesSharedChangeReqVO.class);
        if (!"UPDATE".equals(request.getAction())) return;
        if (applyingSource.get() != null) throw invalid("共享资料变更执行状态冲突");
        applyingSource.set(request.getSubjectType() + ":" + request.getSourceId());
        try {
        if ("PROCESS".equals(request.getSubjectType())) processes.getObject().updateProcess(request.getProcess());
        else if ("DEVICE".equals(request.getSubjectType())) {
            machinery.getObject().updateMachinery(request.getDevice());
            for (Long id : proposal.getJSONArray("sourceDeviceIds").toJavaList(Long.class)) {
                var device = deviceMapper.selectById(id);
                if (device == null) throw invalid("原执行设备已不存在");
                device.setDeviceCode(request.getDevice().getCode()); device.setDeviceName(request.getDevice().getName());
                if (deviceMapper.updateById(device) != 1) throw invalid("执行设备资料更新失败");
            }
        }
        } finally {
            applyingSource.remove();
        }
    }

    private String hash(Object object) { return canonicalizer.sha256(JSON.toJSONString(object)); }
    private record Plan(MesSharedChangeReqVO request, JSONObject before, JSONObject after,
                        Set<Long> deviceIds, Long targetDeviceId, List<MesProRouteVersionDO> references, Map<String, Object> result) {}
}
