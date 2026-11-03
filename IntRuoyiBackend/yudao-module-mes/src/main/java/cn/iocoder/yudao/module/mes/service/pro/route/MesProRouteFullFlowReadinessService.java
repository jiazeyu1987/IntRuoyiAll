package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderModeGuard;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
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
public class MesProRouteFullFlowReadinessService {
    private final MesProRouteVersionMapper versions;
    private final MesProRouteVersionSnapshotResolver snapshots;
    private final MesProRouteProcessFlowService graphs;
    private final MesProRouteCandidateConfigServiceImpl productionConfigs;
    private final MesProRouteFlowConfigService flowConfigs;
    private final MesTeamLeaderActiveOrderService activeOrders;
    private final MesFixedTestOrderModeGuard testMode;
    private final MesProEdhrCandidateResolver candidates;
    private final MesProductionReleaseRequiredCandidateResolver releaseCandidates;
    private final MesProBatchRecordReportMapper reports;
    private final MesProBatchRecordJimuReportGateway reportGateway;
    private final cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService workTasks;
    private final cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper formVersions;

    @Transactional(readOnly = true)
    public Result check(Long routeId, Long routeVersionId, Long actor) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<Item> items = new ArrayList<>();
        inspect(items, "重置", "测试模式与操作身份", "在系统配置中开启测试模式后重新检测", () -> {
            testMode.requireEnabled(actor);
            return "当前租户、登录身份及测试模式满足重置入口要求";
        });
        var version = routeVersionId == null ? versions.selectActiveByRouteId(routeId) : versions.selectById(routeVersionId);
        JSONObject configs = null;
        if (version == null || !Objects.equals(routeId, version.getRouteId())
                || !Boolean.TRUE.equals(version.getActive()) || !"ACTIVE".equals(version.getLifecycleStatus())) {
            items.add(new Item("生产准备", "正式路线版本", "BLOCKED",
                    "当前查看的版本不是该路线已发布且启用的正式版本", "发布并启用本版本后重新检测；固定测试订单使用正式版本"));
        } else {
            String json = inspect(items, "生产准备", "正式快照身份与完整性", "修复正式路线快照及发布状态", () -> {
                var resolved = snapshots.resolveVersion(version.getId());
                require(MesProRouteVersionSnapshotValidator.hasCompleteConfigSnapshot(resolved.routeSnapshotJson()),
                        "正式路线缺少完整的工序、产品或生产配置快照");
                return resolved.routeSnapshotJson();
            });
            // Keep snapshot JSON internal; the result contains only a readable receipt.
            if (json != null) {
                configs = JSON.parseObject(json).getJSONObject("configSnapshots");
                items.set(items.size() - 1, new Item("生产准备", "正式快照身份与完整性", "PASS",
                        "正式版本快照、校验摘要及必需配置完整", ""));
                inspect(items, "生产准备", "工序连接完整性", "补齐开始、结束及工序连接并消除环路", () -> {
                    var graph = graphs.getGraph(routeId, version.getId());
                    require(Boolean.TRUE.equals(graph.getValid()) && "VALID".equals(graph.getValidationStatus()),
                            "工序流转关系未通过校验：" + graph.getValidationMessages());
                    return "工序图通过现有业务校验";
                });
                inspect(items, "生产准备 / 一线PQC", "固定订单、产品、数量与QA任务计划", "检查固定测试订单的产品、正式路线、DCC及产品/通用QA规程", () -> {
                    activeOrders.checkFixedTestOrderProductionSource(routeId, version.getId());
                    return "固定订单使用当前正式路线；生产数量与QA工序、检验项目、规则及任务计划通过实际加入流程的只读校验";
                });
            }
        }
        if (configs != null) {
            JSONObject config = configs;
            inspect(items, "一线生产", "逐工序生产配置、设备与工艺", "在相应工序补齐有效的生产参数、设备和工艺关联", () -> {
                productionConfigs.validateProductionReadiness(version.getId(), config);
                return "逐工序配置结构、设备启用状态、设备唯一性及工艺关联校验通过";
            });
            inspect(items, "生产组长", "工序开始责任人", "在工序开始配置负责当前路线的生产组长及有效候选人员", () -> {
                var leaders = flowConfigs.getRouteStartProductionLeaders(routeId, version.getId());
                require(!leaders.isEmpty(), "未配置生产组长");
                for (var leader : leaders) {
                    require(Objects.equals(routeId, leader.getProductionLineId()), "生产组长负责范围不属于当前路线");
                    candidates.resolveRouteBindingCandidate(leader.getCandidateSourceType(),
                            JSON.toJSONString(leader.getCandidateSourceIds()), "工序开始生产组长");
                }
                return "生产组长候选池存在启用用户且负责当前路线";
            });
            inspectBatchRecords(items, config, routeId, version.getId());
            inspect(items, "PQC生产放行 / 上市放行", "四类附件上传责任人", "在工序开始的批记录附件中配置四类资料的有效上传人员", () -> {
                var owners = flowConfigs.getBatchRecordAttachmentOwners(routeId, version.getId());
                require(owners.size() == 4, "四类附件配置不完整");
                for (var owner : owners) {
                    candidates.resolveRouteBindingCandidate(owner.getCandidateSourceType(),
                            JSON.toJSONString(owner.getCandidateSourceIds()), owner.getAttachmentName());
                }
                return "来料检报告、灭菌报告、成品检报告及成品检记录均有有效候选人员";
            });
        } else {
            for (String stage : List.of("生产组长", "一线生产", "批记录与表单槽位", "PQC生产放行 / 上市放行")) {
                items.add(new Item(stage, "版本配置检查", "PENDING", "依赖当前路线的有效正式快照，尚未完成检测", "先解决正式版本缺项，再重新检测"));
            }
        }
        inspectReleaseRole(items, tenantId, "PQC生产放行", MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER);
        inspectReleaseRole(items, tenantId, "上市放行", MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        // These facts do not exist before reset/execution and must never be advertised as PASS.
        items.add(new Item("重置", "历史数据允许重置", "PENDING", "实际重置仍须核验历史数据归属、审计保留及下游引用；本检测不删除数据", "通过生产组长工作台执行固定测试订单重置，若被阻断须先处理其明确原因"));
        items.add(new Item("生产组长 / 一线生产 / 一线PQC", "人员分配、权限与签名", "PENDING", "排班、工序人员分配、对象权限、签名认证和物料实领需在本轮真实操作中验证", "由对应责任人在真实页面完成分配、投产、报工和检验"));
        items.add(new Item("双100%完工", "生产与PQC均达到100%", "PENDING", "必须由本轮真实报工与检验产生，不能凭路线配置认定完工", "完成全部生产数量与必需PQC检验，并关闭异常"));
        items.add(new Item("PQC生产放行", "放行证据与电子签名", "PENDING", "批记录、过程检验、损耗报告、签名及冻结版本须由实际放行校验", "在PQC放行页面完成资料核验和签名"));
        items.add(new Item("上市放行", "最终业务完整性与签名", "PENDING", "四类资料、DHR完整性、检验结果、偏差、返工、报废、库存及签名须按本轮业务事实核验", "由管理者代表在上市放行页面完成预检和签名"));
        boolean blocked = items.stream().anyMatch(item -> "BLOCKED".equals(item.status()));
        return new Result(routeId, version == null ? null : version.getId(),
                version == null ? null : version.getVersionNo(), LocalDateTime.now(),
                blocked ? "BLOCKED" : "PENDING", List.copyOf(items));
    }

