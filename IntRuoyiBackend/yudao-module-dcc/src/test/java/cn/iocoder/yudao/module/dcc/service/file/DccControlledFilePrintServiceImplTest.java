package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintRecordRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePrintRecordDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePrintRecordMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRINT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DccControlledFilePrintServiceImplTest extends BaseMockitoUnitTest {

    private static final Long USER_ID = 99L;
    private static final DccRequestAuditContext AUDIT = new DccRequestAuditContext("192.0.2.14", "word14-browser", "word14-print-request");
    private static final Long CONTROLLED_FILE_ID = 800L;
    private static final Long MASTER_ID = 700L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long SOURCE_FILE_ID = 601L;
    private static final BusinessFileAccessReference DCC_REFERENCE = new BusinessFileAccessReference(
            "dcc", "DCC_CONTROLLED_FILE", CONTROLLED_FILE_ID, "A.1", 31L, null);

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock
    private DccControlledFilePrintRecordMapper printRecordMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private BusinessFileAccessService businessFileAccessService;

    @InjectMocks
    private DccControlledFilePrintServiceImpl printService;

    @BeforeEach
    void allowBusinessFileGate() {
        TenantContextHolder.setTenantId(31L);
        org.mockito.Mockito.lenient().when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenReturn(java.util.Optional.of(DCC_REFERENCE));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void createPrintRecord_allowsCurrentActiveControlledFileWithoutGeneratedArtifact() {
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(currentActiveFile());
        when(controlledFileMasterMapper.selectById(MASTER_ID)).thenReturn(currentMaster(CONTROLLED_FILE_ID));
        when(permissionSupport.hasFilePermission(currentActiveFile(), USER_ID,
                DccFileCategoryPermissionActionEnum.PRINT)).thenReturn(true);
        when(adminUserApi.getUser(USER_ID)).thenReturn(user());
        when(printRecordMapper.insert(any(DccControlledFilePrintRecordDO.class))).thenAnswer(invocation -> {
            DccControlledFilePrintRecordDO record = invocation.getArgument(0);
            record.setId(1200L);
            return 1;
        });

        DccControlledFilePrintRecordRespVO respVO =
                printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq(), AUDIT);

        assertEquals(1200L, respVO.getId());
        assertEquals(CONTROLLED_FILE_ID, respVO.getControlledFileId());
        assertEquals("DCC-PRINT-001", respVO.getFileNumber());
        assertEquals("A.1", respVO.getVersionNo());
        assertEquals("DIRECT_PRINTED", respVO.getApprovalStatus());
        assertNotNull(respVO.getPrintNo());

        ArgumentCaptor<DccControlledFilePrintRecordDO> recordCaptor =
                ArgumentCaptor.forClass(DccControlledFilePrintRecordDO.class);
        verify(printRecordMapper).insert(recordCaptor.capture());
        DccControlledFilePrintRecordDO record = recordCaptor.getValue();
        assertEquals("生产现场受控复印", record.getPurpose());
        assertEquals(2, record.getCopies());
        assertEquals("生产部", record.getReceivingDepartment());
        assertEquals("灌装一线", record.getUseLocation());
        assertEquals("王新 (wangxin)", record.getPrintUserName());
    }

    @Test
    void createPrintRecord_rejectsHistoricalActiveFileEvenWithPrintPermission() {
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(currentActiveFile());
        when(controlledFileMasterMapper.selectById(MASTER_ID)).thenReturn(currentMaster(801L));

        assertServiceException(() -> printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq(), AUDIT),
                CONTROLLED_FILE_PRINT_NOT_ALLOWED);
    }

    @Test
    void createPrintRecord_businessGateDenialStopsBeforePrintRecordAndUserLookup() {
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(currentActiveFile());
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("denied",
                        BusinessFileAccessOperation.PRINT, SOURCE_FILE_ID, "dcc"));

        assertServiceException(() -> printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq(), AUDIT),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(printRecordMapper, never()).insert(any(DccControlledFilePrintRecordDO.class));
        verify(adminUserApi, never()).getUser(any());
    }

    @Test
    void getPrintHtml_businessGateDenialStopsBeforeRecordLookup() {
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(currentActiveFile());
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class)))
                .thenThrow(new BusinessFileAccessDeniedException("denied",
                        BusinessFileAccessOperation.PRINT, SOURCE_FILE_ID, "dcc"));

        assertServiceException(() -> printService.getPrintHtml(USER_ID, CONTROLLED_FILE_ID, 1200L, AUDIT),
                CONTROLLED_FILE_ACCESS_DENIED);

        verify(printRecordMapper, never()).selectByIdAndControlledFileId(any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"records", "create", "html"})
    void printBusinessGateReceivesActualRequestAuditForEveryEntry(String entry) {
        var file = currentActiveFile();
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(MASTER_ID)).thenReturn(currentMaster(CONTROLLED_FILE_ID));
        when(permissionSupport.hasFilePermission(file, USER_ID, DccFileCategoryPermissionActionEnum.PRINT)).thenReturn(true);
        when(businessFileAccessService.assertAllowed(any(BusinessFileAccessRequest.class))).thenAnswer(call -> {
            BusinessFileAccessRequest request = call.getArgument(0);
            assertEquals(BusinessFileAccessOperation.PRINT, request.operation());
            assertEquals(SOURCE_FILE_ID, request.fileId());
            assertEquals(31L, request.tenantId());
            assertEquals(USER_ID, request.userId());
            assertEquals(AUDIT.sourceIp(), request.sourceIp());
            assertEquals(AUDIT.userAgent(), request.userAgent());
            assertEquals(AUDIT.requestId(), request.requestId());
            // The actual provider constructs this strict context before evaluating file permission.
            new DccRequestAuditContext(request.sourceIp(), request.userAgent(), request.requestId());
            return java.util.Optional.of(DCC_REFERENCE);
        });
        if ("records".equals(entry)) {
            when(printRecordMapper.selectListByControlledFileId(CONTROLLED_FILE_ID)).thenReturn(java.util.List.of());
            assertEquals(java.util.List.of(), printService.getPrintRecords(USER_ID, CONTROLLED_FILE_ID, AUDIT));
        } else if ("create".equals(entry)) {
            when(adminUserApi.getUser(USER_ID)).thenReturn(user());
            printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq(), AUDIT);
        } else {
            when(printRecordMapper.selectByIdAndControlledFileId(1200L, CONTROLLED_FILE_ID)).thenReturn(
                    DccControlledFilePrintRecordDO.builder().id(1200L).controlledFileId(CONTROLLED_FILE_ID).build());
            assertEquals(1200L, printService.getPrintHtml(USER_ID, CONTROLLED_FILE_ID, 1200L, AUDIT).getPrintRecordId());
        }
        verify(permissionSupport).hasFilePermission(file, USER_ID, DccFileCategoryPermissionActionEnum.PRINT);
    }

    @Test
    void getPrintRecordsRejectsMissingExactFilePrintGrantBeforeListing() {
        var file = currentActiveFile();
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(MASTER_ID)).thenReturn(currentMaster(CONTROLLED_FILE_ID));
        when(permissionSupport.hasFilePermission(file, USER_ID, DccFileCategoryPermissionActionEnum.PRINT)).thenReturn(false);
        assertServiceException(() -> printService.getPrintRecords(USER_ID, CONTROLLED_FILE_ID, AUDIT), CONTROLLED_FILE_PRINT_NOT_ALLOWED);
        verify(printRecordMapper, never()).selectListByControlledFileId(any());
    }

    private DccControlledFileDO currentActiveFile() {
        return DccControlledFileDO.builder()
                .id(CONTROLLED_FILE_ID)
                .masterId(MASTER_ID)
                .categoryId(CATEGORY_ID)
                .tenantId(31L)
                .dccProjectCodeId(300L)
                .sourceFileId(SOURCE_FILE_ID)
                .fileNumber("DCC-PRINT-001")
                .versionNo("A.1")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build();
    }

    private DccControlledFileMasterDO currentMaster(Long currentControlledFileId) {
        return DccControlledFileMasterDO.builder()
                .id(MASTER_ID)
                .categoryId(CATEGORY_ID)
                .currentActiveControlledFileId(currentControlledFileId)
                .build();
    }

    private DccControlledFilePrintCreateReqVO printReq() {
        DccControlledFilePrintCreateReqVO reqVO = new DccControlledFilePrintCreateReqVO();
        reqVO.setPurpose("生产现场受控复印");
        reqVO.setCopies(2);
        reqVO.setReceivingDepartment("生产部");
        reqVO.setUseLocation("灌装一线");
        return reqVO;
    }

    private AdminUserRespDTO user() {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(USER_ID);
        user.setUsername("wangxin");
        user.setNickname("王新");
        return user;
    }
}
