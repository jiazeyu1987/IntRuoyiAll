package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.simpl.RAMJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;

/** Restores the single task-approved local registration to RAM; never synchronizes shared jobs or changes DB rows. */
@Component
@Profile("dcc-local-development & !prod & !production & !backup & !test & !unit-test")
@ConditionalOnProperty(prefix="dcc.local-activation-startup-sync", name="enabled", havingValue="true", matchIfMissing=false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DccLocalActivationStartupSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DccLocalActivationStartupSyncRunner.class);
    private static final String PREFIX = "dcc.local-activation-startup-sync";
    private static final String HANDLER = "dccControlledFileActivationJob", CRON = "0 * * * * ?";
    private static final String SCHEDULER = "dcc-main-qms-acceptance", DATABASE = "ruoyi-vue-pro";
    // This opt-in belongs only to the already approved local acceptance source, not another deployment.
    private static final String SOURCE_UUID = "92ca05d0-aec8-11f1-a944-02b4e226a5ef";
    private static final long REGISTERED_JOB_ID = 5625L;
    private final JobMapper jobs;
    private final SchedulerManager manager;
    private final Scheduler scheduler;
    private final DataSource source;
    private final Environment environment;
    private final ApplicationContext context;

    public DccLocalActivationStartupSyncRunner(JobMapper jobs, SchedulerManager manager, Scheduler scheduler,
                                              DataSource source, Environment environment, ApplicationContext context) {
        this.jobs=jobs; this.manager=manager; this.scheduler=scheduler;
        this.source=source; this.environment=environment; this.context=context;
    }

    @Override
    public void run(ApplicationArguments args) throws SchedulerException {
        require(Set.of(environment.getActiveProfiles()).equals(Set.of("dcc-local-development")), "PROFILE");
        require(Boolean.TRUE.equals(environment.getProperty(PREFIX+".enabled",Boolean.class)), "OPT_IN");
        require(Objects.equals(environment.getProperty(PREFIX+".job-id",Long.class),REGISTERED_JOB_ID), "REGISTERED_ID");
        // Missing is unsafe: the existing global startup runner otherwise defaults to enabled.
        require(Boolean.FALSE.equals(environment.getProperty("yudao.local-job-control.startup-sync-enabled",Boolean.class)), "GLOBAL_SYNC");
        require(Boolean.TRUE.equals(environment.getProperty("spring.quartz.auto-startup",Boolean.class)), "AUTO_STARTUP");
        require(ZoneId.of("Asia/Shanghai").equals(ZoneId.systemDefault()), "JVM_TIME_ZONE");
        var metadata=scheduler.getMetaData();
        require(manager.isEnabled() && !metadata.isSchedulerRemote() && metadata.getJobStoreClass()==RAMJobStore.class
                && SCHEDULER.equals(metadata.getSchedulerName()) && !scheduler.isShutdown(), "RAM_SCHEDULER");
        validateSource();
        JobDO job=jobs.selectById(REGISTERED_JOB_ID);
        require(job!=null && Objects.equals(job.getId(),REGISTERED_JOB_ID) && Boolean.FALSE.equals(job.getDeleted())
                && Integer.valueOf(1).equals(job.getStatus()) && HANDLER.equals(job.getHandlerName())
                && CRON.equals(job.getCronExpression()) && job.getRetryCount()!=null && job.getRetryCount()>=0
                && job.getRetryInterval()!=null && job.getRetryInterval()>=0, "REGISTRATION");
        require(job.equals(jobs.selectByHandlerName(HANDLER)), "UNIQUE_REGISTRATION");
        Object handler=context.getBean(HANDLER);
        require(handler instanceof JobHandler && AopUtils.getTargetClass(handler)==DccControlledFileActivationJob.class, "HANDLER_BEAN");

        var jobKey=new JobKey(HANDLER); var triggerKey=new TriggerKey(HANDLER);
        var existingJob=scheduler.getJobDetail(jobKey); var existingTrigger=scheduler.getTrigger(triggerKey);
        if(existingJob!=null || existingTrigger!=null) {
            validateRamObjects(job,existingJob,existingTrigger);
            log.info("[dcc-local-activation][existing registration verified, jobId={}, nextFire={}]",job.getId(),existingTrigger.getNextFireTime());
            return;
        }
        require(scheduler.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty()
                && scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup()).isEmpty(), "OTHER_RAM_OBJECTS");
        manager.addJob(job.getId(),job.getHandlerName(),job.getHandlerParam(),job.getCronExpression(),job.getRetryCount(),job.getRetryInterval());
        var restoredTrigger=scheduler.getTrigger(triggerKey);
        validateRamObjects(job,scheduler.getJobDetail(jobKey),restoredTrigger);
        log.info("[dcc-local-activation][registered job restored to RAM, jobId={}, nextFire={}]",job.getId(),restoredTrigger.getNextFireTime());
    }

    private void validateSource() {
        validateUrl(environment.getProperty("spring.datasource.dynamic.datasource.master.url"));
        try(var connection=source.getConnection()) {
            var metadata=connection.getMetaData();
            validateUrl(metadata.getURL());
            require("MySQL".equals(metadata.getDatabaseProductName()), "DATABASE_PRODUCT");
            try(var statement=connection.createStatement(); var result=statement.executeQuery(
                    "SELECT DATABASE() AS database_name,@@server_uuid AS server_uuid,@@session.time_zone AS time_zone")) {
                require(result.next() && DATABASE.equals(result.getString("database_name"))
                        && SOURCE_UUID.equals(result.getString("server_uuid")) && "+08:00".equals(result.getString("time_zone"))
                        && !result.next(), "ACTUAL_DATABASE_IDENTITY");
            }
        } catch(SQLException error) {
            // Fail explicitly without putting connection credentials or raw JDBC diagnostics in startup output.
            throw invalid("SOURCE_READ_"+error.getClass().getSimpleName());
        }
    }

    private void validateUrl(String url) {
        if(url==null || !url.startsWith("jdbc:mysql://")) throw invalid("LOCAL_JDBC_URL");
        URI location;
        try { location=URI.create(url.substring("jdbc:".length())); }
        catch(IllegalArgumentException error) { throw invalid("LOCAL_JDBC_URL"); }
        require(("127.0.0.1".equals(location.getHost()) || "localhost".equals(location.getHost())) && location.getPort()==23306
                && ("/"+DATABASE).equals(location.getPath()) && location.getUserInfo()==null, "LOCAL_JDBC_URL");
    }

    private void validateRamObjects(JobDO row,JobDetail job,Trigger trigger) throws SchedulerException {
        var jobKey=new JobKey(HANDLER); var triggerKey=new TriggerKey(HANDLER);
        require(job!=null && trigger instanceof CronTrigger, "INCOMPLETE_RAM_OBJECTS");
        require(scheduler.getJobKeys(GroupMatcher.anyJobGroup()).equals(Set.of(jobKey))
                && scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup()).equals(Set.of(triggerKey)), "OTHER_RAM_OBJECTS");
        require(jobKey.equals(job.getKey()) && job.getJobClass()==JobHandlerInvoker.class
                && Objects.equals(job.getJobDataMap().get(JobDataKeyEnum.JOB_ID.name()),row.getId())
                && HANDLER.equals(job.getJobDataMap().get(JobDataKeyEnum.JOB_HANDLER_NAME.name())), "RAM_JOB_IDENTITY");
        var cron=(CronTrigger)trigger; var data=cron.getJobDataMap();
        require(triggerKey.equals(cron.getKey()) && jobKey.equals(cron.getJobKey())
                && CRON.equals(cron.getCronExpression()) && "Asia/Shanghai".equals(cron.getTimeZone().getID())
                && Objects.equals(data.get(JobDataKeyEnum.JOB_HANDLER_PARAM.name()),row.getHandlerParam())
                && Objects.equals(data.get(JobDataKeyEnum.JOB_RETRY_COUNT.name()),row.getRetryCount())
                && Objects.equals(data.get(JobDataKeyEnum.JOB_RETRY_INTERVAL.name()),row.getRetryInterval())
                && cron.getNextFireTime()!=null, "RAM_TRIGGER_IDENTITY");
        var state=scheduler.getTriggerState(triggerKey);
        require(state==Trigger.TriggerState.NORMAL || state==Trigger.TriggerState.BLOCKED, "RAM_TRIGGER_STATE");
    }

    private static void require(boolean condition,String reason) { if(!condition) throw invalid(reason); }
    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("DCC_LOCAL_ACTIVATION_STARTUP_INVALID:"+reason);
    }
}
