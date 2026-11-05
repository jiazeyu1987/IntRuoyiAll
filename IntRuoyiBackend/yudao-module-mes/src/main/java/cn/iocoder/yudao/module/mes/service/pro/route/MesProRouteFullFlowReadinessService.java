package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderModeGuard;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Reads formal prerequisites. Never resets an order or manufactures future execution evidence. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MesProRouteFullFlowReadinessService {
    private final MesProRouteVersionMapper versions;
    private final MesProRouteVersionSnapshotResolver snapshots;
    private final MesProRouteProcessFlowService graphs;
    private final MesProRouteCandidateConfigServiceImpl productionConfigs;
    private final MesProRouteFlowConfigService flowConfigs;
    private final MesFixedTestOrderModeGuard testMode;
    private final MesProEdhrCandidateResolver candidates;
    private final MesProductionReleaseRequiredCandidateResolver releaseCandidates;
    private final cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService workTasks;
    private final cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper formVersions;

    @Transactional(readOnly = true)
    public Result check(Long routeId, Long routeVersionId, Long actor) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<Item> items = new ArrayList<>();
        inspect(items, "重置", "测试模式与操作身份", "在系统配置中开启测试模式后重新检测", () -> {
            testMode.requireEnabled(actor);
            return "测试模式已开启，当前账号可以使用重置功能";
        });
        var version = routeVersionId == null ? versions.selectActiveByRouteId(routeId) : versions.selectById(routeVersionId);
        JSONObject configs = null;
        if (version == null || !Objects.equals(routeId, version.getRouteId())
                || !Boolean.TRUE.equals(version.getActive()) || !"ACTIVE".equals(version.getLifecycleStatus())) {
            items.add(new Item("生产准备", "正式路线版本", "BLOCKED",
                    "当前查看的工艺流程版本尚未发布启用，不能用于测试订单", "返回工艺流程版本列表，发布并启用需要检测的版本，再打开该版本点击“检测”"));
        } else {
            String json = inspect(items, "生产准备", "已发布版本配置完整性", "返回工艺流程，点击“修改”，补齐工序、产品和生产配置并保存；发布启用后重新检测", () -> {
                var resolved = resolveSnapshotForInspection(version.getId());
                require(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(resolved.routeSnapshotJson()),
                        "已发布的工艺流程缺少工序、关联产品或生产配置");
                return resolved.routeSnapshotJson();
            });
            // Keep snapshot JSON internal; the result contains only a readable receipt.
            if (json != null) {
                configs = JSON.parseObject(json).getJSONObject("configSnapshots");
                items.set(items.size() - 1, new Item("生产准备", "已发布版本配置完整性", "PASS",
                        "已发布版本的工序、关联产品和生产配置完整，保存内容校验通过", ""));
                inspect(items, "生产准备", "工序连接完整性", "在工艺流程中点击“修改”，再点击“连接工序”，补齐从工序开始到工序结束的连接，去掉循环连接后保存并发布", () -> {
                    var graph = graphs.getGraph(routeId, version.getId());
                    require(Boolean.TRUE.equals(graph.getValid()) && "VALID".equals(graph.getValidationStatus()),
                            "工序流转关系未通过校验：" + graph.getValidationMessages());
                    return "工序之间的连接正确，可以从工序开始按顺序流转到工序结束";
                });
            }
        }
        if (configs != null) {
            JSONObject config = configs;
            inspect(items, "一线生产", "逐工序生产配置、设备与工艺", "在工艺流程中点击“修改”，选中有问题的工序，补齐生产参数、设备和工艺关联；保存并发布后重新检测", () -> {
                productionConfigs.validateProductionReadiness(version.getId(), config);
                return "各工序的生产配置完整，所选设备可用且未重复，工艺关联正确";
            });
            inspect(items, "生产组长", "工序开始责任人", "在工艺流程中点击“修改”，选中“工序开始”→“生产组长”，选择负责当前流程的人员并点击“保存”；发布后重新检测", () -> {
                var leaders = flowConfigs.getRouteStartProductionLeaders(routeId, version.getId());
                require(!leaders.isEmpty(), "未配置生产组长");
                for (var leader : leaders) {
                    require(Objects.equals(routeId, leader.getProductionLineId()), "配置的生产组长负责范围不包含当前工艺流程");
                    candidates.resolveRouteBindingCandidate(leader.getCandidateSourceType(),
                            JSON.toJSONString(leader.getCandidateSourceIds()), "工序开始生产组长");
                }
                return "已配置负责当前工艺流程的生产组长，且有可用的人员账号";
            });
            inspectFormSlots(items, config, routeId, version.getId());
            inspect(items, "PQC生产放行 / 上市放行", "四类附件上传责任人", "在工艺流程中点击“修改”，选中“工序开始”→“批记录附件”，为四类附件选择上传人员或已分配人员的权限角色，点击“保存”；发布后重新检测", () -> {
                var owners = flowConfigs.getBatchRecordAttachmentOwners(routeId, version.getId());
                require(owners.size() == 4, "四类附件配置不完整");
                for (var owner : owners) {
                    candidates.resolveRouteBindingCandidate(owner.getCandidateSourceType(),
                            JSON.toJSONString(owner.getCandidateSourceIds()), owner.getAttachmentName());
                }
                return "来料检报告、灭菌报告、成品检报告及成品检记录都已配置可用的上传人员";
            });
        }
        inspectReleaseRole(items, tenantId, "PQC生产放行", MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER);
        inspectReleaseRole(items, tenantId, "上市放行", MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        boolean blocked = items.stream().anyMatch(item -> "BLOCKED".equals(item.status()));
        return new Result(routeId, version == null ? null : version.getId(),
                version == null ? null : version.getVersionNo(), LocalDateTime.now(),
                blocked ? "BLOCKED" : "PASS", List.copyOf(items));
    }

    private void inspectReleaseRole(List<Item> items, Long tenantId, String stage, String roleCode) {
        String roleName = switch (roleCode) {
            case MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER -> "PQC负责人";
            case MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE -> "管理者代表";
            default -> throw new IllegalArgumentException("不支持的放行角色：" + roleCode);
        };
        String assignAction = "进入“系统管理”→“权限角色”，找到“" + roleName
                + "”，点击该行“分配”，在弹出的人员列表中勾选已启用人员，点击“确定”并确认用户变更，再返回此处点击“重新检测”";
        try {
            var resolved = releaseCandidates.resolveRequiredCandidates(tenantId, roleCode);
            if (resolved.candidateUserIds().isEmpty()) {
                throw new MesProductionReleaseRoleResolutionException(tenantId, roleCode,
                        MesProductionReleaseRoleResolutionReason.CANDIDATE_EMPTY);
            }
            items.add(new Item(stage, "放行角色及授权人员", "PASS",
                    "“" + roleName + "”已分配 " + resolved.candidateUserIds().size() + " 位可用人员，可以处理" + stage, ""));
        } catch (MesProductionReleaseRoleResolutionException ex) {
            String detail = switch (ex.getReason()) {
                case CANDIDATE_EMPTY -> "“" + roleName + "”尚未分配人员，当前无人可以处理" + stage;
                case ROLE_NOT_FOUND -> "当前租户尚未配置“" + roleName + "”权限角色，无法分配放行人员";
                case ROLE_DISABLED -> "“" + roleName + "”权限角色已停用，已分配人员也无法处理" + stage;
                case CANDIDATE_USER_DISABLED -> "“" + roleName + "”中有已停用的人员账号，暂时无法处理" + stage;
                case ROLE_NOT_UNIQUE -> "“" + roleName + "”存在重复角色配置，系统无法确定使用哪一个";
                case TENANT_CONTEXT_MISMATCH -> "当前登录租户与检测租户不一致，无法检查“" + roleName + "”的授权人员";
                case CANDIDATE_USER_NOT_IN_TENANT -> "“" + roleName + "”关联的人员不存在或不属于当前租户";
                case ROLE_DATA_INCONSISTENT -> "“" + roleName + "”的角色或人员授权数据不一致，暂时无法确认放行人员";
            };
            String action = switch (ex.getReason()) {
                case CANDIDATE_EMPTY -> assignAction;
                case ROLE_DISABLED -> "进入“系统管理”→“权限角色”，找到“" + roleName
                        + "”，点击“编辑”，将“状态”改为“开启”并点击“确定”，再返回此处点击“重新检测”";
                case CANDIDATE_USER_DISABLED -> "进入“系统管理”→“权限角色”，找到“" + roleName
                        + "”并点击“查看”，确认哪些账号已停用；请管理员在“用户管理”中核实并处理这些账号及角色授权，完成后重新检测";
                case ROLE_NOT_FOUND -> "请管理员在“系统管理”→“权限角色”中恢复“" + roleName
                        + "”的标准放行角色配置，再点击该角色行的“分配”指定已启用人员；完成后重新检测";
                case TENANT_CONTEXT_MISMATCH -> "退出登录后选择需要检测的租户重新登录，打开该租户的工艺流程，再点击“检测”";
                case ROLE_NOT_UNIQUE, ROLE_DATA_INCONSISTENT, CANDIDATE_USER_NOT_IN_TENANT ->
                        "请管理员进入“系统管理”→“权限角色”，找到“" + roleName
                                + "”并点击“查看”核对人员；联系维护人员修正角色或人员授权数据后，再点击“重新检测”";
            };
            items.add(new Item(stage, "放行角色及授权人员", "BLOCKED", detail, action));
        }
    }

    private MesProRouteVersionSnapshotResolver.ResolvedRouteVersionSnapshot resolveSnapshotForInspection(Long versionId) {
        try {
            return snapshots.resolveVersion(versionId);
        } catch (IllegalStateException ex) {
            // Preserve the technical cause for maintenance while returning a business finding to the user.
            log.warn("工艺流程已发布版本配置检测失败，版本编号={}", versionId, ex);
            throw new IllegalStateException("已发布版本的配置校验未通过，无法继续检查该版本；请管理员核对版本的保存内容和发布状态", ex);
        }
    }

    private void inspectFormSlots(List<Item> items, JSONObject config, Long routeId, Long routeVersionId) {
        JSONArray uses = config.getJSONArray("batchUseConfigs");
        int slots = 0;
        if (uses != null) for (Object raw : uses) {
            JSONObject use = (JSONObject) raw;
            if (!Boolean.TRUE.equals(use.getBoolean("enabled"))) continue;
            JSONArray bindings = use.getJSONArray("formBindings");
            if (bindings == null) continue;
            for (Object value : bindings) {
                JSONObject binding = (JSONObject) value;
                slots++;
                inspect(items, "表单槽位", "工序 " + use.getString("routeProcessId") + " / " + binding.getString("formBindingKey"),
                        "在工艺流程中点击“修改”，选中对应工序的“表单槽位”，绑定已发布的表单模板并配置填写人员；保存并发布后重新检测", () -> {
                    Long templateVersionId = binding.getLong("lastPublishedTemplateVersionId");
                    require(templateVersionId != null, "表单槽位未绑定已发布模板版本");
                    var template = formVersions.selectById(templateVersionId);
                    require(template != null && "PUBLISHED".equals(template.getStatus())
                            && Objects.equals(template.getTemplateId(), binding.getLong("formTemplateId")), "槽位模板版本不存在、未发布或身份不一致");
                    require(binding.getString("formBindingKey") != null && !binding.getString("formBindingKey").isBlank(), "缺少独立表单槽位标识");
                    workTasks.checkRouteFillReadiness(routeId, routeVersionId, use.getLong("routeProcessId"), null,
                            binding.getLong("routeBindingId"), null, null, binding.getString("formBindingKey"), templateVersionId);
                    return "该表单槽位已绑定已发布的模板，并配置了可用的填写人员";
                });
            }
        }
        if (slots == 0) items.add(new Item("表单槽位", "独立绑定", "PASS", "当前已发布版本没有配置独立表单槽位，本项无需检查", ""));
    }

    private static String inspect(List<Item> items, String stage, String name, String action, Supplier<String> check) {
        try {
            String detail = check.get();
            items.add(new Item(stage, name, "PASS", detail, ""));
            return detail;
        } catch (ServiceException | IllegalStateException | IllegalArgumentException | MesProductionReleaseRoleResolutionException ex) {
            // Domain validation failures are visible findings; infrastructure failures propagate to the caller.
            items.add(new Item(stage, name, "BLOCKED", ex.getMessage(), action));
            return null;
        }
    }

    private static void require(boolean valid, String detail) {
        if (!valid) throw new IllegalStateException(detail);
    }

    public record Item(String stage, String name, String status, String detail, String action) { }
    public record Result(Long routeId, Long routeVersionId, String versionNo, LocalDateTime checkedAt,
                         String overallStatus, List<Item> items) { }
}
