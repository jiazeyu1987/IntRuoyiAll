package cn.iocoder.yudao.module.dcc.service.relation;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.relation.vo.DccDataRelationCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.relation.DccDataRelationMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccDataRelationServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccDataRelationServiceImpl service;
    @Mock
    private DccDataRelationMapper relationMapper;
    @Mock
    private DccProductCatalogMapper productCatalogMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccRegistrationCertificateMapper registrationCertificateMapper;
    @Mock
    private DccProductCatalogRegistrationSyncService productCatalogRegistrationSyncService;

    @Test
    void createRelationShouldSyncCatalogFromRegistrationCertificate() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        DccProjectCodeDO projectCode = new DccProjectCodeDO();
        projectCode.setId(22L);
        projectCode.setProjectCode("P-001");
        projectCode.setStatus("ENABLE");
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setId(33L);
        certificate.setProjectCodeId(22L);
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(projectCode);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);

        service.createRelation(99L, request());

        verify(productCatalogRegistrationSyncService).syncRelation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRelationShouldRejectProjectCodeIdentityMismatch() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        catalog.setProjectCode("P-001");
        DccProjectCodeDO projectCode = new DccProjectCodeDO();
        projectCode.setId(22L);
        projectCode.setProjectCode("P-002");
        projectCode.setStatus("ENABLE");
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(projectCode);

        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
        verifyNoInteractions(registrationCertificateMapper, relationMapper, productCatalogRegistrationSyncService);
    }

    @Test
    void createRelationShouldRejectExistingRelation() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        catalog.setProjectCode("P-001");
        DccProjectCodeDO projectCode = new DccProjectCodeDO();
        projectCode.setId(22L);
        projectCode.setProjectCode("P-001");
        projectCode.setStatus("ENABLE");
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setId(33L);
        certificate.setProjectCodeId(22L);
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(projectCode);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);
        DccDataRelationDO existing = new DccDataRelationDO();
        existing.setRegistrationCertificateId(33L);
        when(relationMapper.selectCatalogProjectForUpdate(11L, 22L)).thenReturn(existing);

        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
    }

    @Test
    void createRelationWithoutCertificateShouldPersistProjectBinding() {
        prepareCatalogAndProject();
        DccDataRelationCreateReqVO req = request();
        req.setRegistrationCertificateId(null);

        DccDataRelationDO relation = service.createRelation(99L, req);

        assertNull(relation.getRegistrationCertificateId());
        assertEquals(22L, relation.getProjectCodeId());
        verify(relationMapper).insert(relation);
        verify(productCatalogRegistrationSyncService).syncRelation(relation);
        verifyNoInteractions(registrationCertificateMapper);
    }

    @Test
    void createRelationShouldAttachCertificateToExistingProjectBinding() {
        prepareCatalogAndProject();
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setId(33L);
        certificate.setProjectCodeId(22L);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);
        DccDataRelationDO existing = new DccDataRelationDO();
        existing.setId(44L);
        when(relationMapper.selectCatalogProjectForUpdate(11L, 22L)).thenReturn(existing);

        DccDataRelationDO relation = service.createRelation(99L, request());

        assertEquals(44L, relation.getId());
        assertEquals(33L, relation.getRegistrationCertificateId());
        verify(relationMapper).updateById(relation);
        verify(relationMapper, never()).insert(org.mockito.ArgumentMatchers.any(DccDataRelationDO.class));
        verify(productCatalogRegistrationSyncService).syncRelation(relation);
    }

    @Test
    void createRelationShouldRejectRepeatedProjectOnlyBinding() {
        prepareCatalogAndProject();
        when(relationMapper.selectCatalogProjectForUpdate(11L, 22L)).thenReturn(new DccDataRelationDO());
        DccDataRelationCreateReqVO req = request();
        req.setRegistrationCertificateId(null);
        assertThrows(RuntimeException.class, () -> service.createRelation(99L, req));
    }

    @Test
    void createRelationShouldRejectCertificateFromAnotherProject() {
        prepareCatalogAndProject();
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setProjectCodeId(55L);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);
        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
        verifyNoInteractions(relationMapper, productCatalogRegistrationSyncService);
    }

    @Test
    void createRelationShouldRejectCatalogFromAnotherTenant() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setTenantId(2L);
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setTenantId(1L);
        project.setStatus("ENABLE");
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(project);
        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
        verifyNoInteractions(relationMapper, productCatalogRegistrationSyncService);
    }

    @ParameterizedTest
    @CsvSource({"DISABLE,PROJECT,false", "DISABLE,PROJECT,true", "ENABLE,QMS,false",
            "ENABLE,QMS,true", "ENABLE,GENERAL,false", "ENABLE,GENERAL,true"})
    void createRelationShouldRejectDisabledOrLibraryProjectBeforeCertificateLookup(
            String status, String libraryScope, boolean withCertificate) {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setStatus(status);
        project.setLibraryScope(libraryScope);
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(project);
        DccDataRelationCreateReqVO req = request();
        if (!withCertificate) req.setRegistrationCertificateId(null);

        assertThrows(RuntimeException.class, () -> service.createRelation(99L, req));
        verifyNoInteractions(registrationCertificateMapper, relationMapper, productCatalogRegistrationSyncService);
    }

    private void prepareCatalogAndProject() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setId(22L);
        project.setProjectCode("P-001");
        project.setStatus("ENABLE");
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(project);
    }

    private DccDataRelationCreateReqVO request() {
        DccDataRelationCreateReqVO req = new DccDataRelationCreateReqVO();
        req.setProductCatalogId(11L);
        req.setProjectCodeId(22L);
        req.setRegistrationCertificateId(33L);
        return req;
    }
}
