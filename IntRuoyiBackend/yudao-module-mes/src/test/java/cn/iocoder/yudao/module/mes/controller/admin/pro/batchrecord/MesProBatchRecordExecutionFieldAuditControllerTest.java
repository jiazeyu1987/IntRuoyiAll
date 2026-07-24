package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditHashVerificationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditSaveChangesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHashVerification;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityEvidenceStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityValueOrigin;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordExecutionFieldAuditControllerTest {

    @Mock
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    @InjectMocks
    private MesProBatchRecordExecutionFieldAuditController controller;

    @Test
    void saveChanges_delegatesUnifiedRequestAndReturnsValidHashVerification() {
        MesProBatchRecordExecutionFieldAuditSaveResult serviceResult = new MesProBatchRecordExecutionFieldAuditSaveResult()
                .setFieldAuditRevision(7L)
                .setFieldAuditHeadHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .setCellValuesHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .setAuditBatchId(1001L)
                .setSignatureId(2002L)
                .setChangedAt(LocalDateTime.of(2026, 5, 26, 12, 0))
                .setHashVerification(MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        1L, 2L));
        when(fieldAuditService.saveChanges(any())).thenReturn(serviceResult);

        MesProBatchRecordExecutionFieldAuditSaveChangesReqVO reqVO =
                new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO()
                        .setExecutionId(10L)
                        .setIdempotencyKey("idem-20260526")
                        .setBaseCellValuesHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .setBaseFieldAuditRevision(6L)
                        .setBaseFieldAuditHeadHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                        .setReasonCategory("CORRECTION")
                        .setReasonText("operator correction")
                        .setSignature(new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Signature()
                                .setPassword("secret"))
                        .setChanges(List.of(new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change()
                                .setFieldPath("sheet[0].rows[1].cells[2].temperature")
                                .setFieldKey("temperature")
                                .setRowIndex(1)
                                .setColumnIndex(2)
                                .setValueType("NUMBER")
                                .setNewValueJson(37.5)
                                .setNewValueDisplay("37.5")
                                .setExpectedOldValueJson(36.6)
                                .setExpectedOldValueHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")));

        CommonResult<MesProBatchRecordExecutionFieldAuditSaveRespVO> response = controller.saveChanges(reqVO);

        assertEquals(7L, response.getData().getFieldAuditRevision());
        assertEquals(1001L, response.getData().getAuditBatchId());
        assertEquals(2002L, response.getData().getSignatureId());
        assertEquals("VALID", response.getData().getHashVerification().getStatus());
        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveChanges(captor.capture());
        assertEquals("sheet[0].rows[1].cells[2].temperature",
                captor.getValue().getChanges().get(0).getFieldPath());
        assertEquals("secret", captor.getValue().getSignature().getPassword());
    }

    @Test
    void saveChanges_delegatesAttachmentChangesWithoutOrdinaryFieldChanges() {
        when(fieldAuditService.saveChanges(any())).thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                .setFieldAuditRevision(6L)
                .setFieldAuditHeadHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .setCellValuesHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setAuditBatchId(1002L)
                .setSignatureId(2003L)
                .setChangedAt(LocalDateTime.of(2026, 5, 26, 12, 30))
                .setHashVerification(MesProBatchRecordExecutionFieldAuditHashVerification.valid(
                        "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                        "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                        1L, 0L)));
        MesProBatchRecordExecutionFieldAuditSaveChangesReqVO reqVO = saveRequest("STRING", "same", "same")
                .setChanges(List.of())
                .setAttachmentChanges(List.of(new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.AttachmentChange()
                        .setWorkTaskId(31L)
                        .setFieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                        .setFieldKey("visualEvidence")
                        .setFieldLabel("现场图片")
                        .setRowIndex(1)
                        .setColumnIndex(2)
                        .setAttachmentType("IMAGE")
                        .setAttachmentAction("ADD")
                        .setAttachmentGroupKey("R1C2-IMG-1")
                        .setFileId(901L)
                        .setStorageConfigId(28L)
                        .setStoragePath("edhr/501/evidence.png")
                        .setFileUrl("http://127.0.0.1:9000/yudao/edhr/501/evidence.png")
                        .setFileName("evidence.png")
                        .setContentType("image/png")
                        .setFileSize(2048L)
                        .setSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                        .setStorageRetentionJson("{\"fileId\":901,\"retention\":\"batch-record\"}")));

        CommonResult<MesProBatchRecordExecutionFieldAuditSaveRespVO> response = controller.saveChanges(reqVO);

        assertEquals(1002L, response.getData().getAuditBatchId());
        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveChanges(captor.capture());
        assertEquals(0, captor.getValue().getChanges().size());
        assertEquals(1, captor.getValue().getAttachmentChanges().size());
        assertEquals("visualEvidence", captor.getValue().getAttachmentChanges().get(0).getFieldKey());
        assertEquals("ADD", captor.getValue().getAttachmentChanges().get(0).getAttachmentAction());
    }

    @Test
    void saveChangesValidation_allowsNullNewValueOnlyForNullValueType() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertEquals(0, validator.validate(saveRequest("NULL", null, "null")).size());
        assertEquals(1, validator.validate(saveRequest("STRING", null, "null")).size());
        assertEquals(1, validator.validate(saveRequest("NUMBER", null, "null")).size());
    }

    @Test
    void saveChangesValidation_allowsEmptyDisplayButRequiresPresence() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertEquals(0, validator.validate(saveRequest("STRING", "", "")).size());
        assertEquals(1, validator.validate(saveRequest("STRING", "", null)).size());
    }

    @Test
    void signatureTimeRequest_usesExplicitIsoLocalDateTimeJsonFormat() throws Exception {
        JsonFormat format = MesProBatchRecordExecutionSignatureTimeReqVO.class
                .getDeclaredField("selectedSignedAt")
                .getAnnotation(JsonFormat.class);

        assertNotNull(format);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", format.pattern());
    }

    @Test
    void signatureTimeRequest_deserializesIsoStringBeforeGlobalEpochDeserializer() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper.registerModule(module);

        MesProBatchRecordExecutionFieldAuditSaveChangesReqVO reqVO = objectMapper.readValue("""
                {
                  "executionId": 10,
                  "idempotencyKey": "idem-selected-time",
                  "baseCellValuesHash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "baseFieldAuditRevision": 0,
                  "baseFieldAuditHeadHash": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                  "reasonCategory": "CORRECTION",
                  "reasonText": "operator correction",
                  "signature": {
                    "password": "secret",
                    "signatureTime": {
                      "selectedSignedAt": "2026-06-15T09:30:00",
                      "selectedTimeZone": "Asia/Shanghai",
                      "selectedTimeReason": "manual signature time evidence"
                    }
                  },
                  "changes": [{
                    "fieldPath": "sheet[0].rows[1].cells[2].quantity",
                    "fieldKey": "quantity",
                    "rowIndex": 1,
                    "columnIndex": 2,
                    "valueType": "NUMBER",
                    "newValueJson": 999993,
                    "newValueDisplay": "999993"
                  }]
                }
                """, MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class);

        assertEquals(LocalDateTime.of(2026, 6, 15, 9, 30),
                reqVO.getSignature().getSignatureTime().getSelectedSignedAt());
    }

    @Test
    void queryDetailVerifyAndExport_delegateToServiceContracts() {
        PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO> page = new PageResult<>(
                List.of(new MesProBatchRecordExecutionFieldAuditItemRespVO()
                        .setId(1L)
                        .setExecutionId(10L)
                        .setAuditBatchId(1001L)
                        .setFieldPath("sheet[0].rows[1].cells[2].temperature")
                        .setOldValueDisplay("36.6")
                        .setNewValueDisplay("37.5")
                        .setSignatureId(2002L)
                        .setAuditHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                        .setPreviousHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .setHashVerification(new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO()
                                .setStatus("VALID"))), 1L);
        when(fieldAuditService.getPage(any())).thenReturn(page);
        when(fieldAuditService.getDetail(any())).thenReturn(new MesProBatchRecordExecutionFieldAuditDetailRespVO()
                .setExecutionId(10L)
                .setHashVerification(new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO().setStatus("VALID")));
        when(fieldAuditService.verifyChain(any(MesProBatchRecordExecutionFieldAuditVerifyReqVO.class)))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditVerifyRespVO()
                .setExecutionId(10L)
                .setHashVerification(new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO().setStatus("VALID")));
        MesProBatchRecordExecutionFieldAuditExportRespVO export = new MesProBatchRecordExecutionFieldAuditExportRespVO()
                .setFileName("field-audit-10.json")
                .setContentType("application/json")
                .setRecordCount(1L)
                .setHashVerification(new MesProBatchRecordExecutionFieldAuditHashVerificationRespVO().setStatus("VALID"));
        when(fieldAuditService.export(any())).thenReturn(export);

        assertEquals(1L, controller.getPage(new MesProBatchRecordExecutionFieldAuditPageReqVO()).getData().getTotal());
        assertEquals("VALID", controller.getDetail(new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                .setExecutionId(10L).setAuditBatchId(1001L)).getData().getHashVerification().getStatus());
        assertEquals("VALID", controller.verifyChain(new MesProBatchRecordExecutionFieldAuditVerifyReqVO()
                .setExecutionId(10L)).getData().getHashVerification().getStatus());
        MesProBatchRecordExecutionFieldAuditExportReqVO exportReqVO =
                new MesProBatchRecordExecutionFieldAuditExportReqVO().setFormat("JSON");
        exportReqVO.setExecutionId(10L);
        assertSame(export, controller.export(exportReqVO).getData());
    }

    @Test
    void responsibilitySummaryRejectsPageSizeOverTwoHundred() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO =
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO().setExecutionId(10L);

        assertEquals(1, reqVO.getPageNo());
        assertEquals(50, reqVO.getPageSize());
        assertEquals(0, validator.validate(reqVO).size());
        assertEquals(0, validator.validate(reqVO.setPageSize(200)).size());
        assertEquals(1, validator.validate(reqVO.setPageSize(201)).size());
        assertEquals(1, validator.validate(reqVO.setPageSize(50).setPageNo(0)).size());
        assertEquals(1, validator.validate(reqVO.setPageNo(1).setPageSize(0)).size());

        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getResponsibilitySummary",
                new Class[]{MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class},
                GetMapping.class, "/responsibility-summary",
                "mes:pro-batch-record-execution:field-audit-query");
        Method method = MesProBatchRecordExecutionFieldAuditController.class.getDeclaredMethod(
                "getResponsibilitySummary", MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class);
        assertNotNull(method.getParameters()[0].getAnnotation(Valid.class));

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO expected =
                new MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO()
                        .setExecutionId(10L)
                        .setOverallEvidenceStatus(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED)
                        .setTotal(1L)
                        .setList(List.of());
        reqVO.setPageSize(50)
                .setFieldKeyword("Temperature")
                .setEvidenceStatus(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE)
                .setValueOrigin(MesProBatchRecordExecutionResponsibilityValueOrigin.HUMAN)
                .setActorId(101L);
        when(fieldAuditService.getResponsibilitySummary(any())).thenReturn(expected);

        @SuppressWarnings("unchecked")
        CommonResult<MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO> response =
                (CommonResult<MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO>)
                        method.invoke(controller, reqVO);

        assertSame(expected, response.getData());
        ArgumentCaptor<MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class);
        verify(fieldAuditService).getResponsibilitySummary(captor.capture());
        assertSame(reqVO, captor.getValue());
        assertEquals("Temperature", captor.getValue().getFieldKeyword());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                captor.getValue().getEvidenceStatus());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.HUMAN,
                captor.getValue().getValueOrigin());
        assertEquals(101L, captor.getValue().getActorId());
    }

    @Test
    void responsibilityHistoryValidatesCompositeCursorAndDelegates() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO reqVO =
                new MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO()
                        .setExecutionId(10L)
                        .setFieldPath("sheet[0].rows[1].cells[1]")
                        .setFieldKey("temperature")
                        .setRowIndex(1)
                        .setColumnIndex(1);

        assertEquals(50, reqVO.getPageSize());
        assertEquals(0, validator.validate(reqVO).size());
        assertEquals(0, validator.validate(reqVO.setPageSize(200)).size());
        assertEquals(1, validator.validate(reqVO.setPageSize(201)).size());
        reqVO.setPageSize(50).setCursorFieldAuditRevision(9L).setCursorAuditItemId(null);
        assertEquals(1, validator.validate(reqVO).size());
        reqVO.setCursorFieldAuditRevision(null).setCursorAuditItemId(902L);
        assertEquals(1, validator.validate(reqVO).size());
        reqVO.setCursorFieldAuditRevision(9L).setCursorAuditItemId(902L);
        assertEquals(0, validator.validate(reqVO).size());

        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getResponsibilityHistory",
                new Class[]{MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class},
                GetMapping.class, "/responsibility-history",
                "mes:pro-batch-record-execution:field-audit-query");
        Method method = MesProBatchRecordExecutionFieldAuditController.class.getDeclaredMethod(
                "getResponsibilityHistory", MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class);
        assertNotNull(method.getParameters()[0].getAnnotation(Valid.class));

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO expected =
                new MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO()
                        .setExecutionId(10L)
                        .setFieldPath(reqVO.getFieldPath())
                        .setFieldKey(reqVO.getFieldKey())
                        .setRowIndex(1)
                        .setColumnIndex(1)
                        .setList(List.of())
                        .setHasMore(false);
        when(fieldAuditService.getResponsibilityHistory(any())).thenReturn(expected);

        @SuppressWarnings("unchecked")
        CommonResult<MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO> response =
                (CommonResult<MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO>)
                        method.invoke(controller, reqVO);

        assertSame(expected, response.getData());
        ArgumentCaptor<MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class);
        verify(fieldAuditService).getResponsibilityHistory(captor.capture());
        assertSame(reqVO, captor.getValue());
        assertEquals(9L, captor.getValue().getCursorFieldAuditRevision());
        assertEquals(902L, captor.getValue().getCursorAuditItemId());
    }

    @Test
    void responsibilityEndpointsKeepStaticQueryPermissionAndTenantInterceptor() throws Exception {
        assertNull(MesProBatchRecordExecutionFieldAuditController.class.getAnnotation(TenantIgnore.class));

        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getResponsibilitySummary",
                new Class[]{MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class},
                GetMapping.class, "/responsibility-summary",
                "mes:pro-batch-record-execution:field-audit-query");
        Method summary = MesProBatchRecordExecutionFieldAuditController.class.getDeclaredMethod(
                "getResponsibilitySummary", MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class);
        assertNull(summary.getAnnotation(TenantIgnore.class));

        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getResponsibilityHistory",
                new Class[]{MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class},
                GetMapping.class, "/responsibility-history",
                "mes:pro-batch-record-execution:field-audit-query");
        Method history = MesProBatchRecordExecutionFieldAuditController.class.getDeclaredMethod(
                "getResponsibilityHistory", MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class);
        assertNull(history.getAnnotation(TenantIgnore.class));
    }

    @Test
    void responsibilityExportUsesFixedEndpointPermissionValidationAndDelegation() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MesProBatchRecordExecutionFieldResponsibilityExportReqVO reqVO =
                new MesProBatchRecordExecutionFieldResponsibilityExportReqVO().setExecutionId(10L);
        assertEquals("XLSX", reqVO.getFormat());
        assertEquals(0, validator.validate(reqVO).size());
        assertEquals(1, validator.validate(reqVO.setFormat("PDF")).size());
        reqVO.setFormat("XLSX");

        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "exportResponsibility",
                new Class[]{MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class},
                GetMapping.class, "/responsibility-export",
                "mes:pro-batch-record-execution:field-audit-export");
        Method method = MesProBatchRecordExecutionFieldAuditController.class.getDeclaredMethod(
                "exportResponsibility", MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class);
        assertNotNull(method.getParameters()[0].getAnnotation(Valid.class));
        assertNull(method.getAnnotation(TenantIgnore.class));

        MesProBatchRecordExecutionFieldResponsibilityExportRespVO expected =
                new MesProBatchRecordExecutionFieldResponsibilityExportRespVO()
                        .setFileName("field-responsibility-10.xlsx")
                        .setFormat("XLSX")
                        .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .setContentBase64("UEsDBA==")
                        .setSha256("sha256")
                        .setRecordCount(3L);
        when(fieldAuditService.exportResponsibility(any())).thenReturn(expected);

        @SuppressWarnings("unchecked")
        CommonResult<MesProBatchRecordExecutionFieldResponsibilityExportRespVO> response =
                (CommonResult<MesProBatchRecordExecutionFieldResponsibilityExportRespVO>)
                        method.invoke(controller, reqVO);

        assertSame(expected, response.getData());
        ArgumentCaptor<MesProBatchRecordExecutionFieldResponsibilityExportReqVO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class);
        verify(fieldAuditService).exportResponsibility(captor.capture());
        assertSame(reqVO, captor.getValue());
    }

    @Test
    void mappingsAndPermissions_matchFrozenFieldAuditContract() throws Exception {
        assertRawMethod(MesProBatchRecordExecutionFieldAuditController.class, "saveChanges",
                new Class[]{MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class},
                PutMapping.class, "/save-changes",
                "@ss.hasPermission('mes:pro-batch-record-execution:field-audit-update') "
                        + "or (#reqVO.workTaskId != null and @ss.hasPermission('mes:pro-batch-record-execution:update')) "
                        + "or @ss.hasPermission('mes:pro-batch-record-execution:golden-finger')");
        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getPage",
                new Class[]{MesProBatchRecordExecutionFieldAuditPageReqVO.class},
                GetMapping.class, "/page",
                "mes:pro-batch-record-execution:field-audit-query");
        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "getDetail",
                new Class[]{MesProBatchRecordExecutionFieldAuditDetailReqVO.class},
                GetMapping.class, "/detail",
                "mes:pro-batch-record-execution:field-audit-query");
        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "verifyChain",
                new Class[]{MesProBatchRecordExecutionFieldAuditVerifyReqVO.class},
                PostMapping.class, "/verify-chain",
                "mes:pro-batch-record-execution:field-audit-verify");
        assertMethod(MesProBatchRecordExecutionFieldAuditController.class, "export",
                new Class[]{MesProBatchRecordExecutionFieldAuditExportReqVO.class},
                GetMapping.class, "/export",
                "mes:pro-batch-record-execution:field-audit-export");

        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getExecutionId");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getIdempotencyKey");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getBaseCellValuesHash");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getBaseFieldAuditRevision");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getBaseFieldAuditHeadHash");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getChanges");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getAttachmentChanges");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.class, "getSignature");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change.class, "getFieldPath");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change.class, "getNewValueJson");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change.class, "getNewValueDisplay");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveRespVO.class, "getChangedAt");
        requireGetter(MesProBatchRecordExecutionFieldAuditSaveRespVO.class, "getHashVerification");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getFieldPath");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getOldValueJson");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getNewValueJson");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getSignatureId");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getAuditHash");
        requireGetter(MesProBatchRecordExecutionFieldAuditItemRespVO.class, "getPreviousHash");
        requireGetter(MesProBatchRecordExecutionFieldAuditExportRespVO.class, "getSha256");
        requireGetter(MesProBatchRecordExecutionFieldAuditHashVerificationRespVO.class, "getStatus");
    }

    private void assertMethod(Class<?> controllerClass, String methodName, Class<?>[] parameterTypes,
                              Class<?> mappingClass, String mappingPath, String permission) throws Exception {
        assertRawMethod(controllerClass, methodName, parameterTypes, mappingClass, mappingPath,
                "@ss.hasPermission('" + permission + "')");
    }

    private void assertRawMethod(Class<?> controllerClass, String methodName, Class<?>[] parameterTypes,
                                 Class<?> mappingClass, String mappingPath, String preAuthorizeExpression)
            throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        if (mappingClass == PutMapping.class) {
            assertArrayEquals(new String[]{mappingPath}, method.getAnnotation(PutMapping.class).value());
        } else if (mappingClass == GetMapping.class) {
            assertArrayEquals(new String[]{mappingPath}, method.getAnnotation(GetMapping.class).value());
        } else if (mappingClass == PostMapping.class) {
            assertArrayEquals(new String[]{mappingPath}, method.getAnnotation(PostMapping.class).value());
        } else {
            fail("Unsupported mapping annotation: " + mappingClass);
        }
        assertEquals(preAuthorizeExpression, method.getAnnotation(PreAuthorize.class).value());
    }

    private void requireGetter(Class<?> type, String getterName) {
        try {
            assertNotNull(type.getMethod(getterName));
        } catch (NoSuchMethodException ex) {
            fail("Expected getter to exist: " + type.getName() + "#" + getterName, ex);
        }
    }

    private MesProBatchRecordExecutionFieldAuditSaveChangesReqVO saveRequest(String valueType,
                                                                             Object newValueJson,
                                                                             String newValueDisplay) {
        return new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO()
                .setExecutionId(10L)
                .setWorkTaskId(31L)
                .setIdempotencyKey("idem-typed-null")
                .setBaseCellValuesHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .setBaseFieldAuditRevision(6L)
                .setBaseFieldAuditHeadHash("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
                .setReasonCategory("CORRECTION")
                .setReasonText("operator correction")
                .setSignature(new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Signature()
                        .setPassword("secret"))
                .setChanges(List.of(new MesProBatchRecordExecutionFieldAuditSaveChangesReqVO.Change()
                        .setFieldPath("sheet[0].rows[1].cells[2].temperature")
                        .setFieldKey("temperature")
                        .setRowIndex(1)
                        .setColumnIndex(2)
                        .setValueType(valueType)
                        .setNewValueJson(newValueJson)
                        .setNewValueDisplay(newValueDisplay)));
    }
}
