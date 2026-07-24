package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestArtifactRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface CodexTestArtifactService {

    CodexTestArtifactRespVO saveArtifact(Long executionCaseId, Integer checkpointSort, String artifactType, MultipartFile file);

    ArtifactFile getArtifactFile(Long artifactId);

    record ArtifactFile(File file, String contentType) {
    }

}
