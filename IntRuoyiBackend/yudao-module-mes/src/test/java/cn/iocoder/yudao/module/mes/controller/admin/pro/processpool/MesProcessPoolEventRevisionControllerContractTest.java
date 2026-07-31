package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolEventRevisionControllerContractTest {

    private static final Path CONTROLLER = Path.of("src/main/java/cn/iocoder/yudao/module/mes/"
            + "controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java");
    private static final Path REQ_VO = Path.of("src/main/java/cn/iocoder/yudao/module/mes/"
            + "controller/admin/pro/processpool/vo/ProcessPoolEventRevisionUpdateReqVO.java");

    @Test
    void exposesDedicatedWriteEndpointWithUpdatePermissionAndServiceCall() throws IOException {
        String controller = Files.readString(CONTROLLER);

        assertTrue(controller.contains("@RequestMapping(\"/mes/pro/process-pool/event-revision\")"),
                "F6 revision write controller must expose the approved process-pool event-revision base path.");
        assertTrue(controller.contains("@PostMapping(\"/update-original\")"),
                "F6 revision write endpoint URL must remain stable.");
        assertTrue(controller.contains("@PreAuthorize(\"@ss.hasPermission('mes:pro-process-pool:event-revision:update')\")"),
                "F6 revision write endpoint must use a dedicated write permission.");
        assertFalse(controller.contains("mes:pro-process-pool:query"),
                "F6 revision write endpoint must not reuse timeline/query permission.");
        assertTrue(controller.contains("mesProcessPoolEventRevisionService.updateOriginalRecord"),
                "Controller must delegate to MesProcessPoolEventRevisionService.");
        assertTrue(controller.contains("@Valid @RequestBody ProcessPoolEventRevisionUpdateReqVO reqVO"),
                "Controller must validate the request body before calling service.");
    }

    @Test
    void requestVoContainsRequiredRevisionFieldsAndFifoDiffFields() throws IOException {
        String reqVO = Files.readString(REQ_VO);

        assertTrue(reqVO.contains("private Long eventId;"), "Request must include eventId.");
        assertTrue(reqVO.contains("private String afterPayload;"), "Request must include modified payload.");
        assertTrue(reqVO.contains("private String changeReason;"), "Request must include change reason.");
        assertTrue(reqVO.contains("private Long revisionSignatureId;"), "Request must include new signature id.");
        assertTrue(reqVO.contains("private Long revisionSignatureUserId;"), "Request must include signature user id.");
        assertTrue(reqVO.contains("private String revisionSignatureSnapshot;"), "Request must include signature snapshot.");
        assertTrue(reqVO.contains("private List<FieldChangeReqVO> changedFields;"), "Request must include field-level diff.");
        assertTrue(reqVO.contains("private Boolean affectsQuantityFragment;"), "Diff must flag FIFO-affecting fields.");
        assertTrue(reqVO.contains("private Long sourceQuantityFragmentId;"), "Diff must carry source fragment id.");
        assertTrue(reqVO.contains("private MesProcessPoolFragmentOriginalField originalField;"),
                "Diff must carry original field enum for FIFO lock validation.");
        assertTrue(reqVO.contains("public MesProcessPoolEventRevisionUpdateReqBO toBO()"),
                "Request VO must map to the service BO explicitly.");
        assertTrue(reqVO.contains("@NotBlank(message = \"修改原因不能为空\")"),
                "Missing or blank change reason must fail fast at request validation.");
        assertTrue(reqVO.contains("@NotEmpty(message = \"字段级修改明细不能为空\")"),
                "Missing field diff must fail fast at request validation.");
    }
}
