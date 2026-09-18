package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDossierFileMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesActiveOrderDossierBusinessFileAccessProviderTest {

    @Test
    void resolveReturnsActiveOrderDossierReference() {
        var mapper = mock(MesProcessPoolActiveOrderDossierFileMapper.class);
        var dossierService = mock(MesActiveOrderDossierFileService.class);
        var row = dossierFile();
        when(mapper.selectListByFileId(7001L)).thenReturn(List.of(row));

        var provider = new MesActiveOrderDossierBusinessFileAccessProvider(mapper, dossierService);

        BusinessFileAccessReference reference = provider.resolve(7001L).orElseThrow();

        assertEquals(MesActiveOrderDossierBusinessFileAccessProvider.PROVIDER_ID, reference.providerId());
        assertEquals(MesActiveOrderDossierBusinessFileAccessProvider.BUSINESS_TYPE, reference.businessType());
        assertEquals(8001L, reference.businessId());
        assertTrue(reference.versionKey().contains("8001"));
        assertTrue(reference.versionKey().contains("7001"));
        assertTrue(reference.versionKey().contains("HASH"));
        assertEquals(31L, reference.tenantId());
    }

    @Test
    void assertAllowedUsesActiveOrderDossierAuthorization() {
        var mapper = mock(MesProcessPoolActiveOrderDossierFileMapper.class);
        var dossierService = mock(MesActiveOrderDossierFileService.class);
        var row = dossierFile();
        when(mapper.selectListByFileId(7001L)).thenReturn(List.of(row));
        when(dossierService.requireReadableFile(7L, 7001L)).thenReturn(row);
        var provider = new MesActiveOrderDossierBusinessFileAccessProvider(mapper, dossierService);
        BusinessFileAccessReference reference = provider.resolve(7001L).orElseThrow();

        provider.assertAllowed(new BusinessFileAccessRequest(
                BusinessFileAccessOperation.PREVIEW, 7001L, 31L, 7L,
                null, "REQ-DOSSIER-1", null, "127.0.0.1", "Playwright"), reference);

        verify(dossierService).requireReadableFile(7L, 7001L);
    }

    @Test
    void assertAllowedRejectsChangedActiveOrderDossierReference() {
        var mapper = mock(MesProcessPoolActiveOrderDossierFileMapper.class);
        var dossierService = mock(MesActiveOrderDossierFileService.class);
        var row = dossierFile();
        var changed = dossierFile().setSha256("CHANGED");
        when(mapper.selectListByFileId(7001L)).thenReturn(List.of(row));
        when(dossierService.requireReadableFile(7L, 7001L)).thenReturn(changed);
        var provider = new MesActiveOrderDossierBusinessFileAccessProvider(mapper, dossierService);
        BusinessFileAccessReference reference = provider.resolve(7001L).orElseThrow();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.assertAllowed(
                new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 7001L, 31L, 7L,
                        null, "REQ-DOSSIER-2", null, "127.0.0.1", "Playwright"), reference));

        assertTrue(ex.getMessage().contains("资料文件正式归属已变化"));
    }

    private static MesProcessPoolActiveOrderDossierFileDO dossierFile() {
        MesProcessPoolActiveOrderDossierFileDO row = MesProcessPoolActiveOrderDossierFileDO.builder()
                .id(8001L)
                .activeOrderId(10L)
                .applicationId(55L)
                .categoryKey("INCOMING_INSPECTION_FILE")
                .fileId(7001L)
                .fileUrl("http://file/incoming.pdf")
                .storageConfigId(1L)
                .storagePath("mes/active-order-dossier/10/INCOMING_INSPECTION_FILE/incoming.pdf")
                .fileName("incoming.pdf")
                .contentType("application/pdf")
                .fileSize(12L)
                .sha256("HASH")
                .operatorId(7L)
                .operatorName("生产组长")
                .operatedAt(LocalDateTime.of(2026, 9, 17, 10, 0))
                .build();
        row.setTenantId(31L);
        return row;
    }
}
