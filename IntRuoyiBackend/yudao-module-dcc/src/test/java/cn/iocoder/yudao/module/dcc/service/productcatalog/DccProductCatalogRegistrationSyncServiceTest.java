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

    @Test
    void approvedProjectReadsUniqueCurrentCertificateWithoutManualBinding() {
        var row = new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO();
        row.setProjectCodeId(22L);
        var certificate = cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO.builder()
                .id(33L).projectCodeId(22L).status("ACTIVE").currentVersionId(44L).currentSnapshotId(55L).build();
        when(certificateMapper.selectList(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(certificate));
        when(certificateMapper.selectById(33L)).thenReturn(certificate);
        when(versionMapper.selectById(44L)).thenReturn(cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO.builder()
                .id(44L).certificateId(33L).status("CURRENT").build());
        when(snapshotMapper.selectById(55L)).thenReturn(cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO.builder()
                .id(55L).versionId(44L).productName("已批准产品").residenceAddress("上海")
                .factsJson("{\"certificateType\":\"CE\",\"certificateHolder\":\"持证公司\",\"ceCertificates\":[{\"certificateNo\":\"CE-001\",\"revision\":\"1\"}],\"registrationLink\":\"https://example.com/cert\",\"expiryDate\":\"2029-12-01\"}").build());
        service.enrichCurrentCertificate(row);
        org.junit.jupiter.api.Assertions.assertEquals(33L, row.getRegistrationCertificateId());
        org.junit.jupiter.api.Assertions.assertEquals("CE-001", row.getRegistrationCertificateNumber());
        org.junit.jupiter.api.Assertions.assertEquals("持证公司", row.getCertificateHolder());
        org.junit.jupiter.api.Assertions.assertEquals("2029-12-01", row.getExpiryDate());
        org.junit.jupiter.api.Assertions.assertEquals("https://example.com/cert", row.getRegistrationInfoLink());
        verifyNoInteractions(productCatalogMapper);
    }

    @Test void multipleCertificatesRequireExplicitSelection() {
        var row = new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO();
        row.setProjectCodeId(22L);
        when(certificateMapper.selectList(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(new cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO(),
                        new cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO()));
        service.enrichCurrentCertificate(row);
        org.junit.jupiter.api.Assertions.assertNull(row.getRegistrationCertificateId());
        verifyNoInteractions(versionMapper, snapshotMapper);
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
