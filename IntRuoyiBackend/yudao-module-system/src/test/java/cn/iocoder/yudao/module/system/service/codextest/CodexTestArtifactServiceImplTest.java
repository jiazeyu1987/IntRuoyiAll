package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestArtifactRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestArtifactDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointResultDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestArtifactMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointResultMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(CodexTestArtifactServiceImpl.class)
class CodexTestArtifactServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CodexTestArtifactService codexTestArtifactService;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;
    @Resource
    private CodexTestArtifactMapper codexTestArtifactMapper;

    @TempDir
    private Path artifactTempDir;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(codexTestArtifactService, "artifactTempDir", artifactTempDir.toString());
        ReflectionTestUtils.setField(codexTestArtifactService, "artifactRetentionHours", 24);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void saveArtifact_stampsAuditFieldsWithoutLoginUser() {
        Long executionCaseId = insertExecutionCaseWithCheckpoint();
        MockMultipartFile file = new MockMultipartFile("file", "checkpoint.png", "image/png",
                "evidence".getBytes(StandardCharsets.UTF_8));

        CodexTestArtifactRespVO respVO = codexTestArtifactService.saveArtifact(executionCaseId, 1, "SCREENSHOT", file);

        CodexTestArtifactDO artifact = codexTestArtifactMapper.selectById(respVO.getArtifactId());
        assertEquals("codex-runner", artifact.getCreator());
        assertEquals("codex-runner", artifact.getUpdater());
    }

    private Long insertExecutionCaseWithCheckpoint() {
        CodexTestExecutionDO execution = new CodexTestExecutionDO();
        execution.setTargetTenantId(88L);
        execution.setExecutionMode("SEQUENTIAL");
        execution.setStatus("RUNNING");
        execution.setRequestedBy(99L);
        codexTestExecutionMapper.insert(execution);

        CodexTestExecutionCaseDO executionCase = new CodexTestExecutionCaseDO();
        executionCase.setExecutionId(execution.getId());
        executionCase.setCaseId(100L);
        executionCase.setCaseNameSnapshot("作废测试");
        executionCase.setMethodTextSnapshot("点击作废按钮");
        executionCase.setCheckpointCount(1);
        executionCase.setStatus("RUNNING");
        codexTestExecutionCaseMapper.insert(executionCase);

        CodexTestCheckpointResultDO checkpointResult = new CodexTestCheckpointResultDO();
        checkpointResult.setExecutionCaseId(executionCase.getId());
        checkpointResult.setCheckpointSort(1);
        checkpointResult.setCheckpointNameSnapshot("检查作废结果");
        checkpointResult.setExpectedTextSnapshot("页面显示已作废");
        checkpointResult.setStatus("NOT_RUN");
        codexTestCheckpointResultMapper.insert(checkpointResult);
        return executionCase.getId();
    }

}
