package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.projection.MesProBatchRecordExecutionFieldResponsibilityAuditProjection;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import jakarta.annotation.Resource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(MesProBatchRecordExecutionFieldResponsibilityMapperTest.TenantInterceptorTestConfiguration.class)
class MesProBatchRecordExecutionFieldResponsibilityMapperTest extends BaseDbUnitTest {

    private static final Set<String> PROJECTION_FIELDS = Set.of(
            "auditItemId", "auditBatchId", "executionId", "tenantId", "fieldAuditRevision",
            "batchItemIndex", "fieldPath", "fieldKey", "fieldLabel", "rowIndex", "columnIndex",
            "component", "valueType", "oldValueJson", "oldValueDisplay", "oldValueHash",
            "newValueJson", "newValueDisplay", "newValueHash", "reasonCategory", "reasonText",
            "actorId", "actorName", "signatureId", "signatureProjectionHash", "previousHash",
            "auditHash", "beforeCellValuesHash", "afterCellValuesHash", "executionSnapshotHash",
            "changedAt");

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private DataSource dataSource;
    @Resource
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @BeforeAll
    static void initializeMyBatisMetadata() {
        initializeTableInfo(MesProBatchRecordExecutionDO.class);
        initializeTableInfo(MesProBatchRecordExecutionFieldAuditItemDO.class);
        initializeTableInfo(MesProBatchRecordExecutionSignatureDO.class);
        initializeTableInfo(MesProEdhrWorkTaskDO.class);
    }

    @Test
    void projectionFromCopiesCompleteAuditIdentityAndHashes() {
        LocalDateTime changedAt = LocalDateTime.of(2026, 7, 10, 14, 30);
        MesProBatchRecordExecutionFieldAuditItemDO item = new MesProBatchRecordExecutionFieldAuditItemDO()
                .setId(101L)
                .setAuditBatchId(201L)
                .setExecutionId(301L)
                .setTenantId(122L)
                .setFieldAuditRevision(9L)
                .setBatchItemIndex(2)
                .setFieldPath("sheet[0].rows[1].cells[2].temperature")
                .setFieldKey("temperature")
                .setFieldLabel("温度")
                .setRowIndex(1)
                .setColumnIndex(2)
                .setComponent("InputNumber")
                .setValueType("NUMBER")
                .setOldValueJson("20")
                .setOldValueDisplay("20")
                .setOldValueHash("old-hash")
                .setNewValueJson("21")
                .setNewValueDisplay("21")
                .setNewValueHash("new-hash")
                .setReasonCategory("CORRECTION")
                .setReasonText("复核修正")
                .setActorId(401L)
                .setActorName("操作员甲")
                .setSignatureId(501L)
                .setSignatureProjectionHash("signature-projection-hash")
                .setPreviousHash("previous-hash")
                .setAuditHash("audit-hash")
                .setBeforeCellValuesHash("before-values-hash")
                .setAfterCellValuesHash("after-values-hash")
                .setExecutionSnapshotHash("snapshot-hash")
                .setChangedAt(changedAt);

        MesProBatchRecordExecutionFieldResponsibilityAuditProjection projection =
                MesProBatchRecordExecutionFieldResponsibilityAuditProjection.from(item);

        assertEquals(PROJECTION_FIELDS, Arrays.stream(
                        MesProBatchRecordExecutionFieldResponsibilityAuditProjection.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet()));
        assertEquals(item.getId(), projection.getAuditItemId());
        assertEquals(item.getAuditBatchId(), projection.getAuditBatchId());
        assertEquals(item.getExecutionId(), projection.getExecutionId());
        assertEquals(item.getTenantId(), projection.getTenantId());
        assertEquals(item.getFieldAuditRevision(), projection.getFieldAuditRevision());
        assertEquals(item.getBatchItemIndex(), projection.getBatchItemIndex());
        assertEquals(item.getFieldPath(), projection.getFieldPath());
        assertEquals(item.getFieldKey(), projection.getFieldKey());
        assertEquals(item.getFieldLabel(), projection.getFieldLabel());
        assertEquals(item.getRowIndex(), projection.getRowIndex());
        assertEquals(item.getColumnIndex(), projection.getColumnIndex());
        assertEquals(item.getComponent(), projection.getComponent());
        assertEquals(item.getValueType(), projection.getValueType());
        assertEquals(item.getOldValueJson(), projection.getOldValueJson());
        assertEquals(item.getOldValueDisplay(), projection.getOldValueDisplay());
        assertEquals(item.getOldValueHash(), projection.getOldValueHash());
        assertEquals(item.getNewValueJson(), projection.getNewValueJson());
        assertEquals(item.getNewValueDisplay(), projection.getNewValueDisplay());
        assertEquals(item.getNewValueHash(), projection.getNewValueHash());
        assertEquals(item.getReasonCategory(), projection.getReasonCategory());
        assertEquals(item.getReasonText(), projection.getReasonText());
        assertEquals(item.getActorId(), projection.getActorId());
        assertEquals(item.getActorName(), projection.getActorName());
        assertEquals(item.getSignatureId(), projection.getSignatureId());
        assertEquals(item.getSignatureProjectionHash(), projection.getSignatureProjectionHash());
        assertEquals(item.getPreviousHash(), projection.getPreviousHash());
        assertEquals(item.getAuditHash(), projection.getAuditHash());
        assertEquals(item.getBeforeCellValuesHash(), projection.getBeforeCellValuesHash());
        assertEquals(item.getAfterCellValuesHash(), projection.getAfterCellValuesHash());
        assertEquals(item.getExecutionSnapshotHash(), projection.getExecutionSnapshotHash());
        assertEquals(changedAt, projection.getChangedAt());
    }

