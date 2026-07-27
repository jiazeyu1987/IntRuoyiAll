package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCheckpointSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestNodeChainOptionRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_CASE_EMPTY_CHECKPOINT;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_CASE_EMPTY_METHOD;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_CASE_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_RUNNING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.CASE_PROJECTS;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.CASE_STATUSES;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.EXECUTION_MODES;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.MODE_SEQUENTIAL;

@Service
@Validated
public class CodexTestCaseServiceImpl implements CodexTestCaseService {

    @Resource
    private CodexTestCaseMapper codexTestCaseMapper;
    @Resource
    private CodexTestCheckpointMapper codexTestCheckpointMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCase(CodexTestCaseSaveReqVO createReqVO) {
        validateSaveReqVO(createReqVO, false);
        CodexTestCaseDO testCase = BeanUtils.toBean(createReqVO, CodexTestCaseDO.class);
        codexTestCaseMapper.insert(testCase);
        insertCheckpoints(testCase.getId(), createReqVO.getCheckpoints());
        return testCase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(CodexTestCaseSaveReqVO updateReqVO) {
        validateSaveReqVO(updateReqVO, true);
        validateCaseExists(updateReqVO.getId());
        CodexTestCaseDO updateObj = BeanUtils.toBean(updateReqVO, CodexTestCaseDO.class);
        codexTestCaseMapper.updateById(updateObj);
        codexTestCheckpointMapper.deleteByCaseId(updateReqVO.getId());
        insertCheckpoints(updateReqVO.getId(), updateReqVO.getCheckpoints());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        validateCaseExists(id);
        if (codexTestExecutionCaseMapper.selectRunningCountByCaseId(id) > 0) {
            throw exception(CODEX_TEST_EXECUTION_RUNNING);
        }
        codexTestCheckpointMapper.deleteByCaseId(id);
        codexTestCaseMapper.deleteById(id);
    }

    @Override
    public CodexTestCaseRespVO getCase(Long id) {
        CodexTestCaseDO testCase = validateCaseExists(id);
        CodexTestCaseRespVO respVO = BeanUtils.toBean(testCase, CodexTestCaseRespVO.class);
        respVO.setCheckpoints(CollectionUtils.convertList(codexTestCheckpointMapper.selectListByCaseId(id), this::toCheckpointResp));
        respVO.setCheckpointCount(respVO.getCheckpoints().size());
        return respVO;
    }

    @Override
    public PageResult<CodexTestCaseRespVO> getCasePage(CodexTestCasePageReqVO pageReqVO) {
        PageResult<CodexTestCaseDO> pageResult = codexTestCaseMapper.selectPage(pageReqVO);
        List<Long> caseIds = CollectionUtils.convertList(pageResult.getList(), CodexTestCaseDO::getId);
        Map<Long, List<CodexTestCheckpointDO>> checkpointMap = CollectionUtils.convertMultiMap(
                CollUtil.isEmpty(caseIds) ? List.of() : codexTestCheckpointMapper.selectListByCaseIds(caseIds),
                CodexTestCheckpointDO::getCaseId);
        List<CodexTestCaseRespVO> list = CollectionUtils.convertList(pageResult.getList(), testCase -> {
            CodexTestCaseRespVO respVO = BeanUtils.toBean(testCase, CodexTestCaseRespVO.class);
            List<CodexTestCheckpointDO> checkpoints = checkpointMap.getOrDefault(testCase.getId(), List.of());
            respVO.setCheckpointCount(checkpoints.size());
            respVO.setCheckpoints(CollectionUtils.convertList(checkpoints, this::toCheckpointResp));
            return respVO;
        });
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<CodexTestNodeChainOptionRespVO> getNodeChainOptions() {
        Map<String, CodexTestNodeChainOptionRespVO> optionMap = new LinkedHashMap<>();
        for (CodexTestCaseDO testCase : codexTestCaseMapper.selectNodeChainCases()) {
            CodexTestNodeChainOptionRespVO option = optionMap.computeIfAbsent(testCase.getNodeChainName(), name -> {
                CodexTestNodeChainOptionRespVO newOption = new CodexTestNodeChainOptionRespVO();
                newOption.setName(name);
                newOption.setProject(testCase.getProject());
                newOption.setNodeCount(0);
                return newOption;
            });
            option.setNodeCount(option.getNodeCount() + 1);
        }
        return List.copyOf(optionMap.values());
    }

    private CodexTestCaseDO validateCaseExists(Long id) {
        CodexTestCaseDO testCase = codexTestCaseMapper.selectById(id);
        if (testCase == null) {
            throw exception(CODEX_TEST_CASE_NOT_EXISTS);
        }
        return testCase;
    }

    private void validateSaveReqVO(CodexTestCaseSaveReqVO reqVO, boolean update) {
        if (update && reqVO.getId() == null) {
            throw exception(CODEX_TEST_CASE_NOT_EXISTS);
        }
        normalizeAndValidateNodeChain(reqVO);
        if (StrUtil.isBlank(reqVO.getMethodText())) {
            throw exception(CODEX_TEST_CASE_EMPTY_METHOD);
        }
        if (StrUtil.isBlank(reqVO.getProject()) || !CASE_PROJECTS.contains(reqVO.getProject())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "测试项项目必须是 智能排产、文控、批记录 或 工艺路线");
        }
        if (CollUtil.isEmpty(reqVO.getCheckpoints())) {
            throw exception(CODEX_TEST_CASE_EMPTY_CHECKPOINT);
        }
        if (!EXECUTION_MODES.contains(reqVO.getDefaultExecutionMode())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "默认执行方式必须是 SEQUENTIAL 或 PARALLEL");
        }
        if (reqVO.getParallelSafe() == null) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "是否并行安全不能为空");
        }
        if (!CASE_STATUSES.contains(reqVO.getStatus())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "测试项状态必须是 ENABLE 或 DISABLE");
        }
        for (CodexTestCheckpointSaveReqVO checkpoint : reqVO.getCheckpoints()) {
            if (checkpoint.getSort() == null || checkpoint.getSort() <= 0) {
                throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "检查点排序必须大于 0");
            }
            if (StrUtil.isBlank(checkpoint.getName())) {
                throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "检查点名称不能为空");
            }
            if (StrUtil.isBlank(checkpoint.getExpectedText())) {
                throw exception(CODEX_TEST_CASE_EMPTY_CHECKPOINT);
            }
        }
    }

    private void normalizeAndValidateNodeChain(CodexTestCaseSaveReqVO reqVO) {
        String nodeChainName = StrUtil.trim(reqVO.getNodeChainName());
        if (StrUtil.isBlank(nodeChainName)) {
            reqVO.setNodeChainName(null);
            if (reqVO.getNodeChainSort() != null) {
                throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "未填写节点串时串内序号必须为空");
            }
            return;
        }
        reqVO.setNodeChainName(nodeChainName);
        if (reqVO.getNodeChainSort() == null || reqVO.getNodeChainSort() <= 0) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项的串内序号必须大于 0");
        }
        if (!MODE_SEQUENTIAL.equals(reqVO.getDefaultExecutionMode())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项只能使用顺序执行");
        }
        if (Boolean.TRUE.equals(reqVO.getParallelSafe())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项不允许标记为并行安全");
        }
        if (codexTestCaseMapper.selectCountByNodeChainNameAndSort(
                nodeChainName, reqVO.getNodeChainSort(), reqVO.getId()) > 0) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID,
                    "节点串【" + nodeChainName + "】已存在第 " + reqVO.getNodeChainSort() + " 节点");
        }
        boolean differentProjectExists = codexTestCaseMapper.selectListByNodeChainName(nodeChainName).stream()
                .filter(testCase -> !Objects.equals(testCase.getId(), reqVO.getId()))
                .anyMatch(testCase -> !Objects.equals(testCase.getProject(), reqVO.getProject()));
        if (differentProjectExists) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "同一节点串内的所属项目必须一致");
        }
    }

    private void insertCheckpoints(Long caseId, List<CodexTestCheckpointSaveReqVO> checkpoints) {
        checkpoints.forEach(checkpoint -> {
            CodexTestCheckpointDO checkpointDO = BeanUtils.toBean(checkpoint, CodexTestCheckpointDO.class);
            checkpointDO.setCaseId(caseId);
            codexTestCheckpointMapper.insert(checkpointDO);
        });
    }

    private CodexTestCaseRespVO.Checkpoint toCheckpointResp(CodexTestCheckpointDO checkpoint) {
        return BeanUtils.toBean(checkpoint, CodexTestCaseRespVO.Checkpoint.class);
    }

}
