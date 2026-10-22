package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class MesEdhrNcrMaterialMapperContractTest {
    @Test
    void queryIncludesPrimaryAndActiveNumericOrStringIdsWithoutPrecisionLoss() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),
                MesProEdhrNonconformanceReviewDO.class.getName()), MesProEdhrNonconformanceReviewDO.class);
        var mapper = mock(MesProEdhrNonconformanceReviewMapper.class, CALLS_REAL_METHODS);
        doReturn(List.of()).when(mapper).selectList(any(Wrapper.class));
        long id = 9007199254740993L;
        mapper.selectListByReviewMaterialFileId(id);
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        var wrapper = (AbstractWrapper<?, ?, ?>) captor.getValue();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("review_material_file_id ="));
        assertEquals(2, sql.split("JSON_CONTAINS", -1).length - 1);
        assertEquals(2, sql.split("\\$\\.activeMaterials", -1).length - 1);
        assertFalse(sql.contains("reviewMaterialEvents"));
        assertFalse(sql.contains(Long.toString(id)), "identities must be bound parameters");
        assertEquals(3, wrapper.getParamNameValuePairs().size());
        assertEquals(2, wrapper.getParamNameValuePairs().values().stream().filter(Long.valueOf(id)::equals).count());
        assertTrue(wrapper.getParamNameValuePairs().containsValue(Long.toString(id)));
    }
}
