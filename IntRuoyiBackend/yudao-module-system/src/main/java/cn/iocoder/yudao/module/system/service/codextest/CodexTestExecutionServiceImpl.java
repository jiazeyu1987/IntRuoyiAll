package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionCancelReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionStartReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointResultDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointResultMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionMapper;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_CASE_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_DISABLED_CASE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_RUNNING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_PARALLEL_UNSAFE_CASE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_TARGET_TENANT_INVALID;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.*;

@Service
@Validated
public class CodexTestExecutionServiceImpl implements CodexTestExecutionService {

    @Resource
    private CodexTestCaseMapper codexTestCaseMapper;
    @Resource
    private CodexTestCheckpointMapper codexTestCheckpointMapper;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;
    @Resource
    private CodexTestRunnerBootstrapService codexTestRunnerBootstrapService;
    @Resource
    private TenantService tenantService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startExecution(CodexTestExecutionStartReqVO startReqVO, Long requestedBy) {
        validateStartReqVO(startReqVO, requestedBy);
        validateTargetTenant(startReqVO.getTargetTenantId());
        List<CodexTestCaseDO> cases = getOrderedCases(startReqVO.getCaseIds(), startReqVO.getExecutionMode());
        validateExecutableCases(cases, startReqVO.getExecutionMode());
        codexTestRunnerBootstrapService.ensureRunnerAvailable();

        CodexTestExecutionDO execution = new CodexTestExecutionDO();
        execution.setTargetTenantId(startReqVO.getTargetTenantId());
        execution.setExecutionMode(startReqVO.getExecutionMode());
        execution.setNodeChainExecution(cases.stream()
                .allMatch(testCase -> StrUtil.isNotBlank(testCase.getNodeChainName())));
        execution.setStatus(EXECUTION_PENDING);
        execution.setRequestedBy(requestedBy);
        codexTestExecutionMapper.insert(execution);

        for (CodexTestCaseDO testCase : cases) {
            List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(testCase.getId());
            CodexTestExecutionCaseDO executionCase = new CodexTestExecutionCaseDO();
            executionCase.setExecutionId(execution.getId());
            executionCase.setCaseId(testCase.getId());
            executionCase.setCaseNameSnapshot(testCase.getName());
            executionCase.setMethodTextSnapshot(testCase.getMethodText());
            executionCase.setTestDataTextSnapshot(testCase.getTestDataText());
            executionCase.setCheckpointCount(checkpoints.size());
            executionCase.setStatus(EXECUTION_PENDING);
            codexTestExecutionCaseMapper.insert(executionCase);

            for (CodexTestCheckpointDO checkpoint : checkpoints) {
                CodexTestCheckpointResultDO checkpointResult = new CodexTestCheckpointResultDO();
                checkpointResult.setExecutionCaseId(executionCase.getId());
                checkpointResult.setCheckpointSort(checkpoint.getSort());
                checkpointResult.setCheckpointNameSnapshot(checkpoint.getName());
                checkpointResult.setExpectedTextSnapshot(checkpoint.getExpectedText());
                checkpointResult.setStatus(CHECKPOINT_NOT_RUN);
                codexTestCheckpointResultMapper.insert(checkpointResult);
            }
        }
        return execution.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExecution(CodexTestExecutionCancelReqVO cancelReqVO) {
        CodexTestExecutionDO execution = validateExecutionExists(cancelReqVO.getExecutionId());
        codexTestExecutionCaseMapper.cancelByExecutionId(execution.getId());
        codexTestExecutionMapper.updateStatus(execution.getId(), EXECUTION_CANCELED,
                execution.getStartedAt(), LocalDateTime.now(), "用户取消执行", execution.getRunnerSessionId());
    }

    @Override
    public PageResult<CodexTestExecutionRespVO> getExecutionPage(CodexTestExecutionPageReqVO pageReqVO) {
        PageResult<CodexTestExecutionDO> pageResult = codexTestExecutionMapper.selectPage(pageReqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), CodexTestExecutionRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public CodexTestExecutionRespVO getExecution(Long id) {
        CodexTestExecutionDO execution = validateExecutionExists(id);
        return buildExecutionResp(execution);
    }

    @Override
    public List<CodexTestExecutionRespVO> getExecutionMonitor() {
        return CollectionUtils.convertList(codexTestExecutionMapper.selectMonitorList(), this::buildExecutionResp);
    }

    private CodexTestExecutionRespVO buildExecutionResp(CodexTestExecutionDO execution) {
        CodexTestExecutionRespVO respVO = BeanUtils.toBean(execution, CodexTestExecutionRespVO.class);
        List<CodexTestExecutionCaseDO> executionCases = codexTestExecutionCaseMapper.selectListByExecutionId(execution.getId());
        List<Long> executionCaseIds = CollectionUtils.convertList(executionCases, CodexTestExecutionCaseDO::getId);
        Map<Long, List<CodexTestCheckpointResultDO>> resultMap = CollectionUtils.convertMultiMap(
                CollUtil.isEmpty(executionCaseIds) ? List.of() :
                        codexTestCheckpointResultMapper.selectListByExecutionCaseIds(executionCaseIds),
                CodexTestCheckpointResultDO::getExecutionCaseId);
        respVO.setCases(CollectionUtils.convertList(executionCases, executionCase -> {
            CodexTestExecutionRespVO.CaseResult caseResult =
                    BeanUtils.toBean(executionCase, CodexTestExecutionRespVO.CaseResult.class);
            caseResult.setCheckpointResults(BeanUtils.toBean(resultMap.getOrDefault(executionCase.getId(), List.of()),
                    CodexTestExecutionRespVO.CheckpointResult.class));
            return caseResult;
        }));
        return respVO;
    }

    @Override
    public void rollupExecution(Long executionId) {
        CodexTestExecutionDO execution = validateExecutionExists(executionId);
        String summary = execution.getSummary();
        if (Boolean.TRUE.equals(execution.getNodeChainExecution())
                && codexTestExecutionCaseMapper.selectFailedCountByExecutionId(executionId) > 0) {
            List<CodexTestExecutionCaseDO> pendingCases =
                    codexTestExecutionCaseMapper.selectPendingListByExecutionId(executionId);
            if (CollUtil.isNotEmpty(pendingCases)) {
                LocalDateTime blockedAt = LocalDateTime.now();
                codexTestExecutionCaseMapper.blockPendingByExecutionId(
                        executionId, SEQUENTIAL_BLOCK_REASON, blockedAt);
                codexTestCheckpointResultMapper.blockNotRunByExecutionCaseIds(
                        CollectionUtils.convertList(pendingCases, CodexTestExecutionCaseDO::getId),
                        SEQUENTIAL_BLOCK_REASON, blockedAt);
                summary = SEQUENTIAL_BLOCK_REASON;
            }
        }
        if (codexTestExecutionCaseMapper.selectUnfinishedCountByExecutionId(executionId) > 0) {
            codexTestExecutionMapper.updateStatus(executionId, EXECUTION_RUNNING, execution.getStartedAt(),
                    null, summary, execution.getRunnerSessionId());
            return;
        }
        String status = codexTestExecutionCaseMapper.selectFailedCountByExecutionId(executionId) > 0
                ? EXECUTION_FAIL : EXECUTION_PASS;
        codexTestExecutionMapper.updateStatus(executionId, status, execution.getStartedAt(),
                LocalDateTime.now(), summary, execution.getRunnerSessionId());
    }

    private void validateStartReqVO(CodexTestExecutionStartReqVO startReqVO, Long requestedBy) {
        if (requestedBy == null) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行发起人不能为空");
        }
        if (!EXECUTION_MODES.contains(startReqVO.getExecutionMode())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行方式必须是 SEQUENTIAL 或 PARALLEL");
        }
        if (CollUtil.isEmpty(startReqVO.getCaseIds())) {
            throw exception(CODEX_TEST_CASE_NOT_EXISTS);
        }
    }

    private void validateTargetTenant(Long targetTenantId) {
        try {
            tenantService.validTenant(targetTenantId);
        } catch (ServiceException ex) {
            throw exception(CODEX_TEST_TARGET_TENANT_INVALID, ex.getMessage());
        }
    }

    private List<CodexTestCaseDO> getOrderedCases(List<Long> caseIds, String executionMode) {
        List<CodexTestCaseDO> dbCases = codexTestCaseMapper.selectListByIds(caseIds);
        Map<Long, CodexTestCaseDO> caseMap = CollectionUtils.convertMap(dbCases, CodexTestCaseDO::getId);
        if (caseMap.size() != caseIds.stream().distinct().count()) {
            throw exception(CODEX_TEST_CASE_NOT_EXISTS);
        }
        List<CodexTestCaseDO> selectedCases = caseIds.stream().distinct().map(caseMap::get).toList();
        String nodeChainName = validateNodeChainSelection(selectedCases, executionMode);
        if (nodeChainName == null) {
            return selectedCases;
        }
        return selectedCases.stream()
                .sorted(Comparator.comparing(CodexTestCaseDO::getNodeChainSort))
                .toList();
    }

    private String validateNodeChainSelection(List<CodexTestCaseDO> cases, String executionMode) {
        List<CodexTestCaseDO> nodeChainCases = cases.stream()
                .filter(testCase -> StrUtil.isNotBlank(testCase.getNodeChainName()))
                .toList();
        if (nodeChainCases.isEmpty()) {
            return null;
        }
        Set<String> nodeChainNames = CollectionUtils.convertSet(
                nodeChainCases, CodexTestCaseDO::getNodeChainName);
        if (nodeChainNames.size() != 1) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "一次执行只能选择一个节点串");
        }
        if (nodeChainCases.size() != cases.size()) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项不能与独立测试项混合执行");
        }
        if (!MODE_SEQUENTIAL.equals(executionMode)) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串只能使用顺序执行");
        }
        if (nodeChainCases.stream().anyMatch(testCase -> testCase.getNodeChainSort() == null
                || testCase.getNodeChainSort() <= 0
                || !MODE_SEQUENTIAL.equals(testCase.getDefaultExecutionMode())
                || Boolean.TRUE.equals(testCase.getParallelSafe()))) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串配置不完整或执行控制不正确");
        }
        if (nodeChainCases.stream().map(CodexTestCaseDO::getNodeChainSort).distinct().count()
                != nodeChainCases.size()) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串内存在重复序号");
        }
        List<CodexTestCaseDO> completeNodeChain = codexTestCaseMapper.selectListByNodeChainName(
                nodeChainNames.iterator().next());
        int maxNodeChainSort = completeNodeChain.stream()
                .map(CodexTestCaseDO::getNodeChainSort)
                .max(Integer::compareTo)
                .orElse(0);
        Set<Long> selectedCaseIds = CollectionUtils.convertSet(nodeChainCases, CodexTestCaseDO::getId);
        Set<Long> completeNodeChainCaseIds = CollectionUtils.convertSet(completeNodeChain, CodexTestCaseDO::getId);
        if (completeNodeChain.size() != maxNodeChainSort
                || !selectedCaseIds.equals(completeNodeChainCaseIds)) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串必须从第 1 节点开始连续选择");
        }
        return nodeChainNames.iterator().next();
    }

    private void validateExecutableCases(List<CodexTestCaseDO> cases, String executionMode) {
        for (CodexTestCaseDO testCase : cases) {
            if (!STATUS_ENABLE.equals(testCase.getStatus())) {
                throw exception(CODEX_TEST_DISABLED_CASE, testCase.getName());
            }
            if (codexTestExecutionCaseMapper.selectRunningCountByCaseId(testCase.getId()) > 0) {
                throw exception(CODEX_TEST_EXECUTION_RUNNING);
            }
        }
        if (MODE_PARALLEL.equals(executionMode)) {
            String unsafeNames = cases.stream()
                    .filter(testCase -> !Boolean.TRUE.equals(testCase.getParallelSafe()))
                    .map(CodexTestCaseDO::getName)
                    .reduce((left, right) -> left + "," + right)
                    .orElse(null);
            if (unsafeNames != null) {
                throw exception(CODEX_TEST_PARALLEL_UNSAFE_CASE, unsafeNames);
            }
        }
    }

    private CodexTestExecutionDO validateExecutionExists(Long id) {
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(id);
        if (execution == null) {
            throw exception(CODEX_TEST_EXECUTION_NOT_EXISTS);
        }
        return execution;
    }

}
