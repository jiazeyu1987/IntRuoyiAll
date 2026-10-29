package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryTypeExcelVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesDvMachineryTypeWorkbookServiceTest {
    @InjectMocks private MesDvMachineryTypeWorkbookService subject;
    @Mock private MesDvMachineryTypeMapper mapper;
    @Mock private MesDvMachineryTypeService service;
    @Mock private SecurityFrameworkService security;

    private MesDvMachineryTypeExcelVO row(String code, String parent) {
        var row = new MesDvMachineryTypeExcelVO();
        row.setCode(code); row.setName(code); row.setParentCode(parent); row.setStatus(0); row.setSort(0);
        return row;
    }

    @Test void importsParentBeforeChildEvenWhenRowsAreReversed() {
        Map<String, MesDvMachineryTypeDO> saved = new HashMap<>();
        when(mapper.selectByCode(anyString())).thenAnswer(call -> saved.get(call.getArgument(0)));
        when(security.hasPermission("mes:dv-machinery-type:create")).thenReturn(true);
        when(service.createMachineryType(any())).thenAnswer(call -> {
            cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.type.MesDvMachineryTypeSaveReqVO req = call.getArgument(0);
            long id = saved.size() + 10L;
            saved.put(req.getCode(), MesDvMachineryTypeDO.builder().id(id).code(req.getCode()).build());
            if ("CHILD".equals(req.getCode())) assertEquals(10L, req.getParentId());
            return id;
        });
        subject.importTypes(List.of(row("CHILD", "PARENT"), row("PARENT", null)), false);
        assertEquals(Set.of("PARENT", "CHILD"), saved.keySet());
    }

    @Test void rejectsCyclesAndDuplicateCodes() {
        assertThrows(ServiceException.class, () -> subject.importTypes(List.of(row("A", "B"), row("B", "A")), false));
        assertThrows(ServiceException.class, () -> subject.importTypes(List.of(row("A", null), row("A", null)), false));
        verifyNoInteractions(service);
    }

    @Test void rejectsMissingParentAndMissingPermission() {
        assertThrows(ServiceException.class, () -> subject.importTypes(List.of(row("A", "MISSING")), false));
        assertThrows(AccessDeniedException.class, () -> subject.importTypes(List.of(row("A", null)), false));
        verifyNoInteractions(service);
    }

    @Test void exportsParentCodeInsteadOfDatabaseId() {
        when(security.hasPermission("mes:dv-machinery-type:query")).thenReturn(true);
        when(mapper.selectList()).thenReturn(List.of(
                MesDvMachineryTypeDO.builder().id(10L).code("PARENT").parentId(0L).build(),
                MesDvMachineryTypeDO.builder().id(20L).code("CHILD").parentId(10L).build()));
        var rows = subject.exportTypes();
        assertNull(rows.get(0).getParentCode());
        assertEquals("PARENT", rows.get(1).getParentCode());
    }
}
