package cn.iocoder.yudao.module.dcc.controller.admin;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccFolderTemplateController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccFolderTemplateDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.DccRegistrationCertificateChangeConfirmationController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateChangeConfirmationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccDisplayProjectionTest {
    @Test
    void templateNamesAreBatchedAndOriginalRecordIdentitiesRemainUnchanged() {
        var service = mock(DccFolderTemplateService.class);
        var users = mock(AdminUserApi.class);
        var controller = new DccFolderTemplateController();
        ReflectionTestUtils.setField(controller, "service", service);
        ReflectionTestUtils.setField(controller, "userApi", users);
        var first = new DccFolderTemplateDO();
        first.setId(11L); first.setName("生产目录"); first.setEditedByUserId(7L);
        var second = new DccFolderTemplateDO();
        second.setId(12L); second.setEditedByUserId(7L);
        var missing = new DccFolderTemplateDO();
        missing.setId(13L); missing.setEditedByUserId(8L);
        when(service.list()).thenReturn(List.of(first, second, missing));
        when(users.getUserMap(Set.of(7L, 8L))).thenReturn(Map.of(7L,
                new AdminUserRespDTO().setId(7L).setNickname("真实修改人").setUsername("editor")));

        var result = controller.list().getData();

        verify(users).getUserMap(Set.of(7L, 8L));
        assertEquals("真实修改人", result.get(0).getEditedByName());
        assertEquals("真实修改人", result.get(1).getEditedByName());
        assertNull(result.get(2).getEditedByName());
        assertEquals(11L, result.get(0).getId());
        assertEquals(7L, result.get(0).getEditedByUserId());
        assertEquals("生产目录", result.get(0).getName());
    }

    @Test
    void confirmationProjectionBatchesUsersAndVersionsAndRejectsForeignVersionName() {
        var users = mock(AdminUserApi.class);
        var versions = mock(DccRegistrationCertificateVersionMapper.class);
        var controller = new DccRegistrationCertificateChangeConfirmationController(null, null);
        ReflectionTestUtils.setField(controller, "userApi", users);
        ReflectionTestUtils.setField(controller, "versionMapper", versions);
        var own = new DccRegistrationCertificateVersionDO();
        own.setId(20L); own.setCertificateId(10L); own.setVersionNo(3);
        var foreign = new DccRegistrationCertificateVersionDO();
        foreign.setId(21L); foreign.setCertificateId(99L); foreign.setVersionNo(8);
        when(users.getUserMap(Set.of(7L, 8L))).thenReturn(Map.of(7L,
                new AdminUserRespDTO().setId(7L).setNickname("真实接收人").setUsername("recipient")));
        when(versions.selectBatchIds(Set.of(20L, 21L))).thenReturn(List.of(own, foreign));
        var rows = List.of(
                new DccRegistrationCertificateChangeConfirmationService.Confirmation(1L, 10L, 20L,
                        "effective", 7L, 30L, null, null),
                new DccRegistrationCertificateChangeConfirmationService.Confirmation(2L, 10L, 21L,
                        "effective", 8L, 31L, null, null));

        List<DccRegistrationCertificateChangeConfirmationController.ConfirmationView> result =
                ReflectionTestUtils.invokeMethod(controller, "project", rows);

        verify(users).getUserMap(Set.of(7L, 8L));
        verify(versions).selectBatchIds(Set.of(20L, 21L));
        assertNotNull(result);
        assertEquals(3, result.get(0).versionNo());
        assertEquals("真实接收人", result.get(0).recipientName());
        assertEquals(20L, result.get(0).versionId());
        assertNull(result.get(1).versionNo());
        assertNull(result.get(1).recipientName());
    }
}
