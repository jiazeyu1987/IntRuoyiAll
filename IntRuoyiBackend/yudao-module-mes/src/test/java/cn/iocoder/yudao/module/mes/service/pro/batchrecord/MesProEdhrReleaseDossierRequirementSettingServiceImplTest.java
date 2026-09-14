package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingUpdateReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_LOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrReleaseDossierRequirementSettingServiceImplTest {

    @InjectMocks
    private MesProEdhrReleaseDossierRequirementSettingServiceImpl service;

    @Mock
    private ConfigService configService;
    @Mock
    private MesProEdhrOperationAuditService operationAuditService;

    @Test
    void getRequirementStateParsesStrictCompleteBooleanJsonAndHash() {
        ConfigDO config = config(77L,
                "{\"incomingInspectionReportRequired\":true,"
                        + "\"sterilizationReportRequired\":false,"
                        + "\"finishedProductInspectionReportRequired\":true,"
                        + "\"finishedProductInspectionRecordRequired\":false}");
        when(configService.getConfigByKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY))
                .thenReturn(config);

        MesProEdhrReleaseDossierRequirementState state = service.getRequirementState();
        EdhrReleaseDossierRequirementSettingRespVO resp = service.getRequirementSetting();

        assertTrue(state.incomingInspectionReportRequired());
        assertFalse(state.sterilizationReportRequired());
        assertTrue(state.finishedProductInspectionReportRequired());
        assertFalse(state.finishedProductInspectionRecordRequired());
        assertNotNull(state.configHash());
        assertEquals(state.configHash(), resp.getConfigHash());
        assertEquals(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY, resp.getConfigKey());
    }

    @Test
    void getRequirementStateFailsFastWhenConfigMissing() {
        when(configService.getConfigByKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY))
                .thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, service::getRequirementState);

        assertEquals(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_MISSING.getCode(), exception.getCode());
    }

    @Test
    void getRequirementStateRejectsInvalidJsonMissingFieldOrNonBooleanField() {
        assertInvalidConfig("not-json");
        assertInvalidConfig("{\"incomingInspectionReportRequired\":true,"
                + "\"sterilizationReportRequired\":false,"
                + "\"finishedProductInspectionReportRequired\":true}");
        assertInvalidConfig("{\"incomingInspectionReportRequired\":\"true\","
                + "\"sterilizationReportRequired\":false,"
                + "\"finishedProductInspectionReportRequired\":true,"
                + "\"finishedProductInspectionRecordRequired\":false}");
    }

    @Test
    void updateRequirementSettingIsReadOnlyAndRejectsMutation() {
        when(configService.getConfigByKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY))
                .thenReturn(config(77L,
                        "{\"incomingInspectionReportRequired\":false,"
                                + "\"sterilizationReportRequired\":false,"
                                + "\"finishedProductInspectionReportRequired\":false,"
                                + "\"finishedProductInspectionRecordRequired\":false}"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateRequirementSetting(
                new EdhrReleaseDossierRequirementSettingUpdateReqVO()
                        .setIncomingInspectionReportRequired(true)
                        .setSterilizationReportRequired(false)
                        .setFinishedProductInspectionReportRequired(true)
                        .setFinishedProductInspectionRecordRequired(false)));

        assertEquals(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_LOCKED.getCode(), exception.getCode());
        verifyNoInteractions(operationAuditService);
        verify(configService).getConfigByKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY);
    }

    private void assertInvalidConfig(String value) {
        when(configService.getConfigByKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY))
                .thenReturn(config(77L, value));

        ServiceException exception = assertThrows(ServiceException.class, service::getRequirementState);

        assertEquals(PRO_EDHR_RELEASE_DOSSIER_REQUIREMENT_CONFIG_INVALID.getCode(), exception.getCode());
    }

    private static ConfigDO config(Long id, String value) {
        ConfigDO config = new ConfigDO();
        config.setId(id);
        config.setCategory("mes");
        config.setName("eDHR 放行资料限制开关");
        config.setConfigKey(MesProEdhrReleaseDossierRequirementSettingService.CONFIG_KEY);
        config.setValue(value);
        config.setVisible(Boolean.TRUE);
        config.setRemark("测试配置");
        config.setUpdater("tester");
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
