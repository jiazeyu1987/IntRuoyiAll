package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcItemEquipmentNumberConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcItemEquipmentConfigServiceImplTest {

    @Mock
    private MesPqcItemEquipmentConfigMapper configMapper;
    @Mock
    private MesPqcItemEquipmentNumberConfigMapper numberConfigMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper regulationItemMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationMapper regulationMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesDvMachineryService machineryService;

    @InjectMocks
    private MesPqcItemEquipmentConfigServiceImpl service;

    @Test
    void loadConfigurableItemMapUsesCurrentDccVersionOnly() throws Exception {
        MesQaInspectionRegulationDO regulation = MesQaInspectionRegulationDO.builder()
                .id(1L).dccProjectCodeId(129L).build();
        MesQaInspectionRegulationVersionDO currentVersion = MesQaInspectionRegulationVersionDO.builder()
                .id(10L).regulationId(1L).lifecycleStatus("PUBLISHED").build();
        MesQaInspectionRegulationVersionDO historicalVersion = MesQaInspectionRegulationVersionDO.builder()
                .id(9L).regulationId(1L).lifecycleStatus("RETIRED").build();
        MesQaInspectionRegulationItemDO currentItem = item(10L, "PQC-IDI-001-I004", "无跳压");

        when(regulationMapper.selectByDccProjectCodeId(129L)).thenReturn(regulation);
        when(regulationVersionMapper.selectLatestDraftByRegulationId(1L)).thenReturn(null);
        when(regulationVersionMapper.selectLatestPublishedByRegulationId(1L)).thenReturn(currentVersion);
        when(regulationItemMapper.selectListByVersionId(10L)).thenReturn(List.of(currentItem));
        when(regulationVersionMapper.selectList(any(SFunction.class), any(Collection.class))).thenReturn(
                List.of(currentVersion, historicalVersion));
        when(regulationMapper.selectList(any(SFunction.class), any(Collection.class))).thenReturn(List.of(regulation));
        when(dccProjectCodeMapper.selectList(any(SFunction.class), any(Collection.class))).thenReturn(List.of(
                DccProjectCodeDO.builder().id(129L).projectName("按压式球囊扩充压力泵").build()));

        Method method = MesPqcItemEquipmentConfigServiceImpl.class
                .getDeclaredMethod("loadConfigurableItemMap", Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> result = (Map<String, ?>) method.invoke(service, 129L);

        assertThat(result).containsOnlyKeys("PQC-IDI-001-I004");
    }

    private static MesQaInspectionRegulationItemDO item(Long versionId, String code, String name) {
        return MesQaInspectionRegulationItemDO.builder()
                .id(versionId).regulationVersionId(versionId).itemCode(code).itemName(name)
                .inspectionMethod("检验").standardText("合格").samplingPlanText("1")
                .build();
    }
}
