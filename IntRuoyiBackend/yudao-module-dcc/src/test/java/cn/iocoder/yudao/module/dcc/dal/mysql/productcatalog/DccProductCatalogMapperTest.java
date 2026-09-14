package cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class DccProductCatalogMapperTest {

    @Test
    void selectPageShouldExcludeNullEmptyAndBlankProjectCodesWhenRequested() {
        String sqlSegment = captureSqlSegment(true);

        assertTrue(sqlSegment.contains("project_code IS NOT NULL"));
        assertTrue(sqlSegment.contains("TRIM(project_code) <> ''"));
    }

    @Test
    void selectPageShouldNotFilterProjectCodeWhenNotRequested() {
        String sqlSegment = captureSqlSegment(null);

        assertFalse(sqlSegment.contains("project_code IS NOT NULL"));
        assertFalse(sqlSegment.contains("TRIM(project_code) <> ''"));
    }

    @Test
    void selectPageShouldFilterAllVisibleTitlesExceptRecognitionJson() {
        String sqlSegment = captureSqlSegmentWithVisibleTitleFilters();

        for (String expectedColumn : new String[] {
                "category_level1",
                "category_level2",
                "product_sequence",
                "product",
                "data_source",
                "product_code",
                "project_name",
                "project_code",
                "registration_certificate_name",
                "registration_certificate_number",
                "certificate_holder",
                "registration_place",
                "effective_date",
                "expiry_date",
                "classification",
                "product_status",
                "registration_info_link",
                "remark"
        }) {
            assertTrue(sqlSegment.contains(expectedColumn), "missing filter column: " + expectedColumn);
        }
        assertFalse(sqlSegment.contains("batch_record_total_recognition_json"));
    }

    private String captureSqlSegment(Boolean projectCodeNotBlank) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccProductCatalogDO.class);
        DccProductCatalogMapper mapper = mock(DccProductCatalogMapper.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS));
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Wrapper<DccProductCatalogDO>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) Wrapper.class);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(PageParam.class), wrapperCaptor.capture());
        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProjectCodeNotBlank(projectCodeNotBlank);

        mapper.selectPage(reqVO);

        return wrapperCaptor.getValue().getSqlSegment();
    }

    private String captureSqlSegmentWithVisibleTitleFilters() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccProductCatalogDO.class);
        DccProductCatalogMapper mapper = mock(DccProductCatalogMapper.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS));
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Wrapper<DccProductCatalogDO>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) Wrapper.class);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(PageParam.class), wrapperCaptor.capture());
        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setCategoryLevel1("类别一");
        reqVO.setCategoryLevel2("类别二");
        reqVO.setProductSequence("1");
        reqVO.setProduct("产品");
        reqVO.setDataSource("瑛泰产品");
        reqVO.setProductCode("031314");
        reqVO.setProjectName("项目");
        reqVO.setProjectCode("IDI");
        reqVO.setRegistrationCertificateName("注册证名称");
        reqVO.setRegistrationCertificateNumber("沪械注准");
        reqVO.setCertificateHolder("持证人");
        reqVO.setRegistrationPlace("上海");
        reqVO.setEffectiveDate("2025");
        reqVO.setExpiryDate("2030");
        reqVO.setClassification("二类");
        reqVO.setProductStatus("S");
        reqVO.setRegistrationInfoLink("http");
        reqVO.setRemark("备注");

        mapper.selectPage(reqVO);

        return wrapperCaptor.getValue().getSqlSegment();
    }
}
