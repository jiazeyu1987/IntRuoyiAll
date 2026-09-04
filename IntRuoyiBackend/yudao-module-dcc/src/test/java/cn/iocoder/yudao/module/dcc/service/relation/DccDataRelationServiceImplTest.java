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
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setId(33L);
        certificate.setProjectCodeId(22L);
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(projectCode);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);

        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
    }

    @Test
    void createRelationShouldRejectExistingRelation() {
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        catalog.setProjectCode("P-001");
        DccProjectCodeDO projectCode = new DccProjectCodeDO();
        projectCode.setId(22L);
        projectCode.setProjectCode("P-001");
        DccRegistrationCertificateDO certificate = new DccRegistrationCertificateDO();
        certificate.setId(33L);
        certificate.setProjectCodeId(22L);
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(projectCode);
        when(registrationCertificateMapper.selectById(33L)).thenReturn(certificate);
        when(relationMapper.selectIdentity(11L, 22L, 33L)).thenReturn(new DccDataRelationDO());

        assertThrows(RuntimeException.class, () -> service.createRelation(99L, request()));
    }

    private DccDataRelationCreateReqVO request() {
        DccDataRelationCreateReqVO req = new DccDataRelationCreateReqVO();
        req.setProductCatalogId(11L);
        req.setProjectCodeId(22L);
        req.setRegistrationCertificateId(33L);
        return req;
    }
}
