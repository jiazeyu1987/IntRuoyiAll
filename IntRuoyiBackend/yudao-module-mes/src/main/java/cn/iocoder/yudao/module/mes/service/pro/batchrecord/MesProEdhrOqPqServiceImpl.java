package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRemediateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRetestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepResultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepSubmitReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationCaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationPackageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationRunDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationStepResultDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationCaseMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationDeviationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationPackageMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationRunMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrValidationStepResultMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_DEVIATION_OPEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_PACKAGE_NOT_OQ_READY;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_PQ_REAL_DATA_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_OQ_PQ_RUN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationErrorCodeConstants.PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS;

@Service
public class MesProEdhrOqPqServiceImpl implements MesProEdhrOqPqService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String CASE_TYPE_OQ = "OQ";
    private static final String CASE_TYPE_PQ = "PQ";
    private static final String CASE_STATUS_ACTIVE = "ACTIVE";
    private static final String RUN_STATUS_CREATED = "CREATED";
    private static final String RUN_STATUS_RUNNING = "RUNNING";
    private static final String RUN_STATUS_DEVIATION_OPEN = "DEVIATION_OPEN";
    private static final String RUN_STATUS_PASSED = "PASSED";
    private static final String RUN_STATUS_BLOCKED = "BLOCKED";
    private static final String STEP_RESULT_PASS = "PASS";
    private static final String STEP_RESULT_FAIL = "FAIL";
    private static final String STEP_RESULT_BLOCKED = "BLOCKED";
    private static final String DEVIATION_STATUS_OPEN = "OPEN";
    private static final String DEVIATION_STATUS_REMEDIATED = "REMEDIATED";
    private static final String DEVIATION_STATUS_RETESTED = "RETESTED";
    private static final String DEVIATION_STATUS_CLOSED = "CLOSED";
    private static final Set<String> CASE_TYPES = Set.of(CASE_TYPE_OQ, CASE_TYPE_PQ);
    private static final Set<String> STEP_RESULTS = Set.of(STEP_RESULT_PASS, STEP_RESULT_FAIL, STEP_RESULT_BLOCKED);

    @Resource
    private MesProEdhrValidationPackageMapper packageMapper;
    @Resource
    private MesProEdhrValidationCaseMapper caseMapper;
    @Resource
    private MesProEdhrValidationRunMapper runMapper;
    @Resource
    private MesProEdhrValidationStepResultMapper stepResultMapper;
    @Resource
    private MesProEdhrValidationDeviationMapper deviationMapper;

    @Override
    public PageResult<MesProEdhrOqPqCaseRespVO> getCasePage(MesProEdhrOqPqCasePageReqVO reqVO) {
        return BeanUtils.toBean(caseMapper.selectPage(reqVO), MesProEdhrOqPqCaseRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqCaseRespVO createCase(MesProEdhrOqPqCaseCreateReqVO reqVO) {
        requirePackageOqReady(reqVO.getPackageId());
        validateCaseType(reqVO.getCaseType());
        MesProEdhrValidationCaseDO validationCase = new MesProEdhrValidationCaseDO()
                .setPackageId(reqVO.getPackageId())
                .setCaseCode(reqVO.getCaseCode())
                .setCaseName(reqVO.getCaseName())
                .setCaseType(reqVO.getCaseType())
                .setCaseVersion(reqVO.getCaseVersion())
                .setCaseStatus(CASE_STATUS_ACTIVE)
                .setStepNo(reqVO.getStepNo())
                .setStepTitle(reqVO.getStepTitle())
                .setExpectedResult(reqVO.getExpectedResult())
                .setEvidenceRequirement(reqVO.getEvidenceRequirement())
                .setOwnerName(reqVO.getOwnerName())
                .setReviewerName(reqVO.getReviewerName())
                .setSort(reqVO.getSort() == null ? 0 : reqVO.getSort())
                .setRemark(reqVO.getRemark());
        caseMapper.insert(validationCase);
        return BeanUtils.toBean(caseMapper.selectById(validationCase.getId()), MesProEdhrOqPqCaseRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrOqPqRunRespVO> getRunPage(MesProEdhrOqPqRunPageReqVO reqVO) {
        return BeanUtils.toBean(runMapper.selectPage(reqVO), MesProEdhrOqPqRunRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqRunRespVO createRun(MesProEdhrOqPqRunCreateReqVO reqVO) {
        MesProEdhrValidationCaseDO validationCase = requireCase(reqVO.getCaseId());
        requirePackageOqReady(reqVO.getPackageId());
        if (!reqVO.getPackageId().equals(validationCase.getPackageId())) {
            throw exception(PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS);
        }
        validateRequiredExecutionEvidence(reqVO);
        if (CASE_TYPE_PQ.equals(validationCase.getCaseType()) && missingPqEvidence(reqVO)) {
            throw exception(PRO_EDHR_OQ_PQ_PQ_REAL_DATA_REQUIRED);
        }

        MesProEdhrValidationRunDO run = new MesProEdhrValidationRunDO()
                .setPackageId(reqVO.getPackageId())
                .setCaseId(validationCase.getId())
                .setCaseType(validationCase.getCaseType())
                .setRunCode(buildRunCode(validationCase.getCaseType()))
                .setRunStatus(RUN_STATUS_RUNNING)
                .setExecutionEnvironment(reqVO.getExecutionEnvironment())
                .setReleaseTag(reqVO.getReleaseTag())
                .setSchemaVersion(reqVO.getSchemaVersion())
                .setExecutorName(reqVO.getExecutorName())
                .setReviewerName(reqVO.getReviewerName())
                .setExecutedAt(reqVO.getExecutedAt() == null ? LocalDateTime.now() : reqVO.getExecutedAt())
                .setRealBusinessPath(reqVO.getRealBusinessPath())
                .setRealTestDataSource(reqVO.getRealTestDataSource())
                .setTargetEnvironmentProof(reqVO.getTargetEnvironmentProof())
                .setAttachmentEvidence(reqVO.getAttachmentEvidence())
                .setEvidenceChecksum(reqVO.getEvidenceChecksum())
                .setOpenDeviationCount(0)
                .setBlockedReason("执行记录已创建，需提交步骤结果")
                .setNextAction("按受控步骤提交实际结果和证据")
                .setRemark(reqVO.getRemark());
        runMapper.insert(run);
        return BeanUtils.toBean(runMapper.selectById(run.getId()), MesProEdhrOqPqRunRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqStepResultRespVO submitStepResult(MesProEdhrOqPqStepSubmitReqVO reqVO) {
        MesProEdhrValidationRunDO run = requireRun(reqVO.getRunId());
        MesProEdhrValidationCaseDO validationCase = requireCase(run.getCaseId());
        validateStepResult(reqVO.getStepResult());
        if (StrUtil.isBlank(reqVO.getAttachmentEvidence()) || StrUtil.isBlank(reqVO.getEvidenceChecksum())) {
            throw exception(PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING);
        }

        MesProEdhrValidationStepResultDO stepResult = new MesProEdhrValidationStepResultDO()
                .setPackageId(run.getPackageId())
                .setCaseId(run.getCaseId())
                .setRunId(run.getId())
                .setStepNo(validationCase.getStepNo())
                .setStepTitle(validationCase.getStepTitle())
                .setExpectedResult(validationCase.getExpectedResult())
                .setActualResult(reqVO.getActualResult())
                .setStepResult(reqVO.getStepResult())
                .setExecutorName(run.getExecutorName())
                .setReviewerName(run.getReviewerName())
                .setExecutedAt(LocalDateTime.now())
                .setAttachmentEvidence(reqVO.getAttachmentEvidence())
                .setEvidenceChecksum(reqVO.getEvidenceChecksum())
                .setNextAction(STEP_RESULT_FAIL.equals(reqVO.getStepResult())
                        ? "已生成偏差，完成原因分析、整改、复测和关闭签核"
                        : "步骤结果已保存，可继续完成执行记录")
                .setRemark(reqVO.getRemark());
        stepResultMapper.insert(stepResult);

        if (STEP_RESULT_FAIL.equals(reqVO.getStepResult())) {
            MesProEdhrValidationDeviationDO deviation = createDeviation(run, validationCase, stepResult);
            stepResult.setDeviationId(deviation.getId());
            stepResultMapper.updateById(stepResult);
            run.setRunStatus(RUN_STATUS_DEVIATION_OPEN)
                    .setOpenDeviationCount(deviationMapper.countOpenByRunId(run.getId()))
                    .setBlockedReason("存在开放偏差，不能完成 OQ/PQ 通过")
                    .setNextAction("完成偏差原因、整改、复测、复核和关闭签核");
            runMapper.updateById(run);
        }

        return BeanUtils.toBean(stepResultMapper.selectById(stepResult.getId()), MesProEdhrOqPqStepResultRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqRunRespVO completeRun(Long runId) {
        MesProEdhrValidationRunDO run = requireRun(runId);
        int openDeviationCount = deviationMapper.countOpenByRunId(runId);
        if (openDeviationCount > 0) {
            run.setRunStatus(RUN_STATUS_DEVIATION_OPEN)
                    .setOpenDeviationCount(openDeviationCount)
                    .setBlockedReason("存在开放偏差，不能完成 OQ/PQ 通过")
                    .setNextAction("关闭所有偏差后重新完成执行");
            runMapper.updateById(run);
            throw exception(PRO_EDHR_OQ_PQ_DEVIATION_OPEN);
        }
        List<MesProEdhrValidationStepResultDO> stepResults = stepResultMapper.selectListByRunId(runId);
        if (stepResults.isEmpty() || stepResults.stream().noneMatch(step -> STEP_RESULT_PASS.equals(step.getStepResult()))) {
            run.setRunStatus(RUN_STATUS_BLOCKED)
                    .setOpenDeviationCount(0)
                    .setBlockedReason("缺少通过的步骤结果，不能完成执行")
                    .setNextAction("补齐步骤结果和证据后重新完成执行");
            runMapper.updateById(run);
            throw exception(PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING);
        }
        run.setRunStatus(RUN_STATUS_PASSED)
                .setOpenDeviationCount(0)
                .setConclusion("OQ/PQ执行步骤通过且无开放偏差；本切片不生成验证签核结论")
                .setBlockedReason("无开放偏差")
                .setNextAction("进入验证签核门禁前检查");
        runMapper.updateById(run);
        return BeanUtils.toBean(runMapper.selectById(run.getId()), MesProEdhrOqPqRunRespVO.class);
    }

    @Override
    public PageResult<MesProEdhrOqPqDeviationRespVO> getDeviationPage(MesProEdhrOqPqDeviationPageReqVO reqVO) {
        return BeanUtils.toBean(deviationMapper.selectPage(reqVO), MesProEdhrOqPqDeviationRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqDeviationRespVO remediateDeviation(MesProEdhrOqPqDeviationRemediateReqVO reqVO) {
        MesProEdhrValidationDeviationDO deviation = requireDeviation(reqVO.getDeviationId());
        ensureDeviationNotClosed(deviation);
        deviation.setRootCause(reqVO.getRootCause())
                .setRemediationAction(reqVO.getRemediationAction())
                .setRemediationOwnerName(reqVO.getRemediationOwnerName())
                .setDeviationStatus(DEVIATION_STATUS_REMEDIATED)
                .setBlockedReason("偏差已整改，等待复测")
                .setNextAction("提交复测结果、复测证据和复测复核人");
        deviationMapper.updateById(deviation);
        return BeanUtils.toBean(deviationMapper.selectById(deviation.getId()), MesProEdhrOqPqDeviationRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqDeviationRespVO retestDeviation(MesProEdhrOqPqDeviationRetestReqVO reqVO) {
        MesProEdhrValidationDeviationDO deviation = requireDeviation(reqVO.getDeviationId());
        ensureDeviationReadyForRetest(deviation);
        deviation.setRetestResult(reqVO.getRetestResult())
                .setRetestEvidence(reqVO.getRetestEvidence())
                .setRetestReviewerName(reqVO.getRetestReviewerName())
                .setDeviationStatus(DEVIATION_STATUS_RETESTED)
                .setBlockedReason("偏差已复测，等待关闭签核")
                .setNextAction("提交关闭签核人后关闭偏差");
        deviationMapper.updateById(deviation);
        return BeanUtils.toBean(deviationMapper.selectById(deviation.getId()), MesProEdhrOqPqDeviationRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOqPqDeviationRespVO closeDeviation(MesProEdhrOqPqDeviationCloseReqVO reqVO) {
        MesProEdhrValidationDeviationDO deviation = requireDeviation(reqVO.getDeviationId());
        ensureDeviationReadyForClose(deviation);
        deviation.setCloseSignoffName(reqVO.getCloseSignoffName())
                .setClosedAt(LocalDateTime.now())
                .setDeviationStatus(DEVIATION_STATUS_CLOSED)
                .setBlockedReason("偏差已关闭")
                .setNextAction("如全部步骤通过且无开放偏差，可完成执行");
        deviationMapper.updateById(deviation);
        syncRunOpenDeviationCount(deviation.getRunId());
        return BeanUtils.toBean(deviationMapper.selectById(deviation.getId()), MesProEdhrOqPqDeviationRespVO.class);
    }

    private MesProEdhrValidationPackageDO requirePackageOqReady(Long packageId) {
        MesProEdhrValidationPackageDO validationPackage = packageId == null ? null : packageMapper.selectById(packageId);
        if (validationPackage == null) {
            throw exception(PRO_EDHR_VALIDATION_PACKAGE_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(validationPackage.getOqReady())) {
            throw exception(PRO_EDHR_OQ_PQ_PACKAGE_NOT_OQ_READY);
        }
        return validationPackage;
    }

    private MesProEdhrValidationCaseDO requireCase(Long caseId) {
        MesProEdhrValidationCaseDO validationCase = caseId == null ? null : caseMapper.selectById(caseId);
        if (validationCase == null) {
            throw exception(PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS);
        }
        return validationCase;
    }

    private MesProEdhrValidationRunDO requireRun(Long runId) {
        MesProEdhrValidationRunDO run = runId == null ? null : runMapper.selectById(runId);
        if (run == null) {
            throw exception(PRO_EDHR_OQ_PQ_RUN_NOT_EXISTS);
        }
        return run;
    }

    private MesProEdhrValidationDeviationDO requireDeviation(Long deviationId) {
        MesProEdhrValidationDeviationDO deviation = deviationId == null ? null : deviationMapper.selectById(deviationId);
        if (deviation == null) {
            throw exception(PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED);
        }
        return deviation;
    }

    private void validateCaseType(String caseType) {
        if (!CASE_TYPES.contains(caseType)) {
            throw exception(PRO_EDHR_OQ_PQ_CASE_NOT_EXISTS);
        }
    }

    private void validateStepResult(String stepResult) {
        if (!STEP_RESULTS.contains(stepResult)) {
            throw exception(PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING);
        }
    }

    private void validateRequiredExecutionEvidence(MesProEdhrOqPqRunCreateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getExecutionEnvironment()) || StrUtil.isBlank(reqVO.getReleaseTag())
                || StrUtil.isBlank(reqVO.getSchemaVersion()) || StrUtil.isBlank(reqVO.getExecutorName())
                || StrUtil.isBlank(reqVO.getReviewerName()) || StrUtil.isBlank(reqVO.getAttachmentEvidence())
                || StrUtil.isBlank(reqVO.getEvidenceChecksum())) {
            throw exception(PRO_EDHR_OQ_PQ_RUN_EVIDENCE_MISSING);
        }
    }

    private boolean missingPqEvidence(MesProEdhrOqPqRunCreateReqVO reqVO) {
        return StrUtil.isBlank(reqVO.getRealBusinessPath())
                || StrUtil.isBlank(reqVO.getRealTestDataSource())
                || StrUtil.isBlank(reqVO.getTargetEnvironmentProof());
    }

    private MesProEdhrValidationDeviationDO createDeviation(MesProEdhrValidationRunDO run,
                                                            MesProEdhrValidationCaseDO validationCase,
                                                            MesProEdhrValidationStepResultDO stepResult) {
        MesProEdhrValidationDeviationDO deviation = new MesProEdhrValidationDeviationDO()
                .setPackageId(run.getPackageId())
                .setCaseId(run.getCaseId())
                .setRunId(run.getId())
                .setStepResultId(stepResult.getId())
                .setDeviationCode(buildDeviationCode(run.getCaseType()))
                .setDeviationTitle(validationCase.getCaseCode() + "-" + validationCase.getStepNo() + "执行失败")
                .setDeviationStatus(DEVIATION_STATUS_OPEN)
                .setFailedActualResult(stepResult.getActualResult())
                .setBlockedReason("失败项已生成偏差，关闭前阻断执行通过")
                .setNextAction("补充原因、整改、复测、复核和关闭签核");
        deviationMapper.insert(deviation);
        return deviation;
    }

    private void ensureDeviationNotClosed(MesProEdhrValidationDeviationDO deviation) {
        if (DEVIATION_STATUS_CLOSED.equals(deviation.getDeviationStatus())) {
            throw exception(PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED);
        }
    }

    private void ensureDeviationReadyForRetest(MesProEdhrValidationDeviationDO deviation) {
        ensureDeviationNotClosed(deviation);
        if (StrUtil.isBlank(deviation.getRootCause()) || StrUtil.isBlank(deviation.getRemediationAction())
                || StrUtil.isBlank(deviation.getRemediationOwnerName())) {
            throw exception(PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED);
        }
    }

    private void ensureDeviationReadyForClose(MesProEdhrValidationDeviationDO deviation) {
        if (StrUtil.isBlank(deviation.getRootCause()) || StrUtil.isBlank(deviation.getRemediationAction())
                || StrUtil.isBlank(deviation.getRemediationOwnerName()) || StrUtil.isBlank(deviation.getRetestResult())
                || StrUtil.isBlank(deviation.getRetestEvidence()) || StrUtil.isBlank(deviation.getRetestReviewerName())) {
            throw exception(PRO_EDHR_OQ_PQ_DEVIATION_CLOSE_REQUIRED);
        }
    }

    private void syncRunOpenDeviationCount(Long runId) {
        MesProEdhrValidationRunDO run = requireRun(runId);
        int openDeviationCount = deviationMapper.countOpenByRunId(runId);
        run.setOpenDeviationCount(openDeviationCount);
        if (openDeviationCount == 0 && RUN_STATUS_DEVIATION_OPEN.equals(run.getRunStatus())) {
            run.setRunStatus(RUN_STATUS_RUNNING)
                    .setBlockedReason("偏差已关闭，等待重新提交通过步骤或完成执行")
                    .setNextAction("确认步骤结果为 PASS 后完成执行");
        }
        runMapper.updateById(run);
    }

    private String buildRunCode(String caseType) {
        return "EDHR-" + caseType + "-RUN-" + CODE_TIME.format(LocalDateTime.now());
    }

    private String buildDeviationCode(String caseType) {
        return "EDHR-" + caseType + "-DEV-" + CODE_TIME.format(LocalDateTime.now());
    }
}
