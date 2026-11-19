package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import com.alibaba.fastjson.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;
import static cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class MesProRouteTreeService {
    @Resource private MesProRouteMapper routes;
    @Resource private MesProRouteVersionMapper versions;
    @Resource private ObjectProvider<MesProCraftService> crafts;
    @Resource private ObjectProvider<MesProRouteService> routeService;
    @Resource private MesProRouteVersionPublishProjectionServiceImpl projection;
    @Resource private MesProRouteDeviceIdentitySnapshotService devices;
    @Resource private MesProRouteControlledContentAdapter platform;
    @Resource private MesProRouteOwnerPermissionService owners;
    private final MesProRouteSnapshotCanonicalizer canonicalizer = new MesProRouteSnapshotCanonicalizer();

    public boolean isGroup(Long id) { return "GROUP".equals(requireRoute(id).getNodeType()); }
    public List<Map<String, Object>> tree() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<MesProRouteDO> routeList = routes.selectList();
        Map<Long, String> productCodes = routeService.getObject().getProductCodesByRouteIds(routeList.stream()
                .filter(route -> "FLOW".equals(route.getNodeType())).map(MesProRouteDO::getId).toList());
        for (MesProRouteDO route : routeList) {
            MesProRouteVersionDO active = versions.selectActiveByRouteId(route.getId());
            MesProRouteVersionDO candidate = versions.selectOpenCandidateByRouteId(route.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", route.getId()); row.put("parentId", route.getParentId());
            row.put("nodeType", route.getNodeType()); row.put("name", route.getName()); row.put("code", route.getCode());
            row.put("productCodes", "FLOW".equals(route.getNodeType()) ? productCodes.get(route.getId()) : null);
            row.put("status", route.getStatus()); row.put("activeRouteVersionId", active == null ? null : active.getId());
            row.put("activeRouteVersionNo", active == null ? null : active.getVersionNo());
            row.put("pendingStatus", candidate == null ? null : candidate.getLifecycleStatus());
            row.put("processIds", structure(active));
            row.put("different", route.getParentId() != null && !structure(active).equals(structure(active(route.getParentId()))));
            result.add(row);
        }
        return result;
    }
    public Long createGroup(String name, Long parentId, Long sourceRouteId) {
        crafts.getObject().lockCatalog();
        if (name == null || name.isBlank()) throw invalid("请填写分组名称");
        if (routes.selectByName(name.trim()) != null) throw invalid("名称已存在");
        if (parentId != null) { requireGroup(parentId); assertMutable(parentId); }
        Long sourceId = parentId == null ? sourceRouteId : parentId;
        if (sourceId == null) throw invalid("请选择分组的初始工艺模板");
        JSONObject source = snapshot(active(sourceId));
        MesProRouteDO group = MesProRouteDO.builder().name(name.trim())
                .code("GROUP-" + UUID.randomUUID().toString().replace("-", ""))
                .parentId(parentId).nodeType("GROUP").status(1).build();
        if (routes.insert(group) != 1) throw invalid("分组保存失败");
        source.put("routeId", group.getId()); source.put("routeCode", group.getCode());
        source.put("routeName", group.getName()); source.put("name", group.getName()); source.put("status", 1);
        JSONObject configs = source.getJSONObject("configSnapshots");
        configs.put("products", new JSONArray()); configs.put("productBoms", new JSONArray()); configs.remove("craftChangeId");
        MesProRouteVersionDO version = MesProRouteVersionDO.builder().routeId(group.getId()).versionNo("V1")
                .active(true).lifecycleStatus(STATUS_ACTIVE).build();
        MesProRouteVersionSnapshotIdentityWriter.apply(version, source.toJSONString());
        versions.insert(version);
        finishInitialVersion(version);
        owners.bindCurrentUserAsOwner(group.getId());
        return group.getId();
    }
    public Long createInherited(Long parentId, MesProRouteCreateReqVO request) {
        crafts.getObject().lockCatalog(); requireGroup(parentId); assertMutable(parentId);
        JSONObject template = snapshot(active(parentId));
        request.setProcessIds(MesProRouteTreePatch.order(template));
        return routeService.getObject().createRouteWithInheritedConfig(request, parentId, template);
    }
    public MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult projectInitialVersion(MesProRouteVersionDO version) {
        return projection.projectCandidate(version);
    }
    private void finishInitialVersion(MesProRouteVersionDO version) {
        MesProRouteVersionPublishProjectionServiceImpl.ProjectionResult result = projection.projectCandidate(version);
        MesProRouteVersionSnapshotIdentityWriter.apply(version, result.rewrittenSnapshotJson());
        if (versions.updateById(version) != 1) throw invalid("初始配置保存失败");
        devices.captureCreated(version);
        platform.recordActiveRegisteredIfMissing(version, SecurityFrameworkUtils.getLoginUserId(), "树形工艺模板初始化");
    }
    public Map<String, Object> movePreview(Long id, Long parentId) {
        MesProRouteDO route = requireRoute(id);
        if (parentId != null) requireGroup(parentId);
        if (Objects.equals(id, parentId) || descendants(id).stream().anyMatch(item -> Objects.equals(item.getId(), parentId)))
            throw invalid("不能移入自身或下级分组");
        List<Long> own = structure(active(id)), expected = parentId == null ? own : structure(active(parentId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("different", !own.equals(expected));
        result.put("currentProcessIds", own); result.put("expectedProcessIds", expected);
        result.put("currentProcesses", processNames(snapshot(active(id))));
        result.put("expectedProcesses", parentId == null ? List.of() : processNames(snapshot(active(parentId))));
        result.put("fingerprint", canonicalizer.sha256(JSON.toJSONString(Arrays.asList(id, parentId, own, expected,
                route.getParentId(), active(id).getRouteSnapshotSha256(),
                parentId == null ? null : active(parentId).getRouteSnapshotSha256()))));
        return result;
    }
    public void move(Long id, Long parentId, String fingerprint, boolean confirmDifference) {
        crafts.getObject().lockCatalog(); assertMutable(id);
        if (parentId != null) assertMutable(parentId);
        for (MesProRouteDO child : descendants(id)) assertMutable(child.getId());
        Map<String, Object> preview = movePreview(id, parentId);
        if (!Objects.equals(fingerprint, preview.get("fingerprint"))) throw invalid("流程结构已变化，请重新确认");
        if (Boolean.TRUE.equals(preview.get("different")) && !confirmDifference) throw invalid("工序结构不同，确认后才能添加到此分组");
        // 显式置空必须写入数据库；MyBatis 默认忽略 null 更新值。
        routes.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProRouteDO>()
                .eq(MesProRouteDO::getId, id).set(MesProRouteDO::getParentId, parentId));
    }
    public Map<String, Object> preview(Long candidateId) {
        crafts.getObject().lockCatalog();
        Plan plan = plan(candidateId);
        return plan.preview;
    }
    public MesProRouteVersionDO submit(Long candidateId, String expectedFingerprint, String reason) {
        crafts.getObject().lockCatalog();
        Plan plan = plan(candidateId);
        if (!Objects.equals(expectedFingerprint, plan.preview.get("fingerprint"))) throw invalid("待发布内容或影响范围已变化，请重新预览");
        if (reason == null || reason.isBlank()) throw invalid("请填写本次统一修改原因");
        if (!plan.blockers.isEmpty()) throw invalid(String.join("；", plan.blockers));
        crafts.getObject().submitTree(candidateId, plan.snapshots, reason, plan.routeIds, plan.preview);
        return versions.selectById(candidateId);
    }
    private Plan plan(Long candidateId) {
        MesProRouteVersionDO candidate = versions.selectById(candidateId);
        if (candidate == null || !STATUS_DRAFT.equals(candidate.getLifecycleStatus()) || candidate.getActive()
                || !isGroup(candidate.getRouteId())) throw invalid("请选择父节点的草稿版本");
        crafts.getObject().assertEditable(candidate);
        MesProRouteVersionDO current = active(candidate.getRouteId());
        if (!Objects.equals(current.getId(), candidate.getSourceRouteVersionId())) throw invalid("父节点正式版本已变化");
        List<MesProRouteDO> children = descendants(candidate.getRouteId());
        Map<Long, JSONObject> snapshots = new LinkedHashMap<>();
        List<Long> routeIds = children.stream().map(MesProRouteDO::getId).toList();
        snapshots.put(candidate.getRouteId(), snapshot(candidate));
        List<String> blockers = new ArrayList<>();
        List<Map<String, Object>> affected = new ArrayList<>();
        for (MesProRouteDO child : children) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("routeId", child.getId()); row.put("name", child.getName()); row.put("nodeType", child.getNodeType());
            try {
                MesProRouteVersionDO open = versions.selectOpenCandidateByRouteId(child.getId());
                if (open != null) throw invalid("已有未完成版本 " + open.getVersionNo());
                MesProRouteVersionDO base = active(child.getId());
                JSONObject merged = MesProRouteTreePatch.apply(snapshot(current), snapshot(candidate), snapshot(base));
                // 子流程合并结果仍处于候选阶段：新增工序可能使用负数 client 引用，
                // 先走候选快照校验，发布投影完成后再走正式快照正数引用校验。
                MesProRouteSnapshotCanonicalizer.Validation validation =
                        canonicalizer.validateCandidate(child.getId(), merged.toJSONString());
                if (!validation.ready()) throw invalid("配置不完整：" + validation.blockers());
                snapshots.put(child.getId(), merged);
                row.put("versionNo", base.getVersionNo()); row.put("beforeProcesses", processNames(snapshot(base)));
                row.put("afterProcesses", processNames(merged));
                row.put("beforeHash", base.getRouteSnapshotSha256());
                row.put("afterHash", canonicalizer.sha256(merged.toJSONString()));
                row.put("changes", differences(snapshot(base), merged, ""));
            } catch (cn.iocoder.yudao.framework.common.exception.ServiceException error) {
                blockers.add(child.getName() + "：" + error.getMessage()); row.put("blocker", error.getMessage());
            }
            affected.add(row);
        }
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("groupName", requireRoute(candidate.getRouteId()).getName());
        preview.put("routeIds", routeIds); preview.put("affected", affected); preview.put("blockers", blockers);
        preview.put("changes", differences(snapshot(current), snapshot(candidate), ""));
        preview.put("candidateHash", candidate.getRouteSnapshotSha256());
        preview.put("fingerprint", canonicalizer.sha256(JSON.toJSONString(preview)));
        return new Plan(snapshots, routeIds, blockers, preview);
    }
    public void verifyChange(MesProCraftChangeDO change) {
        JSONObject proposal = JSON.parseObject(change.getAfterJson());
        Long rootId = proposal.getLong("rootRouteId");
        Set<Long> expected = new HashSet<>(proposal.getJSONArray("routeIds").toJavaList(Long.class));
        Set<Long> actual = new HashSet<>(descendants(rootId).stream().map(MesProRouteDO::getId).toList());
        if (!expected.equals(actual)) throw invalid("父节点下属流程已变化，整批不能发布");
    }
    public void assertMutable(Long id) {
        Set<Long> visited = new HashSet<>();
        for (Long current = id; current != null;) {
            if (!visited.add(current)) throw invalid("树形归属存在循环");
            MesProRouteDO route = requireRoute(current);
            MesProRouteVersionDO open = versions.selectOpenCandidateByRouteId(current);
            if (open != null && (crafts.getObject().groupId(open) != null || !STATUS_DRAFT.equals(open.getLifecycleStatus())))
                throw invalid("该流程或上级正在整批审核，不能调整归属或新增下级");
            current = route.getParentId();
        }
    }
    public void assertDeletable(Long id) {
        assertMutable(id);
        if (!descendants(id).isEmpty()) throw invalid("请先移出下级流程，再删除分组");
    }
    private MesProRouteDO requireGroup(Long id) {
        MesProRouteDO route = requireRoute(id);
        if (!"GROUP".equals(route.getNodeType())) throw invalid("只有分组节点可以添加下级");
        return route;
    }
    private MesProRouteDO requireRoute(Long id) {
        MesProRouteDO route = routes.selectById(id);
        if (route == null) throw invalid("工艺流程节点不存在");
        return route;
    }
    private MesProRouteVersionDO active(Long id) {
        MesProRouteVersionDO value = versions.selectActiveByRouteId(id);
        if (value == null) throw invalid("缺少已发布配置：" + id);
        return value;
    }
    private List<MesProRouteDO> descendants(Long root) {
        List<MesProRouteDO> all = routes.selectList(), result = new ArrayList<>();
        Set<Long> visited = new HashSet<>(Set.of(root));
        Queue<Long> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            Long id = queue.remove();
            for (MesProRouteDO row : all) if (Objects.equals(row.getParentId(), id)) {
                if (!visited.add(row.getId())) throw invalid("树形归属存在循环");
                result.add(row); queue.add(row.getId());
            }
        }
        return result;
    }
    private static JSONObject snapshot(MesProRouteVersionDO version) { return JSON.parseObject(version.getRouteSnapshotJson()); }
    private static List<Long> structure(MesProRouteVersionDO version) {
        return version == null ? List.of() : MesProRouteTreePatch.order(snapshot(version));
    }
    private static List<String> processNames(JSONObject root) {
        return root.getJSONObject("configSnapshots").getJSONObject("flowGraph").getJSONArray("nodes")
                .toJavaList(JSONObject.class).stream().sorted(Comparator.comparing(row -> row.getInteger("sort")))
                .map(row -> row.getString("processName")).toList();
    }
    private static List<Map<String, Object>> differences(Object before, Object after, String path) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (Objects.equals(before, after)) return result;
        if (before instanceof JSONObject old && after instanceof JSONObject next) {
            Set<String> keys = new TreeSet<>(old.keySet()); keys.addAll(next.keySet());
            for (String key : keys) result.addAll(differences(old.get(key), next.get(key), path.isEmpty() ? key : path + "." + key));
        } else {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", path); item.put("before", before); item.put("after", after); result.add(item);
        }
        return result;
    }
    private record Plan(Map<Long, JSONObject> snapshots, List<Long> routeIds, List<String> blockers, Map<String, Object> preview) {}
}
