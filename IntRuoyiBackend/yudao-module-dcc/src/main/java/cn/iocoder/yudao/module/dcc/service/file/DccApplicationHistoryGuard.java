package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;

/** Validates actual BPM history behind the persisted application binding, including failed obsolete rounds. */
@Service
public class DccApplicationHistoryGuard {
    @Resource private BpmProcessInstanceService processes;
    public void require(DccControlledFileDO file,String type,String round) {
        var history=processes.getHistoricProcessInstance(round);
        String key="UPLOAD".equals(type)?DccControlledFileProcessDefinitionKeys.UPLOAD
                :"REVISION".equals(type)?DccControlledFileProcessDefinitionKeys.REVISION
                :"OBSOLETE".equals(type)?DccControlledFileProcessDefinitionKeys.OBSOLETE:null;
        if(key==null || history==null || !Objects.equals(round,history.getId())
                || !Objects.equals(file.getTenantId().toString(),history.getTenantId())
                || !Objects.equals(key,history.getProcessDefinitionKey()) || history.getStartUserId()==null)
            throw new IllegalStateException("申请轮次与真实BPM历史身份不一致");
        if("OBSOLETE".equals(type)) {
            var variables=history.getProcessVariables();
            if(variables==null || !"DCC".equals(variables.get("systemCode"))
                    || !"CONTROLLED_FILE".equals(variables.get("objectType")) || !"OBSOLETE".equals(variables.get("actionCode"))
                    || !file.getId().toString().equals(String.valueOf(variables.get("objectId")))
                    || !Objects.equals(file.getVersionNo(),variables.get("objectVersion")))
                throw new IllegalStateException("作废轮次与真实BPM对象版本不一致");
        } else if(!Objects.equals(file.getId().toString(),history.getBusinessKey())
                || !Objects.equals(String.valueOf(file.getRequesterId()),history.getStartUserId()))
            throw new IllegalStateException("内容申请轮次与真实BPM业务对象或申请人不一致");
    }
}
