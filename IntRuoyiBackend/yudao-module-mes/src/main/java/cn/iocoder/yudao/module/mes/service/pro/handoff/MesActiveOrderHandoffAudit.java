package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffDeliveryDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MesActiveOrderHandoffAudit {
    @Resource private GxpAuditService audit;
    public void lock() {
        MesActiveOrderHandoffContract.require(TransactionSynchronizationManager.isActualTransactionActive(),"交接状态与审计必须处于正式事务");
        audit.acquireLedgerLock();
    }
    public void append(String operation,String source,String subject,Long id,Integer version,Object before,Object after,String reason) {
        MesActiveOrderHandoffContract.require(after!=null&&Objects.equals(version,rowVersion(after)),"交接审计后态版本与正式对象不一致");
        audit.append(GxpAuditCommand.builder().operationId("mes.active-order-handoff."+operation)
                .subjectId(subject+":"+id).subjectVersion(String.valueOf(version))
                .beforeState(envelope(before)).afterState(envelope(after))
                .reason(reason).reasonSource("SYSTEM").reasonCode("ACTIVE_ORDER_FORMAL_HANDOFF")
                .resultStatus("SUCCESS").idempotencyKey("MES_HANDOFF:"+subject+":"+id+":"+operation+":"+version)
                .sourceType("SERVICE_METHOD").sourceLocator(source)
                .links(List.of(new GxpAuditRelation("SUBJECT",subject,String.valueOf(id),String.valueOf(version),null))).build());
    }
    private GxpAuditStateEnvelope envelope(Object value) {
        return GxpAuditStateEnvelope.builder().state(value==null?"ABSENT":"PRESENT")
                .objectVersion(value==null?null:String.valueOf(rowVersion(value)))
                .canonicalJson(JsonUtils.toJsonString(value==null?Map.of():value)).build();
    }
    private Integer rowVersion(Object value) {
        Integer version;
        if(value instanceof MesActiveOrderHandoffTaskDO task)version=task.getRowVersion();
        else if(value instanceof MesActiveOrderHandoffDeliveryDO delivery)version=delivery.getRowVersion();
        else throw MesActiveOrderHandoffContract.failure("交接审计对象类型无效");
        MesActiveOrderHandoffContract.require(version!=null&&version>=0,"交接审计对象版本缺失");
        return version;
    }
}
