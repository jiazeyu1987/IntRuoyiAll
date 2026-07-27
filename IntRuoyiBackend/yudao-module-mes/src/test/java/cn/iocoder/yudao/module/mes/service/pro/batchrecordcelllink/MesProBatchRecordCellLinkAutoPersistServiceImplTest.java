package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillItemVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditChange;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordCellLinkAutoPersistServiceImplTest {

    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordCellLinkService cellLinkService;
    @Mock
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    @InjectMocks
    private MesProBatchRecordCellLinkAutoPersistServiceImpl service;

    @Test
    void autoPersist_appliesProductionWorkOrderBatchCodeThroughFieldAudit() {
        MesProBatchRecordExecutionDO execution = draftExecution("[]");
        BatchRecordCellLinkPrefillItemVO prefill = productionBatchCodePrefill("34126020001");
        when(executionMapper.selectById(9001L)).thenReturn(execution);
        when(cellLinkService.getPrefill(9001L, 8101L)).thenReturn(new BatchRecordCellLinkPrefillRespVO()
                .setTargetExecutionId(9001L)
                .setPrefills(List.of(prefill))
                .setConflicts(List.of()));
        when(fieldAuditService.saveSystemCellLinkChanges(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setFieldAuditRevision(1L)
                        .setFieldAuditHeadHash("head-after")
                        .setCellValuesHash("cell-after")
                        .setChangedFieldCount(1));

        BatchRecordCellLinkAutoPersistResult result = service.autoPersist(new BatchRecordCellLinkAutoPersistCommand()
                .setExecutionId(9001L)
                .setWorkTaskId(8101L)
                .setTrigger("TASK_OPEN"));

        assertEquals(1, result.getAppliedCount());
        assertEquals(0, result.getConflictCount());
        assertEquals("APPLIED", result.getItems().get(0).getStatus());
        assertEquals(1L, result.getFieldAuditRevisionAfter());
        assertEquals("head-after", result.getFieldAuditHeadHashAfter());

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> commandCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(commandCaptor.capture());
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command = commandCaptor.getValue();
        assertEquals(9001L, command.getExecutionId());
        assertEquals(8101L, command.getWorkTaskId());
        assertTrue(command.getIdempotencyKey().contains("CELL_LINK_AUTO_PREFILL:9001:11:7:3:3"));
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"), command.getBaseCellValuesHash());
        assertEquals("OTHER", command.getReasonCategory());
        assertTrue(command.getReasonText().contains("生产批号"));
        MesProBatchRecordExecutionFieldAuditChange change = command.getChanges().get(0);
        assertEquals("sheet[0].rows[3].cells[3].batchCode", change.getFieldPath());
        assertEquals("batchCode", change.getFieldKey());
        assertEquals(3, change.getRowIndex());
        assertEquals(3, change.getColumnIndex());
        assertEquals(MesProBatchRecordExecutionFieldAuditValueType.STRING, change.getValueType());
        assertEquals("34126020001", change.getNewValueJson());
        assertEquals("34126020001", change.getNewValueDisplay());
    }

    @Test
    void autoPersist_missingProductionBatchCodeFailsFastWithoutWritingBlankValue() {
        MesProBatchRecordExecutionDO execution = draftExecution("[]");
        BatchRecordCellLinkPrefillItemVO missing = productionBatchCodePrefill(null)
                .setStatus("SOURCE_VALUE_MISSING");
        when(executionMapper.selectById(9001L)).thenReturn(execution);
        when(cellLinkService.getPrefill(9001L, 8101L)).thenReturn(new BatchRecordCellLinkPrefillRespVO()
                .setTargetExecutionId(9001L)
                .setPrefills(List.of())
                .setConflicts(List.of(missing)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.autoPersist(
                new BatchRecordCellLinkAutoPersistCommand()
                        .setExecutionId(9001L)
                        .setWorkTaskId(8101L)
                        .setTrigger("TASK_OPEN")));

        assertEquals(MesProBatchRecordCellLinkErrorCodeConstants
                .PRO_BATCH_RECORD_CELL_LINK_AUTO_PERSIST_SOURCE_VALUE_MISSING.getCode(), ex.getCode());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void autoPersist_existingManualValueIsReportedAndNotOverwritten() {
        String existingJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 3,
                "columnIndex", 3,
                "value", "MANUAL-BATCH"
        )));
        MesProBatchRecordExecutionDO execution = draftExecution(existingJson);
        BatchRecordCellLinkPrefillItemVO manual = productionBatchCodePrefill("34126020001")
                .setStatus("TARGET_ALREADY_MANUAL");
        when(executionMapper.selectById(9001L)).thenReturn(execution);
        when(cellLinkService.getPrefill(9001L, 8101L)).thenReturn(new BatchRecordCellLinkPrefillRespVO()
                .setTargetExecutionId(9001L)
                .setPrefills(List.of())
                .setConflicts(List.of(manual)));

        BatchRecordCellLinkAutoPersistResult result = service.autoPersist(new BatchRecordCellLinkAutoPersistCommand()
                .setExecutionId(9001L)
                .setWorkTaskId(8101L)
                .setTrigger("TASK_OPEN"));

        assertEquals(0, result.getAppliedCount());
        assertEquals(1, result.getConflictCount());
        assertEquals("TARGET_ALREADY_MANUAL", result.getItems().get(0).getStatus());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(org.mockito.ArgumentMatchers.any());
    }

    private MesProBatchRecordExecutionDO draftExecution(String cellValuesJson) {
        return MesProBatchRecordExecutionDO.builder()
                .id(9001L)
                .status(0)
                .batchRecordReportId("target-report")
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(Map.of(
                                "fieldPath", "sheet[0].rows[3].cells[3].batchCode",
                                "fieldKey", "batchCode",
                                "label", "生产批号",
                                "rowIndex", 3,
                                "columnIndex", 3,
                                "component", "input",
                                "valueType", "STRING",
                                "defaultValue", ""
                        ))
                )))
                .build();
    }

    private BatchRecordCellLinkPrefillItemVO productionBatchCodePrefill(String value) {
        return new BatchRecordCellLinkPrefillItemVO()
                .setRuleId(11L)
                .setRuleVersion(7L)
                .setTargetCellKey("3:3")
                .setTargetRowIndex(3)
                .setTargetColumnIndex(3)
                .setSourceType("PRODUCTION_WORK_ORDER")
                .setSourceReportId("PRODUCTION_WORK_ORDER")
                .setSourceFieldCode("batchCode")
                .setSourceFieldName("生产批号")
                .setSourceLabel("生产批号")
                .setOverwritePolicy("ONLY_WHEN_EMPTY")
                .setValue(value)
                .setStatus("APPLICABLE");
    }
}
