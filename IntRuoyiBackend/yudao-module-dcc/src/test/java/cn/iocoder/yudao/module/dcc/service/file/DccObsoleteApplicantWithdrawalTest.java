package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileWithdrawReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccObsoleteApplicantWithdrawalTest {
    @Test void usesBusinessPermissionAndExactObsoleteObjectWithoutChangingTheControlledFile() {
        var files = mock(DccControlledFileMapper.class);
        var forms = mock(FormActionInstanceMapper.class);
        var permissions = mock(DccControlledFileCategoryPermissionSupport.class);
        var bpm = mock(BpmProcessInstanceService.class);
        var service = new DccControlledFileObsoleteServiceImpl();
        ReflectionTestUtils.setField(service, "controlledFileMapper", files);
        ReflectionTestUtils.setField(service, "obsoleteForms", forms);
        ReflectionTestUtils.setField(service, "permissionSupport", permissions);
        ReflectionTestUtils.setField(service, "obsoleteProcessService", bpm);
        var file = DccControlledFileDO.builder().id(10L).tenantId(1L).versionNo("A/1").status("ACTIVE").build();
        var form = FormActionInstanceDO.builder().id(30L).applicantUserId(7L)
                .systemCode("DCC").objectType("CONTROLLED_FILE").objectId("10")
                .objectVersion("A/1").actionCode("OBSOLETE").build();
        when(files.selectById(10L)).thenReturn(file);
        when(forms.selectOwnershipByTenantAndProcessId(1L, "round-1")).thenReturn(List.of(form));
        when(permissions.hasFilePermission(file, 7L, DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(true);
        var request = new DccControlledFileWithdrawReqVO().setProcessInstanceId("round-1").setReason("重新核对");
        TenantContextHolder.setTenantId(1L);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(new LoginUser().setId(7L).setTenantId(1L).setUserType(2));
            service.withdrawObsoleteApplication(7L, 10L, request);
            verify(bpm).cancelProcessInstanceByStartUser(eq(7L), argThat(r -> "round-1".equals(r.getId()) && "重新核对".equals(r.getReason())));
            assertEquals("ACTIVE", file.getStatus());
            clearInvocations(bpm);
            form.setObjectId("11");
            assertThrows(RuntimeException.class, () -> service.withdrawObsoleteApplication(7L, 10L, request));
            form.setObjectId("10"); form.setActionCode("PUBLISH");
            assertThrows(RuntimeException.class, () -> service.withdrawObsoleteApplication(7L, 10L, request));
            form.setActionCode("OBSOLETE"); form.setApplicantUserId(8L);
            assertThrows(RuntimeException.class, () -> service.withdrawObsoleteApplication(7L, 10L, request));
            form.setApplicantUserId(7L);
            when(permissions.hasFilePermission(file, 7L, DccFileCategoryPermissionActionEnum.OBSOLETE)).thenReturn(false);
            assertThrows(RuntimeException.class, () -> service.withdrawObsoleteApplication(7L, 10L, request));
            verifyNoInteractions(bpm);
            verify(files, never()).updateById(any(DccControlledFileDO.class));
        } finally { TenantContextHolder.clear(); }
    }
}
