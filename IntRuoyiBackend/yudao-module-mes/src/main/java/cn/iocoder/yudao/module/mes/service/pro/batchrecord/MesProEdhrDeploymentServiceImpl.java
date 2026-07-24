package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentGateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeploymentUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeliveryProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeploymentEvidenceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeploymentGateItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeliveryProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeploymentEvidenceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeploymentGateItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_EVIDENCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_INTERFACE_RESPONSE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_LICENSE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_MANIFEST_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_PROJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeploymentErrorCodeConstants.PRO_EDHR_DEPLOYMENT_VERSION_INCONSISTENT;

@Service
public class MesProEdhrDeploymentServiceImpl implements MesProEdhrDeploymentService {

    private static final DateTimeFormatter DEPLOYMENT_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String STATUS_DELIVERY_DRAFT = "DELIVERY_DRAFT";
    private static final String STATUS_ENVIRONMENT_CHECKED = "ENVIRONMENT_CHECKED";
    private static final String STATUS_INSTALLED = "INSTALLED";
    private static final String STATUS_INTEGRATED = "INTEGRATED";
    private static final String STATUS_DELIVERY_BLOCKED = "DELIVERY_BLOCKED";
    private static final String GATE_STATUS_PASSED = "PASSED";
    private static final String GATE_STATUS_BLOCKED = "BLOCKED";
    private static final String GATE_ENVIRONMENT_AUTHORIZED = "ENVIRONMENT_AUTHORIZED";
    private static final String GATE_RELEASE_MANIFEST = "RELEASE_MANIFEST";
    private static final String GATE_SCHEMA_REQUIRED_SQL = "SCHEMA_REQUIRED_SQL";
    private static final String GATE_LICENSE_VALID = "LICENSE_VALID";
    private static final String GATE_INTERFACE_RESPONSE = "INTERFACE_RESPONSE";

    @Resource
    private MesProEdhrDeliveryProjectMapper projectMapper;
    @Resource
    private MesProEdhrDeploymentEvidenceMapper deploymentEvidenceMapper;
    @Resource
    private MesProEdhrDeploymentGateItemMapper deploymentGateItemMapper;

