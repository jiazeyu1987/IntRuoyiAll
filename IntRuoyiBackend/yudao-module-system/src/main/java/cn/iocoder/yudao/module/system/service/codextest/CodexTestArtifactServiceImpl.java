package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestArtifactRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestArtifactDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointResultDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestArtifactMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointResultMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_ARTIFACT_NOT_FOUND;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;

@Service
public class CodexTestArtifactServiceImpl implements CodexTestArtifactService {

    @Value("${yudao.codex-test.artifact-temp-dir:}")
    private String artifactTempDir;
    @Value("${yudao.codex-test.artifact-retention-hours:24}")
    private Integer artifactRetentionHours;

    @Resource
    private CodexTestArtifactMapper codexTestArtifactMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;

    @Override
    public CodexTestArtifactRespVO saveArtifact(Long executionCaseId, Integer checkpointSort, String artifactType, MultipartFile file) {
        if (StrUtil.isBlank(artifactTempDir)) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "artifact 临时目录未配置");
        }
        if (file == null || file.isEmpty()) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "artifact 文件不能为空");
        }
        CodexTestExecutionCaseDO executionCase = codexTestExecutionCaseMapper.selectById(executionCaseId);
        if (executionCase == null) {
            throw exception(CODEX_TEST_EXECUTION_NOT_EXISTS);
        }
        CodexTestCheckpointResultDO checkpointResult =
                codexTestCheckpointResultMapper.selectByCaseAndSort(executionCaseId, checkpointSort);
        if (checkpointResult == null) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "检查点结果不存在");
        }

        String relativePath = executionCase.getExecutionId() + "/" + UUID.randomUUID() + ".bin";
        File targetFile = new File(artifactTempDir, relativePath);
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "artifact 临时目录无法创建");
        }
        try {
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "artifact 文件保存失败：" + ex.getMessage());
        }

        CodexTestArtifactDO artifact = new CodexTestArtifactDO();
        artifact.setExecutionId(executionCase.getExecutionId());
        artifact.setExecutionCaseId(executionCaseId);
        artifact.setCheckpointResultId(checkpointResult.getId());
        artifact.setArtifactType(artifactType);
        artifact.setRelativeTempPath(relativePath);
        artifact.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        artifact.setSizeBytes(file.getSize());
        artifact.setSha256(DigestUtil.sha256Hex(targetFile));
        artifact.setExpiresAt(LocalDateTime.now().plusHours(artifactRetentionHours));
        CodexTestRunnerAuditSupport.stampRunnerAudit(artifact);
        codexTestArtifactMapper.insert(artifact);
        CodexTestArtifactRespVO respVO = new CodexTestArtifactRespVO();
        respVO.setArtifactId(artifact.getId());
        return respVO;
    }

    @Override
    public ArtifactFile getArtifactFile(Long artifactId) {
        CodexTestArtifactDO artifact = codexTestArtifactMapper.selectById(artifactId);
        if (artifact == null || artifact.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw exception(CODEX_TEST_ARTIFACT_NOT_FOUND);
        }
        File file = new File(artifactTempDir, artifact.getRelativeTempPath());
        if (!file.isFile()) {
            throw exception(CODEX_TEST_ARTIFACT_NOT_FOUND);
        }
        return new ArtifactFile(file, artifact.getContentType());
    }

}
