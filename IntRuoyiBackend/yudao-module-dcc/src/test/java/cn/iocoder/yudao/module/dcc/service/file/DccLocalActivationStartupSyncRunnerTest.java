package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileActivationJob;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.infra.service.job.JobStartupSyncRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual Spring profile and Quartz RAM; registry and source-connection reads are explicit isolated ports. */
class DccLocalActivationStartupSyncRunnerTest {
    private static final String HANDLER = "dccControlledFileActivationJob";
    private static final String RUNNER = "cn.iocoder.yudao.module.dcc.service.file.listener.DccLocalActivationStartupSyncRunner";
    private static final ThreadLocal<Fixture> FIXTURE = new ThreadLocal<>();
    private TimeZone originalZone;
    private Fixture fixture;
    private AnnotationConfigApplicationContext context;

    @BeforeEach void setup() throws Exception {
        originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        fixture = new Fixture(); FIXTURE.set(fixture);
    }

    @AfterEach void cleanup() throws Exception {
        try { if (context != null) context.close(); if (fixture != null) fixture.scheduler.shutdown(true); }
        finally { FIXTURE.remove(); TimeZone.setDefault(originalZone); }
    }

    private void open(boolean enabled, String... profiles) {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profiles);
        var properties = new HashMap<String,Object>();
        properties.put("yudao.local-job-control.startup-sync-enabled", fixture.globalSync);
        properties.put("spring.quartz.auto-startup", true);
        properties.put("spring.datasource.dynamic.datasource.master.url", "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro");
        properties.put("dcc.local-activation-startup-sync.job-id", 5625L);
        if (enabled) properties.put("dcc.local-activation-startup-sync.enabled", true);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("task-fixture", properties));
        context.register(Ports.class, JobStartupSyncRunner.class);
        // Test discovery permits a behavioral RED on the old startup with no restorer; no missing class is referenced.
        if (ClassUtils.isPresent(RUNNER, getClass().getClassLoader()))
            context.register(ClassUtils.resolveClassName(RUNNER, getClass().getClassLoader()));
        context.refresh();
    }

    private void run() throws Exception {
        var runners = new ArrayList<>(context.getBeansOfType(ApplicationRunner.class).values());
        AnnotationAwareOrderComparator.sort(runners);
        for (var runner : runners) runner.run(new DefaultApplicationArguments());
    }

    @Test void explicitExistingMinuteJobIsActuallyRestoredIntoTheEmptyRamScheduler() throws Exception {
        var original = new LinkedHashMap<String,Object>();
        original.put("id", fixture.row.getId()); original.put("status", fixture.row.getStatus());
        original.put("handler", fixture.row.getHandlerName()); original.put("cron", fixture.row.getCronExpression());
        open(true, "dcc-local-development"); run();
        assertTrue(fixture.scheduler.checkExists(new JobKey(HANDLER)), "The already registered minute job must survive this RAM restart");
        var job = fixture.scheduler.getJobDetail(new JobKey(HANDLER));
        var trigger = (CronTrigger) fixture.scheduler.getTrigger(new TriggerKey(HANDLER));
        assertEquals(5625L, job.getJobDataMap().getLong(JobDataKeyEnum.JOB_ID.name()));
        assertEquals("0 * * * * ?", trigger.getCronExpression()); assertNotNull(trigger.getNextFireTime());
        assertEquals("Asia/Shanghai", trigger.getTimeZone().getID()); assertTrue(fixture.scheduler.isInStandbyMode());
        assertEquals(1, fixture.scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.anyJobGroup()).size());
        assertEquals(original, Map.of("id", fixture.row.getId(), "status", fixture.row.getStatus(),
                "handler", fixture.row.getHandlerName(), "cron", fixture.row.getCronExpression()));
        verify(fixture.manager, times(1)).addJob(eq(5625L), eq(HANDLER), isNull(), eq("0 * * * * ?"), eq(0), eq(0));
        verify(fixture.mapper, never()).insert(any(JobDO.class)); verify(fixture.mapper, never()).updateById(any(JobDO.class));
        for (Connection connection : fixture.borrowedConnections) verify(connection, never()).setReadOnly(anyBoolean());
        verifyNoInteractions(fixture.jobs);
    }

    @Test void completeIdenticalRamObjectsDoNotGetDuplicatedOrRescheduled() throws Exception {
        open(true, "dcc-local-development"); run();
        java.util.Date nextFire = fixture.scheduler.getTrigger(new TriggerKey(HANDLER)).getNextFireTime(); run();
        assertEquals(nextFire, fixture.scheduler.getTrigger(new TriggerKey(HANDLER)).getNextFireTime());
        verify(fixture.manager, times(1)).addJob(anyLong(), anyString(), any(), anyString(), anyInt(), anyInt());
        verify(fixture.manager, never()).updateJob(anyString(), any(), anyString(), anyInt(), anyInt());
        verify(fixture.manager, never()).deleteJob(anyString());
    }

    @Test void disabledByDefaultDoesNotReadTheRegistryOrRegisterAnything() throws Exception {
        open(false, "dcc-local-development"); run();
        assertFalse(fixture.scheduler.checkExists(new JobKey(HANDLER)));
        verifyNoInteractions(fixture.mapper, fixture.source, fixture.manager, fixture.jobs);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "production", "backup", "test", "unit-test", "local"})
    void nonAcceptanceOrForbiddenProfileCannotLoadTheRestorer(String profile) throws Exception {
        if ("local".equals(profile)) open(true, profile);
        else open(true, "dcc-local-development", profile);
        run();
        assertFalse(fixture.scheduler.checkExists(new JobKey(HANDLER)));
        verifyNoInteractions(fixture.mapper, fixture.source, fixture.manager, fixture.jobs);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STOP", "ID", "HANDLER", "CRON", "UUID", "GLOBAL", "FOREIGN", "PARTIAL"})
    void driftOrAnotherRamJobFailsBeforeAnyRestoration(String change) throws Exception {
        switch (change) {
            case "STOP" -> fixture.row.setStatus(2);
            case "ID" -> fixture.row.setId(5626L);
            case "HANDLER" -> fixture.row.setHandlerName("unapprovedHandler");
            case "CRON" -> fixture.row.setCronExpression("0/10 * * * * ?");
            case "UUID" -> fixture.serverUuid = "different-real-source";
            case "GLOBAL" -> fixture.globalSync = true;
            case "FOREIGN" -> fixture.scheduler.addJob(JobBuilder.newJob(EmptyJob.class).withIdentity("foreign").storeDurably().build(), false);
            case "PARTIAL" -> fixture.scheduler.addJob(JobBuilder.newJob(EmptyJob.class).withIdentity(HANDLER).storeDurably().build(), false);
            default -> throw new AssertionError(change);
        }
        open(true, "dcc-local-development"); assertThrows(Exception.class, this::run);
        verify(fixture.manager, never()).addJob(anyLong(), anyString(), any(), anyString(), anyInt(), anyInt());
        verify(fixture.manager, never()).deleteJob(anyString()); verifyNoInteractions(fixture.jobs);
        assertFalse(fixture.scheduler.checkExists(new TriggerKey(HANDLER)));
    }

    public static class EmptyJob implements Job { public void execute(JobExecutionContext context) { } }

    private static final class Fixture {
        final JobMapper mapper = mock(JobMapper.class);
        final JobService jobs = mock(JobService.class);
        final DataSource source = mock(DataSource.class);
        final Scheduler scheduler;
        final SchedulerManager manager;
        final JobDO row = JobDO.builder().id(5625L).name("Task-owned activation")
                .handlerName(HANDLER).cronExpression("0 * * * * ?").status(1).retryCount(0).retryInterval(0).build();
        boolean globalSync;
        String serverUuid = "92ca05d0-aec8-11f1-a944-02b4e226a5ef";
        final List<Connection> borrowedConnections = new ArrayList<>();

        Fixture() throws Exception {
            row.setDeleted(false);
            var props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName", "dcc-main-qms-acceptance");
            props.setProperty("org.quartz.threadPool.threadCount", "1");
            props.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");
            props.setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
            scheduler = new StdSchedulerFactory(props).getScheduler(); manager = spy(new SchedulerManager(scheduler));
            when(mapper.selectById(5625L)).thenAnswer(call -> row);
            when(mapper.selectByHandlerName(HANDLER)).thenAnswer(call -> row);
            when(source.getConnection()).thenAnswer(call -> connection());
        }

        Connection connection() throws Exception {
            var connection = mock(Connection.class); var metadata = mock(DatabaseMetaData.class);
            borrowedConnections.add(connection);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getURL()).thenReturn("jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro");
            when(metadata.getDatabaseProductName()).thenReturn("MySQL");
            var statement = mock(Statement.class); var result = mock(ResultSet.class);
            when(connection.createStatement()).thenReturn(statement); when(statement.executeQuery(anyString())).thenReturn(result);
            when(result.next()).thenReturn(true, false);
            when(result.getString("database_name")).thenReturn("ruoyi-vue-pro");
            when(result.getString("server_uuid")).thenReturn(serverUuid);
            when(result.getString("time_zone")).thenReturn("+08:00");
            return connection;
        }
    }

    @Configuration(proxyBeanMethods=false)
    static class Ports {
        @Bean JobMapper jobMapper() { return FIXTURE.get().mapper; }
        @Bean JobService jobService() { return FIXTURE.get().jobs; }
        @Bean DataSource dataSource() { return FIXTURE.get().source; }
        @Bean Scheduler scheduler() { return FIXTURE.get().scheduler; }
        @Bean SchedulerManager schedulerManager() { return FIXTURE.get().manager; }
        @Bean(name=HANDLER) DccControlledFileActivationJob activationJob() {
            return new DccControlledFileActivationJob(mock(DccControlledFileLifecycleService.class));
        }
    }
}