    private void inspectReleaseRole(List<Item> items, Long tenantId, String stage, String roleCode) {
        inspect(items, stage, "正式放行角色候选池", "为正式放行角色配置当前租户的启用用户：" + roleCode, () -> {
            var resolved = releaseCandidates.resolveRequiredCandidates(tenantId, roleCode);
            require(!resolved.candidateUserIds().isEmpty(), "放行角色候选池为空");
            return "正式角色 " + roleCode + " 已解析到 " + resolved.candidateUserIds().size() + " 位有效候选人员";
        });
    }

    private void inspectBatchRecords(List<Item> items, JSONObject config, Long routeId, Long routeVersionId) {
        JSONArray uses = config.getJSONArray("batchUseConfigs");
        if (uses == null || uses.isEmpty()) {
            items.add(new Item("批记录", "逐工序正式绑定", "BLOCKED", "正式快照缺少批记录逐工序配置", "逐工序绑定已发布批记录版本；不使用表单槽位补齐"));
        } else {
            int count = 0;
            for (Object raw : uses) {
                JSONObject use = (JSONObject) raw;
                if (!Boolean.TRUE.equals(use.getBoolean("enabled"))) continue;
                JSONArray bindings = use.getJSONArray("batchRecordReports");
                if (bindings == null) continue;
                for (Object value : bindings) {
                    JSONObject binding = (JSONObject) value;
                    count++;
                    String label = "工序 " + use.getString("routeProcessId") + " / " + binding.getString("batchRecordReportId");
                    inspect(items, "批记录", label, "恢复正式绑定的报表版本并确认填写规则及填写候选人", () -> {
                        checkReport(binding);
                        workTasks.checkRouteFillReadiness(routeId, routeVersionId, use.getLong("routeProcessId"), null,
                                binding.getLong("routeBindingId"), binding.getString("batchRecordReportId"),
                                binding.getLong("batchRecordVersionId"), null, null);
                        return "正式报表身份、填写规则和填写候选池有效";
                    });
                }
            }
            if (count == 0) items.add(new Item("批记录", "逐工序正式绑定", "BLOCKED", "未找到启用工序的正式批记录绑定", "配置启用工序的正式批记录"));
        }
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
                        "检查该槽位的正式模板版本及填写责任人", () -> {
                    Long templateVersionId = binding.getLong("lastPublishedTemplateVersionId");
                    require(templateVersionId != null, "表单槽位未绑定已发布模板版本");
                    var template = formVersions.selectById(templateVersionId);
                    require(template != null && "PUBLISHED".equals(template.getStatus())
                            && Objects.equals(template.getTemplateId(), binding.getLong("formTemplateId")), "槽位模板版本不存在、未发布或身份不一致");
                    require(binding.getString("formBindingKey") != null && !binding.getString("formBindingKey").isBlank(), "缺少独立表单槽位标识");
                    workTasks.checkRouteFillReadiness(routeId, routeVersionId, use.getLong("routeProcessId"), null,
                            binding.getLong("routeBindingId"), null, null, binding.getString("formBindingKey"), templateVersionId);
                    return "独立表单槽位的模板与填写候选池有效";
                });
            }
        }
        if (slots == 0) items.add(new Item("表单槽位", "独立绑定", "PASS", "正式快照未配置独立表单槽位，无需检查槽位模板及填写责任人", ""));
    }

    private void checkReport(JSONObject binding) {
        String reportId = binding.getString("batchRecordReportId");
        require(reportId != null && !reportId.isBlank(), "缺少正式报表ID");
        var report = reports.selectByReportId(reportId);
        require(report != null, "报表元数据不存在：" + reportId);
        require(binding.getLong("batchRecordDefinitionId") != null && binding.getLong("batchRecordVersionId") != null
                && Objects.equals(binding.getLong("batchRecordDefinitionId"), report.getBatchRecordDefinitionId())
                && Objects.equals(binding.getLong("batchRecordVersionId"), report.getBatchRecordVersionId()), "报表与冻结的批记录定义或版本身份不一致");
        String json = reportGateway.getReportJsonReadOnly(reportId);
        require(json != null && !json.isBlank(), "报表内容为空");
        require(MesProBatchRecordCellRuleSupport.countUnreviewedFillableCells(JSON.parseObject(json)) == 0,
                "报表存在未确认的填写规则");
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
