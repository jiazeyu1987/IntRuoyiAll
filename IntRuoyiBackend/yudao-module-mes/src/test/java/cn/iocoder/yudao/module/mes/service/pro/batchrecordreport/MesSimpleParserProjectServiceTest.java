package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesSimpleParserProjectServiceTest {
    @Mock private DccProjectCodeMapper projectMapper;
    @Mock private MesRouteDccProjectBindingMapper bindingMapper;
    @InjectMocks private MesSimpleParserProjectService service;

    @Test
    void listsMoreThanOnePageWithoutProjectMembershipAndKeepsOccupationSeparate() {
        when(projectMapper.selectEnabledList()).thenReturn(IntStream.rangeClosed(1, 125)
                .mapToObj(this::project).toList());
        when(bindingMapper.selectList()).thenReturn(List.of(MesRouteDccProjectBindingDO.builder()
                .dccProjectCodeId(125L).routeId(900L).build()));

        var options = service.options(null);
        assertEquals(125, options.size());
        assertFalse(options.get(0).routeOccupied());
        assertTrue(options.get(124).routeOccupied());
        assertEquals("P-125", options.get(124).projectCode());
    }

    @Test
    void searchesByCodeOrNameWithoutCaseOrWhitespaceSensitivity() {
        when(projectMapper.selectEnabledList()).thenReturn(List.of(project(1), project(2)));
        when(bindingMapper.selectList()).thenReturn(List.of());
        assertEquals(List.of(2L), service.options(" p-2 ").stream().map(
                MesSimpleParserProjectService.ProjectOption::id).toList());
        assertEquals(2, service.options("项目").size());
        assertTrue(service.options("missing").isEmpty());
    }

    private DccProjectCodeDO project(int id) {
        var project = new DccProjectCodeDO();
        project.setId((long) id);
        project.setProjectCode("P-" + id);
        project.setProjectName("项目" + id);
        project.setStatus("ENABLE");
        return project;
    }
}
