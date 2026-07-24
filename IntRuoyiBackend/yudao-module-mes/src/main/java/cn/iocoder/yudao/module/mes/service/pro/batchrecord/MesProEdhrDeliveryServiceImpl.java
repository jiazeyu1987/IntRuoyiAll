package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryGateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryGateSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeliveryGateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeliveryProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrEvidencePackageDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeliveryGateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeliveryProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrEvidencePackageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeliveryErrorCodeConstants.PRO_EDHR_DELIVERY_GATE_ITEM_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeliveryErrorCodeConstants.PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeliveryErrorCodeConstants.PRO_EDHR_DELIVERY_PROJECT_NOT_EXISTS;

@Service
public class MesProEdhrDeliveryServiceImpl implements MesProEdhrDeliveryService {

    private static final DateTimeFormatter PROJECT_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String PROJECT_STATUS_BLOCKED = "BLOCKED";
    private static final String GATE_STATUS_BLOCKED = "BLOCKED";
    private static final String PACKAGE_STATUS_MISSING = "MISSING";
    private static final String EVIDENCE_STATUS_MISSING = "MISSING";

    private static final List<GateTemplate> GATE_TEMPLATES = List.of(
            new GateTemplate("CSV_VALIDATION", "CSV验证包", "CSV_VALIDATION", "GATE_CSV_VALIDATION", "CSV验证资料",
                    List.of("验证计划", "URS/FRS追踪矩阵", "风险评估", "IQ/OQ/PQ报告", "偏差关闭记录"),
                    "缺少CSV验证计划、追踪矩阵、风险评估、IQ/OQ/PQ执行证据或偏差关闭记录",
                    "质量负责人", "补齐CSV验证包并完成QA复核", "阻断验证签核"),
            new GateTemplate("OQ_PQ", "OQ/PQ执行包", "OQ_PQ", "GATE_OQ_PQ",
                    "OQ/PQ执行资料", List.of("OQ脚本", "PQ脚本", "执行记录", "偏差/CAPA关闭"),
                    "缺少OQ/PQ脚本、真实执行记录或偏差/CAPA关闭证据",
                    "验证负责人", "完成OQ/PQ执行台与偏差关闭记录归档", "阻断上线验证结论"),
            new GateTemplate("TRAINING", "培训签核包", "TRAINING", "GATE_TRAINING",
                    "培训资料", List.of("角色培训矩阵", "培训签到", "考核记录", "归档确认"),
                    "缺少角色覆盖、培训签到、考核或培训归档证据",
                    "培训负责人", "按角色补齐培训覆盖并归档考核记录", "阻断交付确认"),
            new GateTemplate("DEPLOYMENT_AUTH", "部署授权包", "DEPLOYMENT_AUTH", "GATE_DEPLOYMENT_AUTH",
                    "部署授权资料", List.of("部署授权单", "发布清单", "schema版本", "授权批准"),
                    "缺少部署授权、发布清单、schema版本一致性或批准记录",
                    "IT负责人", "补齐部署授权和发布清单并确认schema版本", "阻断部署放行"),
            new GateTemplate("INTERFACE", "接口联调包", "INTERFACE", "GATE_INTERFACE",
                    "接口联调资料", List.of("接口范围", "请求/响应样例", "异常处理记录", "联调签字"),
                    "缺少接口范围、真实请求响应证据或异常处理记录",
                    "集成负责人", "补齐接口范围和真实联调证据", "阻断接口上线确认"),
            new GateTemplate("OPERATIONS", "运维交接包", "OPERATIONS", "GATE_OPERATIONS",
                    "运维资料", List.of("备份链路", "恢复演练", "监控告警", "运维SOP"),
                    "缺少备份链路、恢复演练证据、监控告警或运维SOP",
                    "运维负责人", "完成恢复演练并归档运维SOP", "阻断商业化交付签核")
    );

    @Resource
    private MesProEdhrDeliveryProjectMapper projectMapper;
    @Resource
    private MesProEdhrEvidencePackageMapper evidencePackageMapper;
    @Resource
    private MesProEdhrDeliveryGateItemMapper gateItemMapper;

