package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProRouteTreeServiceTest {
    private MesProRouteTreeService service;
    private MesProRouteMapper routes;
    private MesProRouteVersionMapper versions;
    private MesProRouteService routeService;
    private MesProCraftService crafts;
    @BeforeEach void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), ""), MesProRouteDO.class);
        service = new MesProRouteTreeService(); routes = mock(MesProRouteMapper.class);
        versions = mock(MesProRouteVersionMapper.class); routeService = mock(MesProRouteService.class);
        crafts = mock(MesProCraftService.class);
        ObjectProvider<MesProCraftService> cp = mock(ObjectProvider.class);
        ObjectProvider<MesProRouteService> rp = mock(ObjectProvider.class);
        when(cp.getObject()).thenReturn(crafts); when(rp.getObject()).thenReturn(routeService);
        ReflectionTestUtils.setField(service,"routes",routes); ReflectionTestUtils.setField(service,"versions",versions);
        ReflectionTestUtils.setField(service,"crafts",cp); ReflectionTestUtils.setField(service,"routeService",rp);
        when(routes.updateById(any(MesProRouteDO.class))).thenReturn(1);
    }
    private MesProRouteDO node(long id, String type, Long parent) {
        var node = MesProRouteDO.builder().id(id).nodeType(type).parentId(parent).name("节点"+id).code("R"+id).build();
        when(routes.selectById(id)).thenReturn(node); return node;
    }
    @Test void parsedRouteCreatesPureDirectoryAndDoesNotCopyConfiguration() {
        node(1,"FLOW",null);
        when(routes.insert(any(MesProRouteDO.class))).thenAnswer(call -> {
            MesProRouteDO group = call.getArgument(0); group.setId(2L);
            assertEquals("GROUP-UNASSIGNED",group.getCode()); assertEquals("待分配",group.getName());
            assertEquals("GROUP",group.getNodeType()); when(routes.selectById(2L)).thenReturn(group); return 1;
        });
        service.assignParsedRoute(1L);
        verify(routes).updateById(argThat((MesProRouteDO r) -> r.getId()==1L && r.getParentId()==2L));
        verify(versions,never()).insert(any(MesProRouteVersionDO.class));
        verify(crafts).lockCatalog();
    }
    @Test void renamedUnassignedDirectoryIsReusedByCode() {
        node(1,"FLOW",null); var group=node(2,"GROUP",null); group.setName("收纳箱");group.setCode("GROUP-UNASSIGNED");
        when(routes.selectByCode("GROUP-UNASSIGNED")).thenReturn(group);
        service.assignParsedRoute(1L);
        verify(routes,never()).insert(any(MesProRouteDO.class));
        verify(routes).updateById(argThat((MesProRouteDO r) -> r.getParentId()==2L));
    }
    @Test void renameTrimsNameAndRejectsDuplicateOrBlank() {
        node(2,"GROUP",null); service.renameGroup(2L," 新目录 ");
        verify(routes).updateById(argThat((MesProRouteDO r) -> "新目录".equals(r.getName())));
        var duplicate = node(3,"GROUP",null);
        when(routes.selectByName("重复")).thenReturn(duplicate);
        assertThrows(RuntimeException.class,()->service.renameGroup(2L,"重复"));
        assertThrows(RuntimeException.class,()->service.renameGroup(2L," "));
        node(4,"FLOW",null); assertThrows(RuntimeException.class,()->service.renameGroup(4L,"流程"));
    }
    @Test void onlyDirectoryWithoutAnyChildrenCanBeDeleted() {
        var group=node(2,"GROUP",null); var emptyChild=node(3,"GROUP",2L);
        when(routes.selectList()).thenReturn(List.of(group,emptyChild));
        assertThrows(RuntimeException.class,()->service.deleteGroup(2L));
        verify(routeService,never()).deleteRoute(anyLong());
        when(routes.selectList()).thenReturn(List.of(group)); service.deleteGroup(2L);
        verify(routeService).deleteRoute(2L);
    }
    @Test void movingIntoUnassignedDirectoryDoesNotRequireTemplateOrChangeConfiguration() {
        node(1,"FLOW",null); var group=node(2,"GROUP",null); group.setCode("GROUP-UNASSIGNED");
        var active=MesProRouteVersionDO.builder().id(10L).routeId(1L).routeSnapshotSha256("hash")
                .routeSnapshotJson("{\"configSnapshots\":{\"flowGraph\":{\"nodes\":[]}}}").build();
        when(versions.selectActiveByRouteId(1L)).thenReturn(active);
        var preview=service.movePreview(1L,2L); assertEquals(false,preview.get("different"));
        service.move(1L,2L,(String)preview.get("fingerprint"),false);
        verify(routes).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class));
        verify(versions,never()).updateById(any(MesProRouteVersionDO.class));
    }
    @Test void publishingGroupCannotRestoreItsOldSnapshotName() {
        var group=node(2,"GROUP",null);group.setName("新目录名");
        var projection=new MesProRouteVersionPublishProjectionServiceImpl();
        ReflectionTestUtils.setField(projection,"routeMapper",routes);
        var snapshot=com.alibaba.fastjson.JSON.parseObject("{\"routeName\":\"旧名称\",\"routeCode\":\"G2\",\"status\":1}");
        ReflectionTestUtils.invokeMethod(projection,"updateRoute",2L,snapshot);
        verify(routes).updateById(argThat((MesProRouteDO r) -> "新目录名".equals(r.getName())));
    }
}
