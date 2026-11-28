package cn.iocoder.yudao.module.erp.service.sync.runtime;

import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncWatermarkDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeSourceLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpKingdeePagedSyncTransactionTest {

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {}

    private static final LocalDateTime END = LocalDateTime.of(2026, 6, 12, 2, 0);
    private EmbeddedDatabase db;
    private AnnotationConfigApplicationContext beans;
    private JdbcTemplate sql;
    private TransactionTemplate page;
    private ErpKingdeeSyncCancellation cancel;
    private ErpKingdeeSyncRuntimeService runtime;
    private boolean rejectSuccess;

    @BeforeEach
    void open() {
        db = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        sql = new JdbcTemplate(db);
        sql.execute("create table sync_run (id bigint primary key, status int not null)");
        sql.execute("create table page_data (id int primary key)");
        sql.execute("create table watermark (cutoff timestamp not null)");
        var manager = new DataSourceTransactionManager(db);
        page = new TransactionTemplate(manager);
        var runs = mock(ErpKingdeeSyncRunMapper.class);
        var marks = mock(ErpKingdeeSyncWatermarkMapper.class);
        cancel = mock(ErpKingdeeSyncCancellation.class);
        when(runs.insert(any(ErpKingdeeSyncRunDO.class))).thenAnswer(call -> {
            ErpKingdeeSyncRunDO row = call.getArgument(0);
            row.setId(1L);
            return sql.update("insert into sync_run values (?, ?)", row.getId(), row.getStatus());
        });
        when(marks.insert(any(ErpKingdeeSyncWatermarkDO.class))).thenAnswer(call -> {
            ErpKingdeeSyncWatermarkDO row = call.getArgument(0);
            return sql.update("insert into watermark values (?)", row.getLastSuccessTime());
        });
        when(runs.update(any(), any())).thenAnswer(call -> {
            ErpKingdeeSyncRunDO row = call.getArgument(0);
            if (rejectSuccess && row.getStatus() == 20) {
                throw new IllegalStateException("injected finish failure");
            }
            return sql.update("update sync_run set status=? where id=?", row.getStatus(), row.getId());
        });
        beans = new AnnotationConfigApplicationContext();
        beans.register(Transactions.class);
        beans.registerBean(DataSourceTransactionManager.class, () -> manager);
        beans.registerBean(ErpKingdeeSyncRuntimeTransactionService.class,
                () -> new ErpKingdeeSyncRuntimeTransactionService(marks, runs));
        beans.refresh();
        runtime = new ErpKingdeeSyncRuntimeServiceImpl(
                beans.getBean(ErpKingdeeSyncRuntimeTransactionService.class), cancel, mock(ErpKingdeeSourceLock.class));
    }

    @AfterEach
    void close() {
        if (beans != null) beans.close();
        if (db != null) db.shutdown();
    }

    private ErpKingdeeSyncCommand command(boolean paged) {
        var builder = ErpKingdeeSyncCommand.builder().syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST)
                .triggerType(paged ? ErpKingdeeSyncTriggerTypeEnum.FULL : ErpKingdeeSyncTriggerTypeEnum.AUTO)
                .windowEnd(END);
        if (paged) builder.pageTransactions(true);
        return builder.build();
    }

    private void checkTransaction(boolean active) {
        assertEquals(active, TransactionSynchronizationManager.isActualTransactionActive());
        assertEquals(active, TransactionSynchronizationManager.hasResource(db));
    }

    private ErpKingdeeSyncRunResult pages(ErpKingdeeSyncContext context) {
        assertEquals(context.getRunId(), ErpKingdeeOrderSyncContext.getRunId());
        for (int i = 1; i <= 2; i++) {
            checkTransaction(false);
            final int id = i;
            page.executeWithoutResult(s -> {
                checkTransaction(true);
                sql.update("insert into page_data values (?)", id);
            });
        }
        checkTransaction(false);
        return ErpKingdeeSyncRunResult.success(END, 2, 0, 0, 0);
    }

    private void checkStored(int status, int pages, int marks) {
        assertEquals(status, sql.queryForObject("select status from sync_run where id=1", Integer.class));
        assertEquals(pages, sql.queryForObject("select count(*) from page_data", Integer.class));
        assertEquals(marks, sql.queryForObject("select count(*) from watermark", Integer.class));
        assertNull(ErpKingdeeOrderSyncContext.getRunId());
    }

    @Test
    void pagedSuccessCommitsBusinessPagesThenSuccessAndWatermark() {
        runtime.executeSync(command(true), this::pages);
        checkStored(20, 2, 1);
        assertEquals(END, sql.queryForObject("select cutoff from watermark", java.sql.Timestamp.class).toLocalDateTime());
    }

    @Test
    void failedFinalUpdateRollsBackWatermarkButNotPreviouslyCommittedPages() {
        rejectSuccess = true;
        assertEquals("injected finish failure", assertThrows(IllegalStateException.class,
                () -> runtime.executeSync(command(true), this::pages)).getMessage());
        checkStored(30, 2, 0);
    }

    @Test
    void cancellationAfterLastPageDoesNotAdvanceWatermark() {
        runtimeCancellationAfterPages();
        checkStored(30, 2, 0);
    }

    private void runtimeCancellationAfterPages() {
        assertThrows(CancellationException.class, () -> runtime.executeSync(command(true), context -> {
            var result = pages(context);
            doThrow(new CancellationException("stopped")).when(cancel).check(context.getRunId());
            when(cancel.isRequested(context.getRunId())).thenReturn(true);
            return result;
        }));
    }

    @Test
    void defaultModeRollsBackTaskAndWatermarkTogetherIfFinishFails() {
        rejectSuccess = true;
        assertEquals("injected finish failure", assertThrows(IllegalStateException.class,
                () -> runtime.executeSync(command(false), context -> {
                    checkTransaction(true);
                    sql.update("insert into page_data values (1)");
                    return ErpKingdeeSyncRunResult.success(END, 1, 0, 0, 0);
                })).getMessage());
        checkStored(30, 0, 0);
    }
}
