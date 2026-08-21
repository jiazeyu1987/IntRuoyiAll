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
}
