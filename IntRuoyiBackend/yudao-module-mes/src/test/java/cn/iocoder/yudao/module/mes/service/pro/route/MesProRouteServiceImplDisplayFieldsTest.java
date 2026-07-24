package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowBoundaryEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProRouteFlowConfigTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteServiceImplDisplayFieldsTest {

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProRouteProcessFlowBoundaryEdgeMapper routeProcessFlowBoundaryEdgeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesMdItemService itemService;

    @InjectMocks
    private MesProRouteServiceImpl routeService;

    @BeforeEach
    void setUpFlowGraphDefaults() {
        lenient().when(routeProcessFlowEdgeMapper.selectConfiguredRouteIdsByRouteIds(any())).thenReturn(List.of());
        lenient().when(routeProcessFlowBoundaryEdgeMapper.selectConfiguredRouteIdsByRouteIds(any())).thenReturn(List.of());
        lenient().when(routeProcessMapper.selectRelationConfiguredRouteIdsByRouteIds(any())).thenReturn(List.of());
    }

    @Test
    void getRoutePageRespVO_returnsOwnerKeyProcessAndProductCodes() {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(1L)
                .code("ROUTE-LIST-001")
                .name("Route List 001")
                .description("legacy description")
                .status(0)
                .remark("legacy remark")
                .build();
        when(routeMapper.selectPage(any())).thenReturn(new PageResult<>(List.of(route), 1L));
        when(routeProductMapper.selectListByRouteIds(Set.of(1L))).thenReturn(List.of(
                MesProRouteProductDO.builder()
                        .id(10L)
                        .routeId(1L)
                        .itemId(1001L)
                        .remark("binding bind-001 / owner 张三")
                        .build()
        ));
        when(routeProcessMapper.selectListByRouteIds(Set.of(1L))).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(100L).routeId(1L).processId(11L).sort(10).keyFlag(true).build(),
                MesProRouteProcessDO.builder().id(101L).routeId(1L).processId(12L).sort(20).build()
        ));
        when(itemService.getItemMap(Set.of(1001L))).thenReturn(java.util.Map.of(
                1001L, cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO.builder()
                        .id(1001L).code("ITEM-001").name("关联产品A").build()
        ));
        when(processMapper.selectListByIds(any())).thenReturn(List.of(
                MesProProcessDO.builder().id(11L).code("PROC-001").name("首道工序").status(0).build(),
                MesProProcessDO.builder().id(12L).code("PROC-999").name("末道工序A").status(0).build()
        ));
        when(routeVersionMapper.selectListByRouteIds(Set.of(1L))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(900L).routeId(1L).versionNo("V2").active(Boolean.TRUE).build()
        ));
        when(routeFlowConfigMapper.selectListByRouteIds(Set.of(1L))).thenReturn(List.of(
                MesProRouteFlowConfigDO.builder().routeId(1L)
                        .useType(MesProRouteFlowConfigTypeEnum.SCHEDULE.getType()).enabled(Boolean.TRUE).build(),
                MesProRouteFlowConfigDO.builder().routeId(1L)
                        .useType(MesProRouteFlowConfigTypeEnum.BATCH.getType()).enabled(Boolean.FALSE).build()
        ));

        MesProRoutePageReqVO reqVO = new MesProRoutePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesProRouteRespVO> pageResult = routeService.getRoutePageRespVO(reqVO);

        assertEquals(1L, pageResult.getTotal());
        MesProRouteRespVO row = pageResult.getList().get(0);
        assertEquals("ROUTE-LIST-001", row.getCode());
        assertEquals("张三", row.getOwnerName());
        assertEquals("首道工序", row.getKeyProcessName());
        assertEquals("末道工序A", row.getLastProcessName());
        assertEquals("ITEM-001", row.getProductCodes());
        assertEquals(900L, row.getActiveRouteVersionId());
        assertEquals("V2", row.getActiveRouteVersionNo());
        assertEquals(Boolean.TRUE, row.getScheduleRouteEnabled());
        assertEquals(Boolean.FALSE, row.getBatchRouteEnabled());
    }

    @Test
    void getRoutePageRespVO_keepsBaseFieldsWhenOwnerAndProcessMissing() {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(2L)
                .code("ROUTE-LIST-002")
                .name("Route List 002")
                .description("legacy description 2")
                .status(1)
                .remark("legacy remark 2")
                .build();
        when(routeMapper.selectPage(any())).thenReturn(new PageResult<>(List.of(route), 1L));
        when(routeProductMapper.selectListByRouteIds(Set.of(2L))).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteIds(Set.of(2L))).thenReturn(List.of());
        when(routeVersionMapper.selectListByRouteIds(Set.of(2L))).thenReturn(List.of());
        when(routeFlowConfigMapper.selectListByRouteIds(Set.of(2L))).thenReturn(List.of());

        MesProRoutePageReqVO reqVO = new MesProRoutePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setCode("ROUTE-LIST-002");

        PageResult<MesProRouteRespVO> pageResult = routeService.getRoutePageRespVO(reqVO);

        assertEquals(1L, pageResult.getTotal());
        MesProRouteRespVO row = pageResult.getList().get(0);
        assertEquals(route.getCode(), row.getCode());
        assertEquals(route.getName(), row.getName());
        assertEquals(route.getStatus(), row.getStatus());
        assertNull(row.getOwnerName());
        assertNull(row.getKeyProcessName());
        assertNull(row.getLastProcessName());
        assertNull(row.getProductCodes());
        assertEquals(Boolean.FALSE, row.getScheduleRouteEnabled());
        assertEquals(Boolean.FALSE, row.getBatchRouteEnabled());
    }

    @Test
    void getRoutePageRespVO_returnsPendingRouteVersionAtAGlance() {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(4L)
                .code("ROUTE-LIST-004")
                .name("Route List 004")
                .status(0)
                .remark("route with candidate versions")
                .build();
        when(routeMapper.selectPage(any())).thenReturn(new PageResult<>(List.of(route), 1L));
        when(routeProductMapper.selectListByRouteIds(Set.of(4L))).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteIds(Set.of(4L))).thenReturn(List.of());
        when(routeFlowConfigMapper.selectListByRouteIds(Set.of(4L))).thenReturn(List.of());
        when(routeVersionMapper.selectListByRouteIds(Set.of(4L))).thenReturn(List.of(
                MesProRouteVersionDO.builder()
                        .id(904L).routeId(4L).versionNo("V4").active(Boolean.FALSE).lifecycleStatus("DRAFT").build(),
                MesProRouteVersionDO.builder()
                        .id(903L).routeId(4L).versionNo("V3").active(Boolean.FALSE).lifecycleStatus("READY_TO_PUBLISH").build(),
                MesProRouteVersionDO.builder()
                        .id(902L).routeId(4L).versionNo("V2").active(Boolean.FALSE).lifecycleStatus("CANCELLED").build(),
                MesProRouteVersionDO.builder()
                        .id(901L).routeId(4L).versionNo("V1").active(Boolean.TRUE).lifecycleStatus("ACTIVE").build()
        ));

        MesProRoutePageReqVO reqVO = new MesProRoutePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<MesProRouteRespVO> pageResult = routeService.getRoutePageRespVO(reqVO);

        MesProRouteRespVO row = pageResult.getList().get(0);
        assertEquals(901L, row.getActiveRouteVersionId());
        assertEquals("V1", row.getActiveRouteVersionNo());
        assertEquals(903L, row.getPendingRouteVersionId());
        assertEquals("V3", row.getPendingRouteVersionNo());
        assertEquals("READY_TO_PUBLISH", row.getPendingRouteVersionStatus());
        assertEquals(2, row.getPendingRouteVersionCount());
    }

    @Test
    void updateRoute_persistsOwnerIntoStructuredRemarkAndGetRouteRespVOCleansIt() {
        MesProRouteDO route = MesProRouteDO.builder()
                .id(3L)
                .code("ROUTE-LIST-003")
                .name("Route List 003")
                .description("legacy description 3")
                .status(0)
                .remark("原始备注")
                .build();
        when(routeMapper.selectById(3L)).thenReturn(route);
        when(routeMapper.selectByCode("ROUTE-LIST-003")).thenReturn(null);
        when(routeProductMapper.selectListByRouteIds(Set.of(3L))).thenReturn(List.of());
        when(routeProcessMapper.selectListByRouteIds(Set.of(3L))).thenReturn(List.of());
        when(routeFlowConfigMapper.selectListByRouteIds(Set.of(3L))).thenReturn(List.of());
        when(routeVersionMapper.selectActiveByRouteId(3L)).thenReturn(null);

        var saveReqVO = new cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO();
        saveReqVO.setId(3L);
        saveReqVO.setCode("ROUTE-LIST-003");
        saveReqVO.setName("Route List 003");
        saveReqVO.setDescription("legacy description 3");
        saveReqVO.setRemark("用户备注");
        saveReqVO.setOwnerName("李四");

        routeService.updateRoute(saveReqVO);

        verify(routeMapper).updateById(org.mockito.ArgumentMatchers.<MesProRouteDO>argThat(updated ->
                updated != null
                        && "[owner]李四[/owner]\n用户备注".equals(updated.getRemark())
        ));

        when(routeMapper.selectById(3L)).thenReturn(MesProRouteDO.builder()
                .id(3L)
                .code("ROUTE-LIST-003")
                .name("Route List 003")
                .description("legacy description 3")
                .status(0)
                .remark("[owner]李四[/owner]\n用户备注")
                .build());
        when(routeVersionMapper.selectListByRouteIds(Set.of(3L))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(901L).routeId(3L).versionNo("V1").active(Boolean.TRUE).build()
        ));

        MesProRouteRespVO respVO = routeService.getRouteRespVO(3L);
        assertEquals("李四", respVO.getOwnerName());
        assertEquals("用户备注", respVO.getRemark());
        assertEquals(901L, respVO.getActiveRouteVersionId());
        assertEquals("V1", respVO.getActiveRouteVersionNo());
    }
}
