package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class MesRouteDccProjectBindingServiceTest {

    @InjectMocks
    private MesRouteDccProjectBindingServiceImpl service;

    @Mock
    private MesRouteDccProjectBindingMapper bindingMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteProductBomMapper routeProductBomMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProRouteCandidateConfigService candidateConfigService;

    @Test
    void bindInitialRoute_createsVersionOneFromExpectedZero() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(null);
        when(dccProjectCodeMapper.selectById(200L)).thenReturn(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        stubRouteProducts(100L);

        MesRouteDccProjectBindingRespVO result = service.saveBinding(new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setExpectedVersion(0L).setConfirmedProductItemIds(List.of(1001L)));

        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteId());
        assertEquals(200L, captor.getValue().getDccProjectCodeId());
        assertEquals(1L, captor.getValue().getVersion());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getDeleted()));
        assertEquals(1L, result.getVersion());
        assertEquals(200L, result.getDccProjectCodeId());
        assertTrue(result.getBound());
    }

    @Test
    void rebind_rejectsStaleExpectedVersionAndKeepsCurrentBinding() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(
                new MesRouteDccProjectBindingSaveReqVO()
                        .setRouteId(100L)
                        .setDccProjectCodeId(201L)
                        .setExpectedVersion(2L)));

        assertEquals(PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(bindingMapper, never()).markDeletedById(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void rebind_closesOldCurrentAndCreatesNextVersion() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);
        when(dccProjectCodeMapper.selectById(201L)).thenReturn(dccProjectCode(201L, DccProjectCodeStatusConstants.ENABLE));
        stubRouteProducts(100L);

        MesRouteDccProjectBindingRespVO result = service.saveBinding(new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(201L)
                .setExpectedVersion(3L).setConfirmedProductItemIds(List.of(1001L)));

        verify(bindingMapper).markDeletedById(10L);
        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(201L, captor.getValue().getDccProjectCodeId());
        assertEquals(4L, captor.getValue().getVersion());
        assertEquals(4L, result.getVersion());
        assertEquals(201L, result.getDccProjectCodeId());
        assertTrue(result.getBound());
    }

    @Test
    void bind_rejectsDisabledDccProjectCode() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(null);
        when(dccProjectCodeMapper.selectById(200L)).thenReturn(dccProjectCode(200L, DccProjectCodeStatusConstants.DISABLE));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(
                new MesRouteDccProjectBindingSaveReqVO()
                        .setRouteId(100L)
                        .setDccProjectCodeId(200L)
                        .setExpectedVersion(0L)));

        assertEquals(PRO_ROUTE_DCC_PROJECT_INVALID.getCode(), ex.getCode());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_routeWithoutProductsCreatesProjectBinding() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());

        MesRouteDccProjectBindingRespVO result = service.saveBinding(initialRequest());

        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteId());
        assertEquals(200L, captor.getValue().getDccProjectCodeId());
        assertEquals(1L, captor.getValue().getVersion());
        assertEquals(200L, result.getDccProjectCodeId());
        assertEquals(1L, result.getVersion());
        assertTrue(result.getBound());
        verify(routeProductMapper, never()).deleteByRouteId(any());
    }

    @Test
    void rebind_routeWithoutProductsClosesOldBindingAndCreatesNextVersion() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L))
                .thenReturn(binding(10L, 100L, 200L, 3L, false));
        when(dccProjectCodeMapper.selectById(201L))
                .thenReturn(dccProjectCode(201L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());

        MesRouteDccProjectBindingRespVO result = service.saveBinding(new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L).setDccProjectCodeId(201L).setExpectedVersion(3L));

        verify(bindingMapper).markDeletedById(10L);
        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(201L, captor.getValue().getDccProjectCodeId());
        assertEquals(4L, captor.getValue().getVersion());
        assertEquals(201L, result.getDccProjectCodeId());
        assertEquals(4L, result.getVersion());
        assertTrue(result.getBound());
        verify(routeProductMapper, never()).deleteByRouteId(any());
    }

    @Test
    void productionProductIdentityCheck_stillRejectsRouteWithoutProducts() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> MesRouteDccProductMasterInvariant.requireMatching(100L, List.of(), Map.of(),
                        dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE)));

        assertEquals(1_040_501_502, ex.getCode());
    }

    @Test
    void bind_productsWithoutConfirmationRejectsBeforeAnyWrite() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(initialRequest()));

        assertEquals(1_040_501_508, ex.getCode());
        verify(routeProductMapper, never()).deleteByRouteId(any());
        verify(routeProductBomMapper, never()).deleteByRouteId(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_changedProductListRejectsBeforeAnyWrite() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L))
                .thenReturn(List.of(routeProduct(1001L), routeProduct(1002L)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(
                initialRequest().setConfirmedProductItemIds(List.of(1001L))));

        assertEquals(1_040_501_508, ex.getCode());
        verify(routeProductMapper, never()).deleteByRouteId(any());
        verify(routeProductBomMapper, never()).deleteByRouteId(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_confirmedProductsClearsBomsAndRelationsBeforeBinding() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));

        MesRouteDccProjectBindingRespVO result = service.saveBinding(
                initialRequest().setConfirmedProductItemIds(List.of(1001L)));

        var order = inOrder(routeProductBomMapper, routeProductMapper, bindingMapper);
        order.verify(routeProductBomMapper).deleteByRouteId(100L);
        order.verify(routeProductMapper).deleteByRouteId(100L);
        order.verify(bindingMapper).insert(any(MesRouteDccProjectBindingDO.class));
        assertTrue(result.getBound());
        assertEquals(200L, result.getDccProjectCodeId());
    }

    @Test
    void bind_productDeletionFailureDoesNotReplaceProjectBinding() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));
        doThrow(new IllegalStateException("delete failed")).when(routeProductMapper).deleteByRouteId(100L);

        assertThrows(IllegalStateException.class, () -> service.saveBinding(
                initialRequest().setConfirmedProductItemIds(List.of(1001L))));

        verify(bindingMapper, never()).markDeletedById(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_projectWriteFailureRollsBackDraftAndFormalProductsAndBoms() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:dcc-bind-rollback;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE route_products (route_id BIGINT, item_id BIGINT)");
        jdbc.execute("CREATE TABLE route_boms (route_id BIGINT, item_id BIGINT)");
        jdbc.execute("CREATE TABLE project_bindings (route_id BIGINT, project_id BIGINT)");
        jdbc.execute("CREATE TABLE draft_snapshots (version_id BIGINT, snapshot VARCHAR(200))");
        jdbc.update("INSERT INTO route_products VALUES (100, 1001)");
        jdbc.update("INSERT INTO route_boms VALUES (100, 1001)");
        jdbc.update("INSERT INTO project_bindings VALUES (100, 200)");
        jdbc.update("INSERT INTO draft_snapshots VALUES (500, 'original-products-and-boms')");
        stubDraftProducts(List.of(routeProduct(1001L)));
        doAnswer(call -> {
            jdbc.update("UPDATE draft_snapshots SET snapshot = 'empty-products-and-boms' WHERE version_id = 500");
            return null;
        }).when(candidateConfigService).saveConfigSnapshots(500L, "draft-sha",
                Map.of("products", Map.of(), "productBoms", Map.of()));
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L))
                .thenReturn(binding(10L, 100L, 200L, 3L, false));
        when(dccProjectCodeMapper.selectById(201L))
                .thenReturn(dccProjectCode(201L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));
        doAnswer(call -> { jdbc.update("DELETE FROM route_boms WHERE route_id = 100"); return null; })
                .when(routeProductBomMapper).deleteByRouteId(100L);
        doAnswer(call -> { jdbc.update("DELETE FROM route_products WHERE route_id = 100"); return null; })
                .when(routeProductMapper).deleteByRouteId(100L);
        when(bindingMapper.markDeletedById(10L)).thenAnswer(call ->
                jdbc.update("DELETE FROM project_bindings WHERE route_id = 100"));
        when(bindingMapper.insert(any(MesRouteDccProjectBindingDO.class)))
                .thenThrow(new IllegalStateException("project save failed"));
        var proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        var transactionalService = (MesRouteDccProjectBindingService) proxyFactory.getProxy();

        assertThrows(IllegalStateException.class, () -> transactionalService.saveBinding(
                initialRequest().setDccProjectCodeId(201L).setExpectedVersion(3L)
                        .setRouteVersionId(500L)
                        .setConfirmedProductItemIds(List.of(1001L))));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM route_products", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM route_boms", Integer.class));
        assertEquals(200L, jdbc.queryForObject("SELECT project_id FROM project_bindings", Long.class));
        assertEquals("original-products-and-boms", jdbc.queryForObject("SELECT snapshot FROM draft_snapshots", String.class));
        jdbc.execute("DROP ALL OBJECTS");
    }

    @Test
    void bind_sameProjectStillClearsConfirmedProducts() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L))
                .thenReturn(binding(10L, 100L, 200L, 3L, false));
        when(dccProjectCodeMapper.selectById(200L))
                .thenReturn(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));

        MesRouteDccProjectBindingRespVO result = service.saveBinding(initialRequest()
                .setExpectedVersion(3L).setConfirmedProductItemIds(List.of(1001L)));

        verify(routeProductBomMapper).deleteByRouteId(100L);
        verify(routeProductMapper).deleteByRouteId(100L);
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
        assertEquals(3L, result.getVersion());
    }

    @Test
    void unbind_writesTombstoneVersionAndReturnsUnboundState() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);

        MesRouteDccProjectBindingRespVO result = service.deleteBinding(100L, 3L);

        verify(bindingMapper).markDeletedById(10L);
        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteId());
        assertEquals(200L, captor.getValue().getDccProjectCodeId());
        assertEquals(4L, captor.getValue().getVersion());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getDeleted()));
        assertEquals(4L, result.getVersion());
        assertNull(result.getDccProjectCodeId());
        assertFalse(result.getBound());
    }

    @Test
    void getBinding_returnsLatestHistoryVersionWhenUnbound() {
        when(routeMapper.selectById(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteId(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(4L);

        MesRouteDccProjectBindingRespVO result = service.getBinding(100L);

        assertEquals(4L, result.getVersion());
        assertNull(result.getDccProjectCodeId());
        assertFalse(result.getBound());
    }

    @Test
    void getBinding_rejectsMissingRoute() {
        when(routeMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getBinding(100L));

        assertEquals(PRO_ROUTE_NOT_EXISTS.getCode(), ex.getCode());
    }

    private MesProRouteDO route(Long routeId) {
        return MesProRouteDO.builder().id(routeId).code("RT-" + routeId).name("route").build();
    }

    @Test
    void bind_draftOnlyProductsRequireConfirmationBeforeWrites() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());
        stubDraftProducts(List.of(routeProduct(1002L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveBinding(initialRequest().setRouteVersionId(500L)));

        assertEquals(1_040_501_508, ex.getCode());
        verify(candidateConfigService, never()).saveConfigSnapshots(any(), any(), any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_confirmedDraftProductsAndBomsClearBeforeProjectSave() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());
        stubDraftProducts(List.of(routeProduct(1002L)));

        service.saveBinding(initialRequest().setRouteVersionId(500L).setConfirmedProductItemIds(List.of(1002L)));

        var order = inOrder(candidateConfigService, bindingMapper);
        order.verify(candidateConfigService).saveConfigSnapshots(500L, "draft-sha",
                Map.of("products", Map.of(), "productBoms", Map.of()));
        order.verify(bindingMapper).insert(any(MesRouteDccProjectBindingDO.class));
        verify(routeProductMapper, never()).deleteByRouteId(any());
    }

    @Test
    void bind_confirmationCoversUnionOfFormalAndDraftProducts() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L))
                .thenReturn(List.of(routeProduct(1001L), routeProduct(1002L)));
        stubDraftProducts(List.of(routeProduct(1002L), routeProduct(1003L)));

        service.saveBinding(initialRequest().setRouteVersionId(500L)
                .setConfirmedProductItemIds(List.of(1001L, 1002L, 1003L)));

        verify(candidateConfigService).saveConfigSnapshots(500L, "draft-sha",
                Map.of("products", Map.of(), "productBoms", Map.of()));
        verify(routeProductMapper).deleteByRouteId(100L);
        verify(routeProductBomMapper).deleteByRouteId(100L);
    }

    @Test
    void bind_changedDraftProductListRejectsBeforeWrites() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());
        stubDraftProducts(List.of(routeProduct(1002L), routeProduct(1003L)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(initialRequest()
                .setRouteVersionId(500L).setConfirmedProductItemIds(List.of(1002L))));

        assertEquals(1_040_501_508, ex.getCode());
        verify(candidateConfigService, never()).saveConfigSnapshots(any(), any(), any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void bind_rejectsForeignOrNonDraftVersion() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of());
        when(routeVersionMapper.selectById(500L)).thenReturn(
                MesProRouteVersionDO.builder().id(500L).routeId(101L).active(false).lifecycleStatus("DRAFT").build(),
                MesProRouteVersionDO.builder().id(500L).routeId(100L).active(true).lifecycleStatus("ACTIVE").build());

        assertThrows(ServiceException.class, () -> service.saveBinding(initialRequest().setRouteVersionId(500L)));
        assertThrows(ServiceException.class, () -> service.saveBinding(initialRequest().setRouteVersionId(500L)));
        verify(routeProductService, never()).getRouteProductListByRouteId(any(), any());
        verify(candidateConfigService, never()).saveConfigSnapshots(any(), any(), any());
    }

    @Test
    void bind_concurrentDraftChangeDoesNotDeleteFormalProductsOrReplaceProject() {
        stubInitialBindingContext(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));
        when(routeProductMapper.selectListByRouteId(100L)).thenReturn(List.of(routeProduct(1001L)));
        stubDraftProducts(List.of(routeProduct(1002L)));
        doThrow(new IllegalStateException("draft changed")).when(candidateConfigService)
                .saveConfigSnapshots(500L, "draft-sha", Map.of("products", Map.of(), "productBoms", Map.of()));

        assertThrows(IllegalStateException.class, () -> service.saveBinding(initialRequest()
                .setRouteVersionId(500L).setConfirmedProductItemIds(List.of(1001L, 1002L))));

        verify(routeProductMapper, never()).deleteByRouteId(any());
        verify(routeProductBomMapper, never()).deleteByRouteId(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    private void stubDraftProducts(List<MesProRouteProductDO> products) {
        when(routeVersionMapper.selectById(500L)).thenReturn(MesProRouteVersionDO.builder()
                .id(500L).routeId(100L).active(false).lifecycleStatus("DRAFT")
                .routeSnapshotSha256("draft-sha").build());
        when(routeProductService.getRouteProductListByRouteId(100L, 500L)).thenReturn(products);
    }

    private MesRouteDccProjectBindingDO binding(Long id, Long routeId, Long dccProjectCodeId,
                                                Long version, Boolean deleted) {
        MesRouteDccProjectBindingDO binding = MesRouteDccProjectBindingDO.builder()
                .id(id)
                .routeId(routeId)
                .dccProjectCodeId(dccProjectCodeId)
                .version(version)
                .build();
        binding.setDeleted(deleted);
        return binding;
    }

    private DccProjectCodeDO dccProjectCode(Long id, String status) {
        return DccProjectCodeDO.builder()
                .id(id)
                .projectCode("DCC-" + id)
                .projectName("project-" + id)
                .status(status)
                .build();
    }

    private void stubInitialBindingContext(DccProjectCodeDO dccProjectCode) {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(null);
        when(dccProjectCodeMapper.selectById(200L)).thenReturn(dccProjectCode);
    }

    private void stubRouteProducts(Long routeId) {
        when(routeProductMapper.selectListByRouteId(routeId)).thenReturn(List.of(routeProduct(1001L)));
    }

    private MesRouteDccProjectBindingSaveReqVO initialRequest() {
        return new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setExpectedVersion(0L);
    }

    private MesProRouteProductDO routeProduct(Long itemId) {
        return MesProRouteProductDO.builder().routeId(100L).itemId(itemId).build();
    }

}
