package cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentAuthorization;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProjectCodeMetadataChangeAuditServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMetadataChangeMapper changeMapper;
    @Mock
    private DccControlledFileMetadataChangeItemMapper changeItemMapper;
    @Mock
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DccProjectCodeAssignmentService assignmentService;

    @InjectMocks
    private DccProjectCodeMetadataChangeAuditServiceImpl auditService;

    @Test
    void recordMetadataChange_allowsUnchangedNullFieldsAndWritesChangedItems() {
        DccControlledFileDO beforeFile = baseFile().fileTypeLevel2(null).build();
        DccControlledFileDO afterFile = baseFile().fileTypeLevel2("DMR").build();
        doAnswer(invocation -> {
            DccControlledFileMetadataChangeDO change = invocation.getArgument(0);
            change.setId(7000L);
            return 1;
        }).when(changeMapper).insert(any(DccControlledFileMetadataChangeDO.class));
        when(changeItemMapper.insert(any(DccControlledFileMetadataChangeItemDO.class))).thenReturn(1);

        auditService.recordMetadataChange(new DccProjectCodeMetadataChangeCommand(
                123L,
                DccProjectCodeAssignmentAuthorization.assignedUser(9100L, 3000L),
                beforeFile,
                afterFile,
                "修正 AI 分类"));

        ArgumentCaptor<DccControlledFileMetadataChangeItemDO> itemCaptor =
                ArgumentCaptor.forClass(DccControlledFileMetadataChangeItemDO.class);
        verify(changeItemMapper).insert(itemCaptor.capture());
        assertEquals(7000L, itemCaptor.getValue().getChangeId());
        assertEquals("fileTypeLevel2", itemCaptor.getValue().getFieldName());
        assertEquals("文件类别 II", itemCaptor.getValue().getFieldLabel());
        assertEquals("DMR", itemCaptor.getValue().getNewValueText());
        verify(assignmentService).markAssignmentFileChanged(9100L, 900L, 1);
    }

    @Test
    void getAuditChangeItems_returnsAllFieldItemsForOneChangeGroup() {
        when(changeMapper.selectById(7000L)).thenReturn(DccControlledFileMetadataChangeDO.builder()
                .id(7000L)
                .assignmentId(9100L)
                .projectCodeId(3000L)
                .controlledFileId(900L)
                .operatorUserId(123L)
                .source("ASSIGNMENT_USER")
                .changeReason("修正 AI 分类")
                .changedTime(LocalDateTime.now())
                .build());
        when(changeItemMapper.selectListByChangeId(7000L)).thenReturn(List.of(
                changeItem(7001L, "fileTypeLevel2", "文件类别 II", "OLD-II", "NEW-II"),
                changeItem(7002L, "fileTypeLevel3", "文件类别 III", "OLD-III", "NEW-III")));
        when(assignmentMapper.selectBatchIds(List.of(9100L))).thenReturn(List.of(DccProjectCodeAssignmentDO.builder()
                .id(9100L)
                .assignmentNo("DCC-PC-A-UT")
                .build()));
        when(projectCodeMapper.selectBatchIds(List.of(3000L))).thenReturn(List.of(DccProjectCodeDO.builder()
                .id(3000L)
                .projectName("PTC")
                .projectCode("PTCABC")
                .build()));
        when(controlledFileMapper.selectBatchIds(List.of(900L))).thenReturn(List.of(baseFile().build()));
        when(adminUserApi.getUserList(List.of(123L))).thenReturn(List.of(
                new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                        .setId(123L)
                        .setNickname("授权用户")));

        var items = auditService.getAuditChangeItems(7000L);

        assertEquals(2, items.size());
        assertEquals("DCC-PC-A-UT", items.get(0).getAssignmentNo());
        assertEquals("fileTypeLevel2", items.get(0).getFieldName());
        assertEquals("fileTypeLevel3", items.get(1).getFieldName());
        assertEquals("授权用户", items.get(0).getOperatorNickname());
    }

    private DccControlledFileMetadataChangeItemDO changeItem(Long id, String fieldName, String fieldLabel,
                                                            String oldValue, String newValue) {
        return DccControlledFileMetadataChangeItemDO.builder()
                .id(id)
                .changeId(7000L)
                .assignmentId(9100L)
                .projectCodeId(3000L)
                .controlledFileId(900L)
                .operatorUserId(123L)
                .fieldName(fieldName)
                .fieldLabel(fieldLabel)
                .oldValueText(oldValue)
                .newValueText(newValue)
                .changedTime(LocalDateTime.now())
                .build();
    }

    private DccControlledFileDO.DccControlledFileDOBuilder baseFile() {
        return DccControlledFileDO.builder()
                .id(900L)
                .masterId(700L)
                .productMasterId(5000L)
                .productCode("PRD20260604001")
                .productName("离心泵")
                .dccProjectCodeId(3000L)
                .needTraining(Boolean.FALSE)
                .fileName("NEW-SOP")
                .fileNumber("DOC-NEW")
                .categoryId(11L)
                .directoryId(31L);
    }
}
