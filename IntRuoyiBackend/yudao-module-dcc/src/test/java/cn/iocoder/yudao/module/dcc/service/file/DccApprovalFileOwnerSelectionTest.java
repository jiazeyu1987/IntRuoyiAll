package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccApprovalFileOwnerSelectionTest {
    private Object selection;
    private AdminUserApi users;
    private DccControlledFileMapper files;
    private DccControlledFileSignatureMapper signatures;
    private DccControlledFileDO file;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        users=mock(AdminUserApi.class); files=mock(DccControlledFileMapper.class); signatures=mock(DccControlledFileSignatureMapper.class);
        file=DccControlledFileDO.builder().id(10L).tenantId(1L).processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .processInstanceId("upload-10").versionNo("A/1").status("PENDING_MATRIX_APPROVAL").build();
        when(users.getUser(9007199254740993L)).thenReturn(new AdminUserRespDTO().setId(9007199254740993L)
                .setTenantId(1L).setStatus(0).setUsername("file-owner").setNickname("文件负责人"));
        when(files.updateById(any(DccControlledFileDO.class))).thenReturn(1);
        try {
            selection=Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccApprovalFileOwnerSelectionService").getConstructor().newInstance();
            ReflectionTestUtils.setField(selection,"users",users); ReflectionTestUtils.setField(selection,"files",files);
            ReflectionTestUtils.setField(selection,"signatures",signatures);
        } catch (ReflectiveOperationException cause) { selection=null; }
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    Object prepare(Long ownerId, DccControlledFileStageCodeEnum stage) {
        assertNotNull(selection,"formal approval file-owner service is missing");
        return ReflectionTestUtils.invokeMethod(selection,"prepare",file,stage,ownerId);
    }
    @Test void ownerIsMandatoryOnlyAtActualNativeMatrixApprovalAndMustBeAnEnabledSameTenantAccount() {
        assertThrows(IllegalArgumentException.class,()->prepare(null,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        assertNotNull(prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        when(users.getUser(9007199254740993L)).thenReturn(new AdminUserRespDTO().setId(9007199254740993L).setTenantId(2L).setStatus(0).setUsername("foreign").setNickname("外租户"));
        assertThrows(IllegalArgumentException.class,()->prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        when(users.getUser(9007199254740993L)).thenReturn(new AdminUserRespDTO().setId(9007199254740993L).setTenantId(1L).setStatus(1).setUsername("disabled").setNickname("停用"));
        assertThrows(IllegalArgumentException.class,()->prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        verifyNoInteractions(files,signatures);
    }
    @Test void reviewDocControlAndObsoleteCannotBorrowOwnerSelectionOrWriteAnySnapshot() {
        assertNull(prepare(null,DccControlledFileStageCodeEnum.MATRIX_REVIEW));
        assertThrows(IllegalArgumentException.class,()->prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_REVIEW));
        assertThrows(IllegalArgumentException.class,()->prepare(9007199254740993L,DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW));
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        assertNull(prepare(null,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        assertThrows(IllegalArgumentException.class,()->prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_APPROVAL));
        verifyNoInteractions(files,signatures);
    }
    @Test void bindingRequiresTheActualSameFileBpmTaskVersionActorAndSignedOwnerFacts() {
        Object selected=prepare(9007199254740993L,DccControlledFileStageCodeEnum.MATRIX_APPROVAL);
        String reason=ReflectionTestUtils.invokeMethod(selection,"signedReason","同意",selected);
        assertTrue(reason.contains("9007199254740993")); assertTrue(reason.contains("file-owner"));
        var signed=DccControlledFileSignatureDO.builder().id(100L).controlledFileId(10L).taskId("approve-task")
                .actorId(99L).actionType("APPROVE").processInstanceId("upload-10").versionNo("A/1")
                .comment(reason).evidenceStatus("VALID").signedAt(LocalDateTime.of(2026,10,3,9,0)).build();
        signed.setMeaningCode("MATRIX_APPROVAL_APPROVE");signed.setEvidenceHash("isolated-signed-evidence");
        when(signatures.selectActionSignature(10L,"approve-task",99L,"APPROVE")).thenReturn(signed);
        ReflectionTestUtils.invokeMethod(selection,"bind",file,"approve-task",99L,100L,selected,reason);
        var capture=org.mockito.ArgumentCaptor.forClass(DccControlledFileDO.class);verify(files).updateById(capture.capture());
        assertEquals(9007199254740993L,ReflectionTestUtils.getField(capture.getValue(),"fileOwnerUserId"));
        assertEquals("文件负责人",ReflectionTestUtils.getField(capture.getValue(),"fileOwnerNicknameSnapshot"));
        assertEquals(100L,ReflectionTestUtils.getField(capture.getValue(),"fileOwnerSignatureId"));
        signed.setProcessInstanceId("another-round");
        assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(selection,"bind",file,"approve-task",99L,100L,selected,reason));
    }
}
