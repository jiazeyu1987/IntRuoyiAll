package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionSpecialNodeCompleteReqVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeCompleteResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProductionReleaseReportControllerContractTest {

    @Test
    void reportCompletionSerializesUnsafeLongIdsAsStrings() throws Exception {
        MesProductionReleaseReportNodeCompleteResult result =
                new MesProductionReleaseReportNodeCompleteResult()
                        .setBatchExecutionId(9007199254740993L)
                        .setBatchTaskId(9007199254740994L)
                        .setWorkTaskId(9007199254740995L)
                        .setNodeType("FINISHED_PRODUCT_INSPECTION_RECORD")
                        .setNodeStatus("COMPLETED")
                        .setActiveAttachmentVersion(1)
                        .setAttachmentIds(List.of(101L))
                        .setAttachmentHashes(List.of("a".repeat(64)))
                        .setReportUploadStatus("MANAGER_RELEASE_PENDING")
                        .setReleaseTransactionId(9007199254740996L)
                        .setManagerReleaseWorkTaskId(9007199254740997L)
                        .setVersion(6);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));

        for (String field : List.of("batchExecutionId", "batchTaskId", "workTaskId",
                "releaseTransactionId", "managerReleaseWorkTaskId")) {
            assertTrue(json.get(field).isTextual(), field + " must be serialized as a string");
        }
        assertEquals("MANAGER_RELEASE_PENDING", json.get("reportUploadStatus").asText());
        assertEquals(6, json.get("version").asInt());
    }

    @Test
    void reusedSpecialNodeEndpointsExposeVersionAndIdempotencyFields() throws Exception {
        assertNotNull(EdhrBatchExecutionSpecialNodeCompleteReqVO.class.getDeclaredField("expectedVersion"));
        assertNotNull(EdhrBatchExecutionSpecialNodeCompleteReqVO.class.getDeclaredField("idempotencyKey"));
        assertNotNull(EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO.class
                .getDeclaredField("expectedVersion"));
        assertNotNull(EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO.class
                .getDeclaredField("idempotencyKey"));
        assertNotNull(EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadRespVO.class
                .getDeclaredField("version"));

        Method complete = MesProEdhrBatchExecutionController.class.getMethod(
                "completeSpecialNode", EdhrBatchExecutionSpecialNodeCompleteReqVO.class);
        Method prepare = MesProEdhrBatchExecutionController.class.getMethod(
                "prepareSpecialNodeAttachmentUpload",
                EdhrBatchExecutionSpecialNodeAttachmentPrepareUploadReqVO.class,
                MultipartFile.class);
        assertEquals("/task/special-node/complete", complete.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/task/special-node/attachment/prepare-upload",
                prepare.getAnnotation(PostMapping.class).value()[0]);
    }
}
