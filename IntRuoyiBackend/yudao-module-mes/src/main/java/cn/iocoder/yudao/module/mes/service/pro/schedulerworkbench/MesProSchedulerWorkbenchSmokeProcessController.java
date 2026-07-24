package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MesProSchedulerWorkbenchSmokeProcessController {

    MesProSchedulerWorkbenchSmokeProcess start(List<String> command, File directory, Map<String, String> environment,
                                               File logFile)
            throws IOException;

    void stop(MesProSchedulerWorkbenchSmokeProcess process, boolean windows);

}
