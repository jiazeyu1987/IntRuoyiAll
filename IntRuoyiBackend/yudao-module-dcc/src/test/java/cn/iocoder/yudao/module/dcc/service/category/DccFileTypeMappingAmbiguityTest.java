package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccFileTypeMappingAmbiguityTest {
    @Test void duplicateActivePathMustNotSelectFirstMatch() {
        var mapper = mock(DccFileTypeTaxonomyMapper.class);
        var service = new DccFileTypeTaxonomyAdminServiceImpl();
        ReflectionTestUtils.setField(service, "taxonomyMapper", mapper);
        when(mapper.selectList()).thenReturn(List.of(row(1L, 0L, 1, "技术"),
                row(2L, 1L, 2, "阶段"), row(3L, 2L, 3, "类型"), row(4L, 2L, 3, "类型")));
        assertThrows(RuntimeException.class,
                () -> service.resolveActiveIdByPath("技术", "阶段", "类型", null, null),
                "歧义分类路径必须明确失败，不能返回第一条");
    }
    private DccFileTypeTaxonomyDO row(Long id, Long parent, int level, String name) {
        return DccFileTypeTaxonomyDO.builder().id(id).parentId(parent).levelNo(level)
                .name(name).code("T" + id).sort(1).active(true).build();
    }
}
