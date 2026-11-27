package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccProductCatalogRegistrationSyncServiceTest extends BaseMockitoUnitTest {
    @InjectMocks
    private DccProductCatalogRegistrationSyncService service;
    @Mock
    private DccProductCatalogMapper productCatalogMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;
    @Mock
    private DccRegistrationCertificateMapper certificateMapper;
    @Mock private cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper versionMapper;
    @Mock private cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper snapshotMapper;

    @Mock private cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProductBindingService productBindings;
    @org.junit.jupiter.api.BeforeEach void tenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L); }
    @org.junit.jupiter.api.AfterEach void clearTenant() { cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear(); }
    @Test
    void approvedProjectReadsUniqueCurrentCertificateWithoutManualBinding() {
        var row = new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO();
        row.setProjectCodeId(22L);row.setId(11L);
        var certificate = cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO.builder()
                .id(33L).projectCodeId(22L).status("ACTIVE").currentVersionId(44L).currentSnapshotId(55L).build();
        when(productBindings.currentCertificateIds(1L,11L)).thenReturn(java.util.List.of(33L));
        when(productBindings.containsProduct(1L,33L,11L)).thenReturn(true);
        certificate.setTenantId(1L);
        when(certificateMapper.selectById(33L)).thenReturn(certificate);
        var version=cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO.builder().id(44L).certificateId(33L).status("CURRENT").build();version.setTenantId(1L);
        when(versionMapper.selectById(44L)).thenReturn(version);
        var snapshot=cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO.builder()
                .id(55L).versionId(44L).productName("已批准产品").residenceAddress("上海")
                .factsJson("{\"certificateType\":\"CE\",\"certificateHolder\":\"持证公司\",\"ceCertificates\":[{\"certificateNo\":\"CE-001\",\"revision\":\"1\"}],\"registrationLink\":\"https://example.com/cert\",\"expiryDate\":\"2029-12-01\"}").build();snapshot.setTenantId(1L);
        when(snapshotMapper.selectById(55L)).thenReturn(snapshot);
        service.enrichCurrentCertificate(row);
        org.junit.jupiter.api.Assertions.assertEquals(33L, row.getRegistrationCertificateId());
        org.junit.jupiter.api.Assertions.assertEquals("CE-001", row.getRegistrationCertificateNumber());
        org.junit.jupiter.api.Assertions.assertEquals("持证公司", row.getCertificateHolder());
        org.junit.jupiter.api.Assertions.assertEquals("2029-12-01", row.getExpiryDate());
        org.junit.jupiter.api.Assertions.assertEquals("https://example.com/cert", row.getRegistrationInfoLink());
        org.junit.jupiter.api.Assertions.assertEquals(1,row.getRegistrationCertificates().size());
        org.junit.jupiter.api.Assertions.assertEquals("CE-001",row.getRegistrationCertificates().get(0).certificateNumber());
        verifyNoInteractions(productCatalogMapper);
    }

    @Test void pendingProductDoesNotExposeAnUnapprovedCertificate() {
        var row = new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO();
        row.setProjectCodeId(22L);row.setId(11L);
        when(productBindings.currentCertificateIds(1L,11L)).thenReturn(java.util.List.of());
        service.enrichCurrentCertificate(row);
        org.junit.jupiter.api.Assertions.assertNull(row.getRegistrationCertificateId());
        verifyNoInteractions(versionMapper, snapshotMapper);
    }

    @Test void severalCertificatesAreListedWithoutArbitrarilySelectingOne() {
        var row=new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO();
        row.setId(11L);row.setProjectCodeId(22L);
        when(productBindings.currentCertificateIds(1L,11L)).thenReturn(java.util.List.of(33L,34L));
        for(long id:java.util.List.of(33L,34L)) {
            var c=cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO.builder()
                .id(id).status("ACTIVE").currentVersionId(id+100).currentSnapshotId(id+200).build();c.setTenantId(1L);
            var v=cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO.builder()
                .id(id+100).certificateId(id).status("CURRENT").build();v.setTenantId(1L);
            var snapshot=cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO.builder()
                .id(id+200).versionId(id+100).productName("产品证"+id)
                .factsJson("{\"certificateType\":\"DOMESTIC\",\"certificateNo\":\"CERT-"+id+"\"}").build();snapshot.setTenantId(1L);
            when(certificateMapper.selectById(id)).thenReturn(c);when(versionMapper.selectById(id+100)).thenReturn(v);
            when(snapshotMapper.selectById(id+200)).thenReturn(snapshot);
        }
        service.enrichCurrentCertificate(row);
        org.junit.jupiter.api.Assertions.assertNull(row.getRegistrationCertificateId());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("CERT-33","CERT-34"),
            row.getRegistrationCertificates().stream().map(x->x.certificateNumber()).toList());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(33L,34L),
            row.getRegistrationCertificates().stream().map(x->x.certificateId()).toList());
    }

    @Test
    void projectOnlyBindingShouldSyncProjectWithoutReadingOrChangingCertificate() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccProductCatalogDO.class);
        DccProductCatalogDO catalog = new DccProductCatalogDO();
        catalog.setId(11L);
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setId(22L);
        project.setProjectCode("P-001");
        project.setProjectName("新项目");
        when(productCatalogMapper.selectById(11L)).thenReturn(catalog);
        when(projectCodeMapper.selectById(22L)).thenReturn(project);
        DccDataRelationDO relation = new DccDataRelationDO();
        relation.setProductCatalogId(11L);
        relation.setProjectCodeId(22L);

        service.syncRelation(relation);

        ArgumentCaptor<LambdaUpdateWrapper<DccProductCatalogDO>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(productCatalogMapper).update(isNull(), update.capture());
        assertTrue(update.getValue().getSqlSet().contains("project_code="));
        assertTrue(update.getValue().getSqlSet().contains("project_name="));
        assertFalse(update.getValue().getSqlSet().contains("registration_certificate"));
        verifyNoInteractions(certificateMapper);
    }
}
