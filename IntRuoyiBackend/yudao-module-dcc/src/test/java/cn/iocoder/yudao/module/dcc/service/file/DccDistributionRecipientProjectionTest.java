package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionRecipientReceiptRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionRecipientProjectionTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock DccControlledFileMessageJobMapper messageJobMapper;
    @Mock DccControlledFileSignatureMapper signatureMapper;
    @Mock PermissionApi permissionApi;
    @Mock AdminUserApi adminUserApi;
    @Mock DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projects;
    @InjectMocks DccDistributionTaskServiceImpl service;
    DccControlledFileDistributionRecipientDO recipient;
    final LocalDateTime signedAt=LocalDateTime.of(2026,10,11,9,0);
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        var file=DccControlledFileDO.builder().id(42L).categoryId(15L).dccProjectCodeId(31L).versionNo("B/1").status("ACTIVE")
                .controlledTime(signedAt).activatedTime(signedAt).distributedTime(signedAt).build();file.setTenantId(1L);
        lenient().when(controlledFileMapper.selectById(42L)).thenReturn(file);
        lenient().when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        lenient().when(permissionApi.hasAnyPermissions(99L,"dcc:controlled-file:distribute")).thenReturn(true);
        lenient().when(adminUserApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        lenient().when(projects.listReadableProjectIds(99L)).thenReturn(List.of(31L));
        lenient().when(permissionSupport.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        lenient().when(distributionMapper.selectListByControlledFileId(42L)).thenReturn(List.of(DccControlledFileDistributionDO.builder()
                .id(51L).controlledFileId(42L).departmentId(61L).distributionMedium("PUBLIC_FOLDER").status("PENDING").build()));
        recipient=DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(123L).messageJobId(81L).build();
        lenient().when(distributionRecipientMapper.selectListByDistributionId(51L)).thenReturn(List.of(recipient));
        lenient().when(messageJobMapper.selectById(81L)).thenReturn(DccControlledFileMessageJobDO.builder().id(81L)
                .businessType("DISTRIBUTION").businessId(51L).recipientUserId(123L).status("FAILED").errorMessage("平台明确失败").build());
        lenient().when(adminUserApi.getUser(123L)).thenReturn(new AdminUserRespDTO().setId(123L).setNickname("当前账号名称"));
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @SuppressWarnings("unchecked") List<DccDistributionRecipientReceiptRespVO> project() throws Exception {
        var method=assertDoesNotThrow(()->DccDistributionTaskServiceImpl.class.getMethod("getDistributionRecipientReceipts",Long.class,Long.class));
        try{return (List<DccDistributionRecipientReceiptRespVO>)method.invoke(service,99L,42L);}
        catch(java.lang.reflect.InvocationTargetException ex){if(ex.getCause() instanceof RuntimeException runtime)throw runtime;throw ex;}
    }
    @Test void failedSendAndUnsignedReceiptHaveNoInventedTime() throws Exception {
        var row=project().get(0);assertEquals("FAILED",row.getMessageStatus());assertEquals("平台明确失败",row.getErrorMessage());
        assertNull(row.getSentAt());assertNull(row.getReadAt());assertNull(row.getSignedAt());assertNull(row.getSignatureId());
        assertEquals("当前账号名称",row.getCurrentUserName());assertNull(row.getSignerNameSnapshot());
    }
    @Test void signedProjectionUsesExactPersistedEvidenceAndTime() throws Exception {
        recipient.setAcknowledgedAt(signedAt);
        when(signatureMapper.selectActionSignature(42L,"DISTRIBUTION:51:71",123L,"DISTRIBUTION_ACK"))
                .thenReturn(DccControlledFileSignatureDO.builder().id(101L).controlledFileId(42L).versionNo("B/1").actorId(123L)
                        .taskId("DISTRIBUTION:51:71").actionType("DISTRIBUTION_ACK").signedAt(signedAt)
                        .actorNicknameSnapshot("实际签名人").actorDeptNameSnapshot("签名时部门").build());
        var row=project().get(0);assertEquals(101L,row.getSignatureId());assertEquals(signedAt,row.getSignedAt());
        assertEquals("实际签名人",row.getSignerNameSnapshot());
    }
    @Test void acknowledgedWithoutFormalSignatureIsAnExplicitFailure() {
        recipient.setAcknowledgedAt(signedAt);assertThrows(Exception.class,this::project);
    }
    @Test void menuPermissionDoesNotReplaceDocumentControlRole() {
        when(permissionApi.hasAnyRoles(99L,"doc_control")).thenReturn(false);assertThrows(Exception.class,this::project);
        verifyNoInteractions(distributionMapper,distributionRecipientMapper);
    }
}
