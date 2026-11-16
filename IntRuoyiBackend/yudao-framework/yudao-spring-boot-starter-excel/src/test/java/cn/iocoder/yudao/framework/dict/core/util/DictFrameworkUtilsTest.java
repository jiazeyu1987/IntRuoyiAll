package cn.iocoder.yudao.framework.dict.core.util;

import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link DictFrameworkUtils} 的单元测试
 */
public class DictFrameworkUtilsTest extends BaseMockitoUnitTest {

    @Mock
    private DictDataCommonApi dictDataApi;

    @BeforeEach
    public void setUp() {
        when(dictDataApi.getCurrentTenantId()).thenReturn(1L);
        DictFrameworkUtils.init(dictDataApi);
        DictFrameworkUtils.clearCache();
    }

    @Test
    public void testParseDictDataLabel() {
        // mock 数据
        List<DictDataRespDTO> dictDatas = ListUtil.of(
                randomPojo(DictDataRespDTO.class, o -> o.setDictType("animal").setValue("cat").setLabel("猫")),
                randomPojo(DictDataRespDTO.class, o -> o.setDictType("animal").setValue("dog").setLabel("狗"))
        );
        // mock 方法
        when(dictDataApi.getDictDataList(eq("animal"))).thenReturn(dictDatas);

        // 断言返回值
        assertEquals("狗", DictFrameworkUtils.parseDictDataLabel("animal", "dog"));
    }

    @Test
    public void testParseDictDataValue() {
        // mock 数据
        List<DictDataRespDTO> dictDatas = ListUtil.of(
                randomPojo(DictDataRespDTO.class, o -> o.setDictType("animal").setValue("cat").setLabel("猫")),
                randomPojo(DictDataRespDTO.class, o -> o.setDictType("animal").setValue("dog").setLabel("狗"))
        );
        // mock 方法
        when(dictDataApi.getDictDataList(eq("animal"))).thenReturn(dictDatas);

        // 断言返回值
        assertEquals("dog", DictFrameworkUtils.parseDictDataValue("animal", "狗"));
    }

    @Test
    void cacheIsSeparatedByTenantAndInvalidationOnlyClearsTheOwner() {
        when(dictDataApi.getDictDataList("mode")).thenReturn(List.of(
                new DictDataRespDTO().setValue("1").setLabel("Production")));
        assertEquals("Production", DictFrameworkUtils.parseDictDataLabel("mode", "1"));
        when(dictDataApi.getCurrentTenantId()).thenReturn(2L);
        when(dictDataApi.getDictDataList("mode")).thenReturn(List.of(
                new DictDataRespDTO().setValue("1").setLabel("Test")));
        assertEquals("Test", DictFrameworkUtils.parseDictDataLabel("mode", "1"));
        DictFrameworkUtils.clearTenantCache(2L);
        when(dictDataApi.getCurrentTenantId()).thenReturn(1L);
        assertEquals("Production", DictFrameworkUtils.parseDictDataLabel("mode", "1"));
        when(dictDataApi.getCurrentTenantId()).thenReturn(null);
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> DictFrameworkUtils.parseDictDataLabel("mode", "1"));
    }

}