    @Override
    public PageResult<MesProEdhrDeploymentRespVO> getPage(MesProEdhrDeploymentPageReqVO reqVO) {
        return BeanUtils.toBean(deploymentEvidenceMapper.selectPage(reqVO), MesProEdhrDeploymentRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDeploymentRespVO createEvidence(MesProEdhrDeploymentCreateReqVO reqVO) {
        MesProEdhrDeliveryProjectDO project = requireProject(reqVO.getProjectId());
        MesProEdhrDeploymentEvidenceDO evidence = new MesProEdhrDeploymentEvidenceDO()
                .setProjectId(project.getId())
                .setDeploymentCode(buildDeploymentCode())
                .setDeploymentName(reqVO.getDeploymentName())
                .setCustomerProjectName(reqVO.getCustomerProjectName())
                .setTargetEnvironment(reqVO.getTargetEnvironment())
                .setEnvironmentAuthorized(Boolean.TRUE.equals(reqVO.getEnvironmentAuthorized()))
                .setEnvironmentCheckSummary(blankToEmpty(reqVO.getEnvironmentCheckSummary()))
                .setServerSummary(blankToEmpty(reqVO.getServerSummary()))
                .setNetworkSummary(blankToEmpty(reqVO.getNetworkSummary()))
                .setObjectStorageSummary(blankToEmpty(reqVO.getObjectStorageSummary()))
                .setCapacitySummary(blankToEmpty(reqVO.getCapacitySummary()))
                .setPermissionSummary(blankToEmpty(reqVO.getPermissionSummary()))
                .setReleaseTag(reqVO.getReleaseTag())
                .setArtifactVersion(blankToEmpty(reqVO.getArtifactVersion()))
                .setArtifactChecksum(blankToEmpty(reqVO.getArtifactChecksum()))
                .setSchemaVersion(reqVO.getSchemaVersion())
                .setMigrationManifest(blankToEmpty(reqVO.getMigrationManifest()))
                .setRequiredSqlManifest(blankToEmpty(reqVO.getRequiredSqlManifest()))
                .setAppImportResult(blankToEmpty(reqVO.getAppImportResult()))
                .setLicenseScope("")
                .setLicenseFileEvidence("")
                .setLicenseCheckResult("")
                .setCustomerLicenseConfirmation("")
                .setInterfaceScope("")
                .setInterfaceVersion("")
                .setIntegrationEnvironment("")
                .setRequestEvidence("")
                .setResponseEvidence("")
                .setInterfaceFailureCount(0)
                .setRemediationAction("")
                .setRetestEvidence("")
                .setInterfaceConfirmedBy("")
                .setDeploymentStatus(STATUS_DELIVERY_DRAFT)
                .setBlockedReason("")
                .setNextAction("执行部署授权接口门禁预检")
                .setGatePassed(false)
                .setEvidenceSnapshotChecksum("")
                .setRemark(reqVO.getRemark());
        deploymentEvidenceMapper.insert(evidence);
        evaluateAndPersist(evidence, project);
        return getDetail(evidence.getId());
    }

    @Override
    public MesProEdhrDeploymentRespVO getDetail(Long id) {
        MesProEdhrDeploymentEvidenceDO evidence = requireEvidence(id);
        return toResp(evidence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDeploymentRespVO updateEvidence(MesProEdhrDeploymentUpdateReqVO reqVO) {
        MesProEdhrDeploymentEvidenceDO evidence = requireEvidence(reqVO.getDeploymentId());
        evidence.setTargetEnvironment(blankToExisting(reqVO.getTargetEnvironment(), evidence.getTargetEnvironment()))
                .setEnvironmentAuthorized(reqVO.getEnvironmentAuthorized() == null
                        ? evidence.getEnvironmentAuthorized() : reqVO.getEnvironmentAuthorized())
                .setEnvironmentCheckSummary(blankToExisting(reqVO.getEnvironmentCheckSummary(), evidence.getEnvironmentCheckSummary()))
                .setServerSummary(blankToExisting(reqVO.getServerSummary(), evidence.getServerSummary()))
                .setNetworkSummary(blankToExisting(reqVO.getNetworkSummary(), evidence.getNetworkSummary()))
                .setObjectStorageSummary(blankToExisting(reqVO.getObjectStorageSummary(), evidence.getObjectStorageSummary()))
                .setCapacitySummary(blankToExisting(reqVO.getCapacitySummary(), evidence.getCapacitySummary()))
                .setPermissionSummary(blankToExisting(reqVO.getPermissionSummary(), evidence.getPermissionSummary()))
                .setReleaseTag(blankToExisting(reqVO.getReleaseTag(), evidence.getReleaseTag()))
                .setArtifactVersion(blankToExisting(reqVO.getArtifactVersion(), evidence.getArtifactVersion()))
                .setArtifactChecksum(blankToExisting(reqVO.getArtifactChecksum(), evidence.getArtifactChecksum()))
                .setSchemaVersion(blankToExisting(reqVO.getSchemaVersion(), evidence.getSchemaVersion()))
                .setMigrationManifest(blankToExisting(reqVO.getMigrationManifest(), evidence.getMigrationManifest()))
                .setRequiredSqlManifest(blankToExisting(reqVO.getRequiredSqlManifest(), evidence.getRequiredSqlManifest()))
                .setAppImportResult(blankToExisting(reqVO.getAppImportResult(), evidence.getAppImportResult()))
                .setLicenseScope(blankToExisting(reqVO.getLicenseScope(), evidence.getLicenseScope()))
                .setLicenseValidUntil(reqVO.getLicenseValidUntil() == null ? evidence.getLicenseValidUntil() : reqVO.getLicenseValidUntil())
                .setLicenseFileEvidence(blankToExisting(reqVO.getLicenseFileEvidence(), evidence.getLicenseFileEvidence()))
                .setLicenseCheckResult(blankToExisting(reqVO.getLicenseCheckResult(), evidence.getLicenseCheckResult()))
                .setCustomerLicenseConfirmation(blankToExisting(reqVO.getCustomerLicenseConfirmation(), evidence.getCustomerLicenseConfirmation()))
                .setInterfaceScope(blankToExisting(reqVO.getInterfaceScope(), evidence.getInterfaceScope()))
                .setInterfaceVersion(blankToExisting(reqVO.getInterfaceVersion(), evidence.getInterfaceVersion()))
                .setIntegrationEnvironment(blankToExisting(reqVO.getIntegrationEnvironment(), evidence.getIntegrationEnvironment()))
                .setRequestEvidence(blankToExisting(reqVO.getRequestEvidence(), evidence.getRequestEvidence()))
                .setResponseEvidence(blankToExisting(reqVO.getResponseEvidence(), evidence.getResponseEvidence()))
                .setInterfaceFailureCount(reqVO.getInterfaceFailureCount() == null ? evidence.getInterfaceFailureCount() : reqVO.getInterfaceFailureCount())
                .setRemediationAction(blankToExisting(reqVO.getRemediationAction(), evidence.getRemediationAction()))
                .setRetestEvidence(blankToExisting(reqVO.getRetestEvidence(), evidence.getRetestEvidence()))
                .setInterfaceConfirmedBy(blankToExisting(reqVO.getInterfaceConfirmedBy(), evidence.getInterfaceConfirmedBy()));
        evaluateAndPersist(evidence, requireProject(evidence.getProjectId()));
        return getDetail(evidence.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDeploymentPrecheckRespVO precheckEvidence(Long deploymentId) {
        MesProEdhrDeploymentEvidenceDO evidence = requireEvidence(deploymentId);
        evaluateAndPersist(evidence, requireProject(evidence.getProjectId()));
        MesProEdhrDeploymentEvidenceDO refreshed = requireEvidence(deploymentId);
        return new MesProEdhrDeploymentPrecheckRespVO()
                .setDeploymentId(refreshed.getId())
                .setDeploymentCode(refreshed.getDeploymentCode())
                .setDeploymentStatus(refreshed.getDeploymentStatus())
                .setGatePassed(refreshed.getGatePassed())
                .setBlockedReason(refreshed.getBlockedReason())
                .setNextAction(refreshed.getNextAction())
                .setGateCheckedAt(refreshed.getGateCheckedAt())
                .setEvidenceSnapshotChecksum(refreshed.getEvidenceSnapshotChecksum())
                .setGateItems(toGateRespList(deploymentGateItemMapper.selectListByDeploymentId(deploymentId)));
    }

    private MesProEdhrDeploymentRespVO toResp(MesProEdhrDeploymentEvidenceDO evidence) {
        return BeanUtils.toBean(evidence, MesProEdhrDeploymentRespVO.class)
                .setGateItems(toGateRespList(deploymentGateItemMapper.selectListByDeploymentId(evidence.getId())));
    }

    private List<MesProEdhrDeploymentGateItemRespVO> toGateRespList(List<MesProEdhrDeploymentGateItemDO> gateItems) {
        return BeanUtils.toBean(gateItems, MesProEdhrDeploymentGateItemRespVO.class);
    }

    private void evaluateAndPersist(MesProEdhrDeploymentEvidenceDO evidence, MesProEdhrDeliveryProjectDO project) {
        List<GateDecision> decisions = List.of(
                evaluateEnvironmentGate(evidence),
                evaluateReleaseManifestGate(evidence, project),
                evaluateSchemaRequiredSqlGate(evidence, project),
                evaluateLicenseGate(evidence),
                evaluateInterfaceGate(evidence)
        );
        for (GateDecision decision : decisions) {
            upsertGate(evidence.getId(), decision);
        }

        List<String> blockers = decisions.stream()
                .filter(decision -> !decision.passed())
                .map(GateDecision::missingEvidence)
                .toList();
        String evidenceSnapshotChecksum = buildEvidenceSnapshotChecksum(evidence, decisions);
        evidence.setGateCheckedAt(LocalDateTime.now())
                .setEvidenceSnapshotChecksum(evidenceSnapshotChecksum);
        if (blockers.isEmpty()) {
            evidence.setDeploymentStatus(STATUS_INTEGRATED)
                    .setGatePassed(true)
                    .setBlockedReason("")
                    .setNextAction("部署、授权许可与接口确认均已具备受控证据，可进入验证签核门禁");
        } else {
            evidence.setDeploymentStatus(resolveBlockedStatus(decisions))
                    .setGatePassed(false)
                    .setBlockedReason(String.join("；", blockers))
                    .setNextAction(resolveNextAction(decisions));
        }
        deploymentEvidenceMapper.updateById(evidence);
    }

    private GateDecision evaluateEnvironmentGate(MesProEdhrDeploymentEvidenceDO evidence) {
        List<String> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(evidence.getEnvironmentAuthorized())) {
            missing.add("缺少目标环境授权");
        }
        requireText(missing, evidence.getEnvironmentCheckSummary(), "缺少环境检查摘要");
        requireText(missing, evidence.getServerSummary(), "缺少服务器检查证据");
        requireText(missing, evidence.getNetworkSummary(), "缺少网络、端口、域名或证书检查证据");
        requireText(missing, evidence.getObjectStorageSummary(), "缺少对象存储检查证据");
        requireText(missing, evidence.getCapacitySummary(), "缺少容量检查证据");
        requireText(missing, evidence.getPermissionSummary(), "缺少权限检查证据");
        return decision(GATE_ENVIRONMENT_AUTHORIZED, "环境授权与检查", "目标环境/服务器/网络/对象存储/容量/权限",
                missing, "实施负责人", "补齐环境授权、服务器、网络、对象存储、容量和权限检查证据", "阻断部署环境确认");
    }

    private GateDecision evaluateReleaseManifestGate(MesProEdhrDeploymentEvidenceDO evidence, MesProEdhrDeliveryProjectDO project) {
        List<String> missing = new ArrayList<>();
        requireText(missing, evidence.getReleaseTag(), "缺少 releaseTag");
        requireText(missing, evidence.getArtifactVersion(), "缺少安装包版本");
        requireText(missing, evidence.getArtifactChecksum(), "缺少制品 checksum");
        requireText(missing, evidence.getMigrationManifest(), "缺少迁移 manifest");
        requireText(missing, evidence.getAppImportResult(), "缺少应用导入结果");
        if (isNotBlank(evidence.getReleaseTag()) && isNotBlank(project.getReleaseTag())
                && !Objects.equals(evidence.getReleaseTag(), project.getReleaseTag())) {
            missing.add(PRO_EDHR_DEPLOYMENT_VERSION_INCONSISTENT.getMsg() + "：releaseTag");
        }
        if (!missing.isEmpty() && missing.stream().anyMatch(item -> item.contains("manifest") || item.contains("checksum"))) {
            PRO_EDHR_DEPLOYMENT_MANIFEST_REQUIRED.getCode();
        }
        return decision(GATE_RELEASE_MANIFEST, "发布包 manifest", "安装包版本/releaseTag/checksum/迁移清单/应用导入",
                missing, "实施负责人", "补齐发布 manifest、制品 checksum、迁移清单和应用导入结果", "阻断安装完成确认");
    }

    private GateDecision evaluateSchemaRequiredSqlGate(MesProEdhrDeploymentEvidenceDO evidence, MesProEdhrDeliveryProjectDO project) {
        List<String> missing = new ArrayList<>();
        requireText(missing, evidence.getSchemaVersion(), "缺少数据库 schema 版本");
        requireText(missing, evidence.getRequiredSqlManifest(), "缺少 required SQL 清单");
        if (isNotBlank(evidence.getSchemaVersion()) && isNotBlank(project.getSchemaVersion())
                && !Objects.equals(evidence.getSchemaVersion(), project.getSchemaVersion())) {
            missing.add(PRO_EDHR_DEPLOYMENT_VERSION_INCONSISTENT.getMsg() + "：schemaVersion");
        }
        if (!missing.isEmpty()) {
            PRO_EDHR_DEPLOYMENT_MANIFEST_REQUIRED.getCode();
        }
        return decision(GATE_SCHEMA_REQUIRED_SQL, "schema 与 required SQL", "数据库版本/required SQL/兼容矩阵",
                missing, "实施负责人", "补齐 schema 版本、required SQL 清单并确认与交付项目一致", "阻断数据库版本确认");
    }

    private GateDecision evaluateLicenseGate(MesProEdhrDeploymentEvidenceDO evidence) {
        List<String> missing = new ArrayList<>();
        requireText(missing, evidence.getLicenseScope(), "缺少授权范围");
        if (evidence.getLicenseValidUntil() == null) {
            missing.add("缺少授权有效期");
        } else if (evidence.getLicenseValidUntil().isBefore(LocalDate.now())) {
            missing.add("授权许可已过期");
        }
        requireText(missing, evidence.getLicenseFileEvidence(), "缺少授权文件证据");
        requireText(missing, evidence.getLicenseCheckResult(), "缺少授权校验结果");
        requireText(missing, evidence.getCustomerLicenseConfirmation(), "缺少客户授权确认");
        if (!missing.isEmpty()) {
            PRO_EDHR_DEPLOYMENT_LICENSE_REQUIRED.getCode();
        }
        return decision(GATE_LICENSE_VALID, "授权许可", "授权范围/有效期/授权文件/校验结果/客户确认",
                missing, "客户IT/商务负责人", "补齐授权许可文件、范围、有效期、校验结果和客户确认", "阻断商业化交付签核");
    }

    private GateDecision evaluateInterfaceGate(MesProEdhrDeploymentEvidenceDO evidence) {
        List<String> missing = new ArrayList<>();
        requireText(missing, evidence.getInterfaceScope(), "缺少接口范围");
        requireText(missing, evidence.getInterfaceVersion(), "缺少接口版本");
        requireText(missing, evidence.getIntegrationEnvironment(), "缺少联调环境");
        requireText(missing, evidence.getRequestEvidence(), "缺少真实请求证据");
        requireText(missing, evidence.getResponseEvidence(), "缺少真实响应证据");
        requireText(missing, evidence.getInterfaceConfirmedBy(), "缺少接口确认人");
        if (evidence.getInterfaceFailureCount() != null && evidence.getInterfaceFailureCount() > 0) {
            requireText(missing, evidence.getRemediationAction(), "存在接口失败项但缺少失败整改措施");
            requireText(missing, evidence.getRetestEvidence(), "存在接口失败项但缺少复测证据");
        }
        if (!missing.isEmpty()) {
            PRO_EDHR_DEPLOYMENT_INTERFACE_RESPONSE_REQUIRED.getCode();
        }
        return decision(GATE_INTERFACE_RESPONSE, "接口请求响应", "接口范围/版本/联调环境/真实请求响应/失败整改复测",
                missing, "接口集成负责人", "补齐接口范围、真实请求响应、失败整改和复测证据", "阻断接口交付确认");
    }

    private GateDecision decision(String gateCode, String gateName, String evidenceSource, List<String> missing,
                                  String ownerName, String nextAction, String signoffImpact) {
        return new GateDecision(gateCode, gateName, missing.isEmpty(), evidenceSource,
                missing.isEmpty() ? "已具备受控证据" : String.join("；", missing),
                ownerName, missing.isEmpty() ? "保持证据冻结并进入后续门禁" : nextAction, signoffImpact);
    }

    private void upsertGate(Long deploymentId, GateDecision decision) {
        MesProEdhrDeploymentGateItemDO existing =
                deploymentGateItemMapper.selectByDeploymentIdAndGateCode(deploymentId, decision.gateCode());
        MesProEdhrDeploymentGateItemDO gateItem = (existing == null ? new MesProEdhrDeploymentGateItemDO() : existing)
                .setDeploymentId(deploymentId)
                .setGateCode(decision.gateCode())
                .setGateName(decision.gateName())
                .setGateStatus(decision.passed() ? GATE_STATUS_PASSED : GATE_STATUS_BLOCKED)
                .setEvidenceSource(decision.evidenceSource())
                .setMissingEvidence(decision.missingEvidence())
                .setOwnerName(decision.ownerName())
                .setNextAction(decision.nextAction())
                .setSignoffImpact(decision.signoffImpact());
        if (existing == null) {
            deploymentGateItemMapper.insert(gateItem);
        } else {
            deploymentGateItemMapper.updateById(gateItem);
        }
    }

    private String resolveBlockedStatus(List<GateDecision> decisions) {
        boolean environmentPassed = findDecision(decisions, GATE_ENVIRONMENT_AUTHORIZED).passed();
        boolean releasePassed = findDecision(decisions, GATE_RELEASE_MANIFEST).passed();
        boolean schemaPassed = findDecision(decisions, GATE_SCHEMA_REQUIRED_SQL).passed();
        if (environmentPassed && releasePassed && schemaPassed) {
            return STATUS_INSTALLED;
        }
        if (environmentPassed) {
            return STATUS_ENVIRONMENT_CHECKED;
        }
        return STATUS_DELIVERY_BLOCKED;
    }

    private String resolveNextAction(List<GateDecision> decisions) {
        return decisions.stream()
                .filter(decision -> !decision.passed())
                .findFirst()
                .map(GateDecision::nextAction)
                .orElse("执行部署授权接口门禁预检");
    }

    private GateDecision findDecision(List<GateDecision> decisions, String gateCode) {
        return decisions.stream()
                .filter(decision -> Objects.equals(decision.gateCode(), gateCode))
                .findFirst()
                .orElseThrow(() -> exception(PRO_EDHR_DEPLOYMENT_EVIDENCE_NOT_EXISTS));
    }

    private String buildEvidenceSnapshotChecksum(MesProEdhrDeploymentEvidenceDO evidence, List<GateDecision> decisions) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("deploymentCode", evidence.getDeploymentCode());
        snapshot.put("projectId", evidence.getProjectId());
        snapshot.put("targetEnvironment", evidence.getTargetEnvironment());
        snapshot.put("releaseTag", evidence.getReleaseTag());
        snapshot.put("schemaVersion", evidence.getSchemaVersion());
        snapshot.put("artifactChecksum", evidence.getArtifactChecksum());
        snapshot.put("licenseScope", evidence.getLicenseScope());
        snapshot.put("licenseValidUntil", evidence.getLicenseValidUntil());
        snapshot.put("interfaceScope", evidence.getInterfaceScope());
        snapshot.put("interfaceVersion", evidence.getInterfaceVersion());
        snapshot.put("responseEvidence", evidence.getResponseEvidence());
        snapshot.put("gates", decisions);
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(snapshot));
    }

    private MesProEdhrDeliveryProjectDO requireProject(Long projectId) {
        MesProEdhrDeliveryProjectDO project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null) {
            throw exception(PRO_EDHR_DEPLOYMENT_PROJECT_NOT_EXISTS);
        }
        return project;
    }

    private MesProEdhrDeploymentEvidenceDO requireEvidence(Long id) {
        MesProEdhrDeploymentEvidenceDO evidence = id == null ? null : deploymentEvidenceMapper.selectById(id);
        if (evidence == null) {
            throw exception(PRO_EDHR_DEPLOYMENT_EVIDENCE_NOT_EXISTS);
        }
        return evidence;
    }

    private String buildDeploymentCode() {
        return "EDHR-DEP-" + DEPLOYMENT_CODE_TIME.format(LocalDateTime.now());
    }

    private void requireText(List<String> missing, String value, String missingMessage) {
        if (!isNotBlank(value)) {
            missing.add(missingMessage);
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToExisting(String value, String existing) {
        return value == null ? existing : value.trim();
    }

    private record GateDecision(String gateCode, String gateName, boolean passed, String evidenceSource,
                                String missingEvidence, String ownerName, String nextAction, String signoffImpact) {
    }
}
