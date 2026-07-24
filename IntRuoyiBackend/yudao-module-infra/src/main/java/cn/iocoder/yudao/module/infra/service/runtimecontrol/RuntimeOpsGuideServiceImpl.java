package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardRecommendationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlWizardScenarioRespVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;

@Service
public class RuntimeOpsGuideServiceImpl implements RuntimeOpsGuideService {

    private final RuntimeOpsCandidateService candidateService;

    public RuntimeOpsGuideServiceImpl(RuntimeOpsCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @Override
    public List<RuntimeControlWizardScenarioRespVO> getScenarios() {
        return List.of(
                scenario("app-exception", "应用异常", "rollback-app", "回滚版本",
                        List.of("生产应用启动失败", "发布后接口大面积异常"),
                        List.of("rollback-candidate", "backup-manifest", "operation-reason"),
                        List.of("release-owner"),
                        List.of("无服务端回滚候选", "候选缺 manifest", "缺生产确认 PROD", "缺发布责任人")),
                scenario("data-exception", "数据异常", "restore-data", "恢复数据",
                        List.of("数据误删", "需要回到指定备份"),
                        List.of("backup-manifest", "checksum"),
                        List.of("data-owner"),
                        List.of("无服务端恢复候选", "缺 manifest", "缺 checksum", "缺数据责任人")),
                scenario("pre-release-check", "发布前检查", "inspection-run", "执行一键巡检",
                        List.of("准备发布测试服或正式服", "发布前需要确认关键证据"),
                        List.of("inspection-report", "business-health", "probe", "backup-point", "capacity-status"),
                        List.of("release-owner"),
                        List.of("缺关键证据", "业务健康未通过", "核心探针失败", "缺备份演练", "磁盘容量风险未关闭")),
                scenario("post-release-observation", "发布后观察", "inspection-run", "执行一键巡检",
                        List.of("发布后需要观察系统是否稳定", "发布后接口或页面出现异常反馈"),
                        List.of("inspection-report", "business-health", "probe", "api-error", "slow-request"),
                        List.of("release-owner", "reviewer"),
                        List.of("缺巡检报告", "发布后探针失败", "业务健康 NO_GO", "API 错误或慢请求未确认")),
                scenario("backup-drill", "备份演练", "backup-points", "查看备份演练",
                        List.of("备份演练失败", "恢复前需要确认备份点可恢复"),
                        List.of("backup-manifest", "checksum", "rehearsal-report", "last-verified-at"),
                        List.of("data-owner"),
                        List.of("缺 manifest", "缺 checksum", "缺演练报告", "最近验证时间缺失")),
                scenario("disk-risk", "磁盘风险", "capacity-status", "查看容量状态",
                        List.of("日志增长过快", "磁盘容量超过阈值"),
                        List.of("capacity-status", "log-directory-size", "disk-usage", "alert"),
                        List.of("ops-owner"),
                        List.of("磁盘采样阻断", "日志目录超过阈值", "容量告警未确认"))
        );
    }

    @Override
    public RuntimeControlWizardRecommendationRespVO recommend(RuntimeControlWizardRecommendationReqVO reqVO) {
        RuntimeControlWizardScenarioRespVO scenario = getScenarios().stream()
                .filter(item -> item.getScenario().equals(reqVO.getScenario()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                        "scenario 不支持：" + reqVO.getScenario()));
        RuntimeControlWizardRecommendationRespVO recommendation = new RuntimeControlWizardRecommendationRespVO();
        recommendation.setScenario(scenario.getScenario());
        recommendation.setRecommendedAction(scenario.getRecommendedAction());
        recommendation.setRecommendedActionLabel(scenario.getRecommendedActionLabel());
        recommendation.setRequiredEvidence(scenario.getRequiredEvidence());
        recommendation.setRequiredOwnerRoles(scenario.getRequiredOwnerRoles());
        if ("rollback-app".equals(scenario.getRecommendedAction())) {
            List<RuntimeControlRollbackCandidateRespVO> candidates = candidateService.listRollbackCandidates();
            recommendation.setRollbackCandidates(candidates);
            recommendation.setRestoreCandidates(List.of());
            recommendation.setBlockingReasons(collectRollbackBlockingReasons(candidates));
        } else if ("restore-data".equals(scenario.getRecommendedAction())) {
            List<RuntimeControlRestoreCandidateRespVO> candidates = candidateService.listRestoreCandidates();
            recommendation.setRollbackCandidates(List.of());
            recommendation.setRestoreCandidates(candidates);
            recommendation.setBlockingReasons(collectRestoreBlockingReasons(candidates));
        } else {
            recommendation.setRollbackCandidates(List.of());
            recommendation.setRestoreCandidates(List.of());
            recommendation.setBlockingReasons(scenario.getBlockingConditions());
        }
        return recommendation;
    }

    private RuntimeControlWizardScenarioRespVO scenario(String scenario, String label, String action, String actionLabel,
                                                       List<String> symptoms, List<String> evidence,
                                                       List<String> ownerRoles, List<String> blockingConditions) {
        RuntimeControlWizardScenarioRespVO respVO = new RuntimeControlWizardScenarioRespVO();
        respVO.setScenario(scenario);
        respVO.setLabel(label);
        respVO.setRecommendedAction(action);
        respVO.setRecommendedActionLabel(actionLabel);
        respVO.setSymptoms(symptoms);
        respVO.setRequiredEvidence(evidence);
        respVO.setRequiredOwnerRoles(ownerRoles);
        respVO.setBlockingConditions(blockingConditions);
        return respVO;
    }

    private List<String> collectRollbackBlockingReasons(List<RuntimeControlRollbackCandidateRespVO> candidates) {
        List<String> reasons = new ArrayList<>();
        if (candidates.isEmpty()) {
            reasons.add("无服务端回滚候选");
        }
        candidates.stream()
                .flatMap(candidate -> candidate.getBlockedReasons().stream())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .forEach(reasons::add);
        return reasons;
    }

    private List<String> collectRestoreBlockingReasons(List<RuntimeControlRestoreCandidateRespVO> candidates) {
        List<String> reasons = new ArrayList<>();
        if (candidates.isEmpty()) {
            reasons.add("无服务端恢复候选");
        }
        candidates.stream()
                .flatMap(candidate -> candidate.getBlockedReasons().stream())
                .filter(StrUtil::isNotBlank)
                .distinct()
                .forEach(reasons::add);
        return reasons;
    }
}
