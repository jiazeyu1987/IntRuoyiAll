package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO.InspectionTypeRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_ITEM_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesQaInspectionStageRulesTest {
    @Test
    void commonVersionHistoryUsesCommonOwnerAndIncludesRetiredVersions() throws Exception {
        Map<Class<?>, Object> dependencies = new HashMap<>();
        var constructor = MesQaInspectionRegulationServiceImpl.class.getConstructors()[0];
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(type -> {
            Object dependency = mock(type);
            dependencies.put(type, dependency);
            return dependency;
        }).toArray();
        var service = (MesQaInspectionRegulationServiceImpl) constructor.newInstance(args);
        var projectMapper = (cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper)
                dependencies.get(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var regulationMapper = (cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper)
                dependencies.get(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper.class);
        var versionMapper = (cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper)
                dependencies.get(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper.class);
        when(projectMapper.selectById(129L)).thenReturn(
                cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder()
                        .id(129L).status("ENABLE").build());
        when(regulationMapper.selectByDccProjectCodeId(129L, "MES_QA_COMMON")).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO.builder()
                        .id(65L).currentVersionId(82L).build());
        when(versionMapper.selectListByRegulationId(65L)).thenReturn(List.of(
                cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO.builder()
                        .id(74L).versionNo("A/8").lifecycleStatus("RETIRED").build(),
                cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO.builder()
                        .id(82L).versionNo("A/9").lifecycleStatus("PUBLISHED").build()));
        var versions = service.listVersions(129L, "MES_QA_COMMON");
        assertEquals(List.of("A/8", "A/9"), versions.stream().map(v -> v.getVersionNo()).toList());
        assertFalse(versions.get(0).getCurrentPublished());
        assertTrue(versions.get(1).getCurrentPublished());
        verify(regulationMapper, never()).selectByDccProjectCodeId(129L, "MES_QA");
    }

    @Test
    void finalInspectionIsConfiguredPerProcessWithItsOwnQuantity() {
        var req = new cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO();
        req.setFinalInspectionApplicable(true);
        req.setInspectionTypeRules(List.of(rule("FIRST","FIRST"),rule("PATROL","PATROL"),rule("FINAL","FINAL")));
        var a = new cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO.InspectionItem()
            .setApplicableInspectionTypes(List.of("PATROL","FINAL")).setFinalInspectionQuantity(2);
        var b = new cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO.InspectionItem()
            .setApplicableInspectionTypes(List.of("PATROL"));
        var processA = new cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO.InspectionProcess()
            .setProcessName("启用末检工序").setItems(List.of(a));
        var processB = new cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO.InspectionProcess()
            .setProcessName("未启用末检工序").setItems(List.of(b));
        req.setProcesses(List.of(processA,processB));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(MesQaInspectionRegulationServiceImpl.class,"validateFinalInspectionApplicability",req));
        a.setFinalInspectionQuantity(null);
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(MesQaInspectionRegulationServiceImpl.class,"validateFinalInspectionApplicability",req));
        a.setFinalInspectionQuantity(2);
        processA.setItems(List.of(a,b));
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(MesQaInspectionRegulationServiceImpl.class,"validateFinalInspectionApplicability",req));
    }

    private static InspectionTypeRule rule(String key, String type) {
        return new InspectionTypeRule().setKey(key).setInspectionType(type).setRequired(true);
    }

    private static void validate(List<InspectionTypeRule> rules) {
        ReflectionTestUtils.invokeMethod(MesQaInspectionRegulationServiceImpl.class,
                "validateInspectionTypeRules", rules);
    }

    @Test
    void acceptsOnePatrolDefinitionAndExplicitFinalApplicability() {
        assertDoesNotThrow(() -> validate(List.of(rule("FIRST", "FIRST"), rule("PATROL", "PATROL"),
                rule("FINAL", "FINAL").setRequired(false))));
    }

    @Test
    void rejectsMorningAfternoonRulesBeforeTheyCanBePublished() {
        var error = assertThrows(ServiceException.class, () -> validate(List.of(
                rule("FIRST", "FIRST"), rule("PATROL_AM", "PATROL"), rule("PATROL_PM", "PATROL"),
                rule("FINAL", "FINAL"))));
        assertEquals(QA_INSPECTION_REGULATION_ITEM_INVALID.getCode(), error.getCode());
    }

    @Test
    void rejectsInvalidKeysTypesDuplicatesAndUnspecifiedApplicability() {
        for (var invalid : List.of(rule("PATROL_AM", "PATROL"), rule("PATROL", "FIRST"),
                rule("FIRST", "FIRST"), rule("PATROL", "PATROL").setRequired(null))) {
            assertThrows(ServiceException.class, () -> validate(List.of(
                    rule("FIRST", "FIRST"), invalid, rule("FINAL", "FINAL"))));
        }
    }
}