    @Override
    public PageResult<MesProEdhrDeliveryProjectRespVO> getProjectPage(MesProEdhrDeliveryProjectPageReqVO reqVO) {
        return BeanUtils.toBean(projectMapper.selectPage(reqVO), MesProEdhrDeliveryProjectRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrDeliveryProjectRespVO createProject(MesProEdhrDeliveryProjectCreateReqVO reqVO) {
        String projectCode = buildProjectCode();
        MesProEdhrDeliveryProjectDO project = new MesProEdhrDeliveryProjectDO()
                .setProjectCode(projectCode)
                .setProjectName(reqVO.getProjectName())
                .setCustomerName(reqVO.getCustomerName())
                .setSiteName(reqVO.getSiteName())
                .setSystemScope(reqVO.getSystemScope())
                .setValidationScope(reqVO.getValidationScope())
                .setReleaseTag(reqVO.getReleaseTag())
                .setSchemaVersion(reqVO.getSchemaVersion())
                .setTargetEnvironment(reqVO.getTargetEnvironment())
                .setProjectStatus(PROJECT_STATUS_BLOCKED)
                .setSignoffAllowed(false)
                .setOwnerName(reqVO.getOwnerName())
                .setOwnerDepartment(reqVO.getOwnerDepartment())
                .setBlockedReason("首切片仅登记交付证据对象，CSV/OQ/PQ、培训、部署授权、接口、运维恢复演练证据均未闭环")
                .setGateSummaryJson(buildInitialGateSummaryJson(projectCode))
                .setRemark(reqVO.getRemark());
        if (projectMapper.insert(project) != 1 || project.getId() == null) {
            throw exception(PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED);
        }

        for (int index = 0; index < GATE_TEMPLATES.size(); index++) {
            createEvidencePackageAndGate(project, GATE_TEMPLATES.get(index), index + 1);
        }
        return getProjectDetail(project.getId());
    }

    @Override
    public MesProEdhrDeliveryProjectRespVO getProjectDetail(Long id) {
        return BeanUtils.toBean(requireProject(id), MesProEdhrDeliveryProjectRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrEvidencePackageRespVO> getEvidencePackagePage(MesProEdhrEvidencePackagePageReqVO reqVO) {
        requireProject(reqVO.getProjectId());
        return BeanUtils.toBean(evidencePackageMapper.selectPage(reqVO), MesProEdhrEvidencePackageRespVO.class);
    }

    @Override
    public MesProEdhrDeliveryGateSummaryRespVO getGateSummary(Long projectId) {
        MesProEdhrDeliveryProjectDO project = requireProject(projectId);
        List<MesProEdhrDeliveryGateItemDO> gateItems = gateItemMapper.selectListByProjectId(projectId);
        if (gateItems.isEmpty()) {
            throw exception(PRO_EDHR_DELIVERY_GATE_ITEM_MISSING);
        }
        int blockedCount = (int) gateItems.stream().filter(item -> Boolean.TRUE.equals(item.getBlockingFlag())).count();
        List<MesProEdhrEvidencePackageDO> packages = evidencePackageMapper.selectListByProjectId(projectId);
        return new MesProEdhrDeliveryGateSummaryRespVO()
                .setProjectId(project.getId())
                .setProjectCode(project.getProjectCode())
                .setProjectStatus(project.getProjectStatus())
                .setSignoffAllowed(project.getSignoffAllowed())
                .setPackageCount(packages.size())
                .setGateCount(gateItems.size())
                .setBlockedCount(blockedCount)
                .setGateStatus(blockedCount > 0 ? GATE_STATUS_BLOCKED : "READY")
                .setSummary(blockedCount > 0
                        ? "存在缺失证据，当前不能进行商业化交付签核"
                        : "所有门禁项已具备证据，可进入签核")
                .setGateItems(BeanUtils.toBean(gateItems, MesProEdhrDeliveryGateItemRespVO.class));
    }

    private MesProEdhrDeliveryProjectDO requireProject(Long id) {
        MesProEdhrDeliveryProjectDO project = id == null ? null : projectMapper.selectById(id);
        if (project == null) {
            throw exception(PRO_EDHR_DELIVERY_PROJECT_NOT_EXISTS);
        }
        return project;
    }

    private void createEvidencePackageAndGate(MesProEdhrDeliveryProjectDO project, GateTemplate template, int sort) {
        MesProEdhrEvidencePackageDO evidencePackage = new MesProEdhrEvidencePackageDO()
                .setProjectId(project.getId())
                .setPackageCode(template.packageCode())
                .setPackageName(template.packageName())
                .setPackageType(template.packageType())
                .setPackageStatus(PACKAGE_STATUS_MISSING)
                .setEvidenceStatus(EVIDENCE_STATUS_MISSING)
                .setOwnerName(template.ownerName())
                .setOwnerDepartment(project.getOwnerDepartment())
                .setRequiredEvidenceJson(JsonUtils.toJsonString(template.requiredEvidence()))
                .setAvailableEvidenceJson("[]")
                .setMissingEvidenceJson(JsonUtils.toJsonString(List.of(template.missingEvidence())))
                .setSignoffImpact(template.signoffImpact())
                .setNextAction(template.nextAction());
        if (evidencePackageMapper.insert(evidencePackage) != 1 || evidencePackage.getId() == null) {
            throw exception(PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED);
        }

        MesProEdhrDeliveryGateItemDO gateItem = new MesProEdhrDeliveryGateItemDO()
                .setProjectId(project.getId())
                .setPackageId(evidencePackage.getId())
                .setGateCode(template.gateCode())
                .setGateName(template.gateName())
                .setGateStatus(GATE_STATUS_BLOCKED)
                .setMissingEvidence(template.missingEvidence())
                .setOwnerName(template.ownerName())
                .setNextAction(template.nextAction())
                .setSignoffImpact(template.signoffImpact())
                .setBlockingFlag(true)
                .setSort(sort);
        if (gateItemMapper.insert(gateItem) != 1 || gateItem.getId() == null) {
            throw exception(PRO_EDHR_DELIVERY_PROJECT_CREATE_FAILED);
        }
    }

    private String buildInitialGateSummaryJson(String projectCode) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("projectCode", projectCode);
        summary.put("gateStatus", GATE_STATUS_BLOCKED);
        summary.put("signoffAllowed", false);
        summary.put("blockedPackageCount", GATE_TEMPLATES.size());
        summary.put("missingPackageCodes", GATE_TEMPLATES.stream().map(GateTemplate::packageCode).toList());
        summary.put("firstSlice", "delivery-cockpit-evidence");
        return JsonUtils.toJsonString(summary);
    }

    private String buildProjectCode() {
        return "EDHR-DEL-" + PROJECT_CODE_TIME.format(LocalDateTime.now());
    }

    private record GateTemplate(String packageCode, String packageName, String packageType, String gateCode,
                                String gateName, List<String> requiredEvidence, String missingEvidence,
                                String ownerName, String nextAction, String signoffImpact) {
    }
}
