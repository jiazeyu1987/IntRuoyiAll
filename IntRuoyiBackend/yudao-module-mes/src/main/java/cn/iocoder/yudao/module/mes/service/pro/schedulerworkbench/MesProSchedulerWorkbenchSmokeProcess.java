package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

public interface MesProSchedulerWorkbenchSmokeProcess {

    long pid();

    boolean isAlive();

    Integer exitCode();

    void stop();

}
