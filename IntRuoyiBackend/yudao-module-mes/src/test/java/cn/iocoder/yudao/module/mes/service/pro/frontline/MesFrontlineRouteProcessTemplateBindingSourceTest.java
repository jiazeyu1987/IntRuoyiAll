package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineRouteProcessTemplateBindingSourceTest {

    @Mock
    private MesProRouteProcessMapper routeProcessMapper;

    private MesFrontlineRouteProcessTemplateBindingSource bindingSource;

    @BeforeEach
    void setUp() {
        bindingSource = new MesFrontlineRouteProcessTemplateBindingSource(routeProcessMapper);
    }

    @Test
    void shouldResolveProductionTemplateFromNonCheckRouteProcess() {
        when(routeProcessMapper.selectById(1001L)).thenReturn(routeProcess(1001L, 101L, 201L, false));

        MesFrontlineTemplateDescriptor template = bindingSource.findTemplate(
                new MesFrontlineTemplateRequest(9001L, 10001L, 101L, 1001L, 201L));

        assertEquals(FrontlineTemplateCodes.PRODUCTION_SIMPLIFIED, template.templateNo());
        assertEquals(FrontlineTemplateTypes.PRODUCTION, template.templateType());
        assertEquals(1001L, template.routeProcessId());
        assertEquals(201L, template.processId());
        assertEquals(10001L, template.actualEmployeeId());
    }

    @Test
    void shouldResolvePqcTemplateFromCheckRouteProcess() {
        when(routeProcessMapper.selectById(1002L)).thenReturn(routeProcess(1002L, 101L, 202L, true));

        MesFrontlineTemplateDescriptor template = bindingSource.findTemplate(
                new MesFrontlineTemplateRequest(9001L, 10002L, 101L, 1002L, 202L));

        assertEquals(FrontlineTemplateCodes.PQC_SIMPLIFIED, template.templateNo());
        assertEquals(FrontlineTemplateTypes.PQC, template.templateType());
        assertEquals(1002L, template.routeProcessId());
        assertEquals(202L, template.processId());
        assertEquals(10002L, template.actualEmployeeId());
    }

    @Test
    void shouldFailWhenRouteProcessDoesNotMatchRequestContext() {
        when(routeProcessMapper.selectById(1003L)).thenReturn(routeProcess(1003L, 101L, 203L, false));

        MesFrontlineTemplateDescriptor template = bindingSource.findTemplate(
                new MesFrontlineTemplateRequest(9001L, 10003L, 101L, 1003L, 999L));

        assertNull(template);
    }

    private static MesProRouteProcessDO routeProcess(Long id, Long routeId, Long processId, Boolean checkFlag) {
        return MesProRouteProcessDO.builder()
                .id(id)
                .routeId(routeId)
                .processId(processId)
                .checkFlag(checkFlag)
                .build();
    }

}
