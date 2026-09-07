package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccRevisionSourceSelectionTest {

    @Test
    void revisionRequestCarriesExplicitSourceIterationAndReason() throws Exception {
        assertTrue(DccControlledFileSubmitReqVO.class.getDeclaredField("revisionSourceControlledFileId") != null);
        assertTrue(DccControlledFileSubmitReqVO.class.getDeclaredField("revisionSourceReason") != null);
        String service = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "service", "file", "DccControlledFileWorkflowServiceImpl.java"),
                StandardCharsets.UTF_8);
        assertTrue(service.contains("resolveExplicitRevisionSource"));
        assertTrue(service.contains("CONTROLLED_FILE_WORKFLOW_IN_PROGRESS"));
        assertTrue(service.contains("predecessorControlledFileId(context.reqVO().getRevisionSourceControlledFileId())"));
        assertTrue(service.contains("revisionBaseActiveControlledFileId"));
    }
}
