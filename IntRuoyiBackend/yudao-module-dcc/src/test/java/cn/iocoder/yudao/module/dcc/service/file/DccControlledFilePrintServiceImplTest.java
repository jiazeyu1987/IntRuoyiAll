package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
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
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRINT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFilePrintServiceImplTest extends BaseMockitoUnitTest {

    private static final Long USER_ID = 99L;
    private static final Long CONTROLLED_FILE_ID = 800L;
    private static final Long MASTER_ID = 700L;
    private static final Long CATEGORY_ID = 10L;

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

    @InjectMocks
    private DccControlledFilePrintServiceImpl printService;

    @Test
    void createPrintRecord_allowsCurrentActiveControlledFileWithoutGeneratedArtifact() {
        when(controlledFileMapper.selectById(CONTROLLED_FILE_ID)).thenReturn(currentActiveFile());
        when(controlledFileMasterMapper.selectById(MASTER_ID)).thenReturn(currentMaster(CONTROLLED_FILE_ID));
        when(permissionSupport.hasCategoryPermission(CATEGORY_ID, USER_ID,
                DccFileCategoryPermissionActionEnum.PRINT)).thenReturn(true);
        when(adminUserApi.getUser(USER_ID)).thenReturn(user());
        when(printRecordMapper.insert(any(DccControlledFilePrintRecordDO.class))).thenAnswer(invocation -> {
            DccControlledFilePrintRecordDO record = invocation.getArgument(0);
            record.setId(1200L);
            return 1;
        });

        DccControlledFilePrintRecordRespVO respVO =
                printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq());

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

        assertServiceException(() -> printService.createPrintRecord(USER_ID, CONTROLLED_FILE_ID, printReq()),
                CONTROLLED_FILE_PRINT_NOT_ALLOWED);
    }

    private DccControlledFileDO currentActiveFile() {
        return DccControlledFileDO.builder()
                .id(CONTROLLED_FILE_ID)
                .masterId(MASTER_ID)
                .categoryId(CATEGORY_ID)
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
