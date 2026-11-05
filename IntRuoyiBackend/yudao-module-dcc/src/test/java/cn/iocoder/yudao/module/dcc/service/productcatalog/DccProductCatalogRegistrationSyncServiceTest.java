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