    @Test
    void projectionListUsesOneExecutionQueryAndStableAscendingOrder() {
        MesProBatchRecordExecutionFieldAuditItemMapper mapper =
                mock(MesProBatchRecordExecutionFieldAuditItemMapper.class, CALLS_REAL_METHODS);
        MesProBatchRecordExecutionFieldAuditItemDO item =
                new MesProBatchRecordExecutionFieldAuditItemDO().setId(11L).setExecutionId(31L);
        doReturn(List.of(item)).when(mapper).selectList(any(Wrapper.class));

        List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> result =
                mapper.selectResponsibilityProjectionList(31L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getAuditItemId());
        LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO> wrapper =
                (LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>) captureItemWrapper(mapper);
        assertInstanceOf(LambdaQueryWrapperX.class, wrapper);
        assertSqlContainsInOrder(wrapper, "execution_id =", "ORDER BY field_audit_revision ASC,id ASC");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(31L));
        verify(mapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void historyPageUsesCompleteIdentityCompositeCursorDescendingOrderAndPageSizePlusOne() {
        MesProBatchRecordExecutionFieldAuditItemMapper mapper =
                mock(MesProBatchRecordExecutionFieldAuditItemMapper.class, CALLS_REAL_METHODS);
        doReturn(List.of()).when(mapper).selectList(any(Wrapper.class));

        mapper.selectResponsibilityHistoryProjectionPage(
                31L, "sheet[0].rows[1].cells[2].temperature", "temperature", 1, 2,
                9L, 101L, 51);

        LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO> wrapper =
                (LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditItemDO>) captureItemWrapper(mapper);
        assertInstanceOf(LambdaQueryWrapperX.class, wrapper);
        assertSqlContainsInOrder(wrapper,
                "execution_id =",
                "field_path =",
                "field_key =",
                "row_index =",
                "column_index =",
                "(field_audit_revision <",
                "OR (field_audit_revision =",
                "AND id <",
                "ORDER BY field_audit_revision DESC,id DESC",
                "LIMIT 51");
        Collection<Object> values = wrapper.getParamNameValuePairs().values();
        assertTrue(values.containsAll(List.of(
                31L, "sheet[0].rows[1].cells[2].temperature", "temperature", 1, 2, 9L, 101L)));
        verify(mapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void signatureIdsUseOneOrderedQueryAndEmptyIdsSkipDatabase() {
        MesProBatchRecordExecutionSignatureMapper mapper =
                mock(MesProBatchRecordExecutionSignatureMapper.class, CALLS_REAL_METHODS);

        assertEquals(List.of(), mapper.selectResponsibilityListByIds(List.of()));
        verify(mapper, never()).selectList(any(Wrapper.class));

        MesProBatchRecordExecutionSignatureDO first =
                new MesProBatchRecordExecutionSignatureDO().setId(501L);
        MesProBatchRecordExecutionSignatureDO second =
                new MesProBatchRecordExecutionSignatureDO().setId(502L);
        List<MesProBatchRecordExecutionSignatureDO> selected = List.of(first, second);
        doReturn(selected).when(mapper).selectList(any(Wrapper.class));

        List<MesProBatchRecordExecutionSignatureDO> result =
                mapper.selectResponsibilityListByIds(List.of(502L, 501L));

        assertSame(selected, result);
        LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO> wrapper =
                (LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>) captureSignatureWrapper(mapper);
        assertInstanceOf(LambdaQueryWrapperX.class, wrapper);
        assertSqlContainsInOrder(wrapper, "id IN", "ORDER BY id ASC");
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of(501L, 502L)));
        verify(mapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void tenantScopedResponsibilityQueriesDoNotRevealOtherTenantRows() {
        List<InnerInterceptor> tenantInterceptors = mybatisPlusInterceptor.getInterceptors().stream()
                .filter(TenantLineInnerInterceptor.class::isInstance)
                .toList();
        assertEquals(1, tenantInterceptors.size());
        assertInstanceOf(TenantLineInnerInterceptor.class, tenantInterceptors.getFirst());
        assertTenantTableMetadata("mes_pro_batch_record_execution", MesProBatchRecordExecutionDO.class);
        assertTenantTableMetadata("mes_pro_batch_record_execution_signature",
                MesProBatchRecordExecutionSignatureDO.class);
        assertTenantTableMetadata("mes_pro_edhr_work_task", MesProEdhrWorkTaskDO.class);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_batch_record_execution
                    (id, execution_code, work_order_id, work_order_code, batch_code, sheet_layout_json, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 1001L, "EXEC-T122", 11L, "WO-11", "BATCH-11", "{}", 122L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_batch_record_execution
                    (id, execution_code, work_order_id, work_order_code, batch_code, sheet_layout_json, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 2001L, "EXEC-T223", 22L, "WO-22", "BATCH-22", "{}", 223L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_batch_record_execution_signature
                    (id, execution_id, actor_id, action_type, signature_mode, password_verified, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 501L, 1001L, 31L, "FIELD_CHANGE", "PASSWORD", true, 122L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_batch_record_execution_signature
                    (id, execution_id, actor_id, action_type, signature_mode, password_verified, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 502L, 1001L, 32L, "FIELD_CHANGE", "PASSWORD", true, 223L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_batch_record_execution_signature
                    (id, execution_id, actor_id, action_type, signature_mode, password_verified, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 503L, 2001L, 33L, "FIELD_CHANGE", "PASSWORD", true, 223L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_edhr_work_task
                    (id, task_code, task_type, batch_execution_id, business_scope_id, execution_id,
                     assignee_user_id, status, action_url, signature_cell_key, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 701L, "TASK-T122", "FIELD_REVIEW", 41L, 41L, 1001L,
                51L, "TODO", "/task/701", "field", 122L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_edhr_work_task
                    (id, task_code, task_type, batch_execution_id, business_scope_id, execution_id,
                     assignee_user_id, status, action_url, signature_cell_key, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 702L, "TASK-T223-SAME-EXEC", "FIELD_REVIEW", 42L, 42L, 1001L,
                52L, "TODO", "/task/702", "field", 223L);
        jdbcTemplate.update("""
                INSERT INTO mes_pro_edhr_work_task
                    (id, task_code, task_type, batch_execution_id, business_scope_id, execution_id,
                     assignee_user_id, status, action_url, signature_cell_key, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 703L, "TASK-T223", "FIELD_REVIEW", 43L, 43L, 2001L,
                53L, "TODO", "/task/703", "field", 223L);
        TenantContextHolder.setTenantId(122L);
        TenantContextHolder.setIgnore(false);

        assertNotNull(executionMapper.selectById(1001L));
        assertNull(executionMapper.selectById(2001L));
        assertNull(executionMapper.selectById(2999L));
        assertEquals(List.of(501L), signatureMapper.selectResponsibilityListByIds(List.of(501L, 502L, 599L))
                .stream().map(MesProBatchRecordExecutionSignatureDO::getId).toList());
        assertEquals(List.of(), signatureMapper.selectResponsibilityListByIds(List.of(503L)));
        assertEquals(List.of(), signatureMapper.selectResponsibilityListByIds(List.of(599L)));
        assertEquals(List.of(701L), workTaskMapper.selectTimelineListByExecutionId(1001L)
                .stream().map(MesProEdhrWorkTaskDO::getId).toList());
        assertEquals(List.of(), workTaskMapper.selectTimelineListByExecutionId(2001L));
        assertEquals(List.of(), workTaskMapper.selectTimelineListByExecutionId(2999L));
    }

    @Test
    void responsibilityMapperMethodsRemainJavaDefaults() throws Exception {
        Method projectionList = MesProBatchRecordExecutionFieldAuditItemMapper.class.getMethod(
                "selectResponsibilityProjectionList", Long.class);
        Method historyPage = MesProBatchRecordExecutionFieldAuditItemMapper.class.getMethod(
                "selectResponsibilityHistoryProjectionPage",
                Long.class, String.class, String.class, Integer.class, Integer.class,
                Long.class, Long.class, int.class);
        Method signatures = MesProBatchRecordExecutionSignatureMapper.class.getMethod(
                "selectResponsibilityListByIds", Collection.class);

        assertTrue(projectionList.isDefault());
        assertTrue(historyPage.isDefault());
        assertTrue(signatures.isDefault());
    }

    private static void initializeTableInfo(Class<?> entityType) {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), entityType.getName()), entityType);
    }

    private static void assertTenantTableMetadata(String tableName, Class<?> entityType) {
        assertNotNull(TableInfoHelper.getTableInfo(tableName));
        assertSame(entityType, TableInfoHelper.getTableInfo(tableName).getEntityType());
        TenantDatabaseInterceptor interceptor = new TenantDatabaseInterceptor(new TenantProperties());
        assertFalse(interceptor.ignoreTable(tableName));
        assertFalse(interceptor.ignoreTable(tableName.toUpperCase()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Wrapper<MesProBatchRecordExecutionFieldAuditItemDO> captureItemWrapper(
            MesProBatchRecordExecutionFieldAuditItemMapper mapper) {
        ArgumentCaptor<Wrapper<MesProBatchRecordExecutionFieldAuditItemDO>> captor =
                ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Wrapper<MesProBatchRecordExecutionSignatureDO> captureSignatureWrapper(
            MesProBatchRecordExecutionSignatureMapper mapper) {
        ArgumentCaptor<Wrapper<MesProBatchRecordExecutionSignatureDO>> captor =
                ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(mapper).selectList(captor.capture());
        return captor.getValue();
    }

    private static void assertSqlContainsInOrder(Wrapper<?> wrapper, String... fragments) {
        String sql = wrapper.getSqlSegment().replace("`", "").replaceAll("\\s+", " ").trim();
        int offset = 0;
        for (String fragment : fragments) {
            int index = sql.indexOf(fragment, offset);
            assertTrue(index >= offset, () -> "Expected SQL fragment in order: " + fragment + " within " + sql);
            offset = index + fragment.length();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantInterceptorTestConfiguration {

        @Bean
        static BeanPostProcessor tenantInterceptorInstaller() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (!(bean instanceof MybatisPlusInterceptor interceptor)) {
                        return bean;
                    }
                    long installed = interceptor.getInterceptors().stream()
                            .filter(TenantLineInnerInterceptor.class::isInstance)
                            .count();
                    if (installed > 1) {
                        throw new IllegalStateException("TenantLineInnerInterceptor installed more than once");
                    }
                    if (installed == 0) {
                        List<InnerInterceptor> interceptors = new java.util.ArrayList<>(
                                interceptor.getInterceptors());
                        interceptors.add(0, new TenantLineInnerInterceptor(
                                new TenantDatabaseInterceptor(new TenantProperties())));
                        interceptor.setInterceptors(interceptors);
                    }
                    return bean;
                }
            };
        }
    }

}
