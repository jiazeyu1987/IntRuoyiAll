package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffDeliveryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.Objects;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Independent Spring proxy boundaries preserve delivery attempts after business commit. */
@Service
public class MesActiveOrderHandoffDeliveryTransactionService {
    @Resource private MesActiveOrderHandoffDeliveryMapper mapper;
    @Resource private MesActiveOrderHandoffAudit audit;
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public MesActiveOrderHandoffDeliveryDO recordAttempt(Long id,Integer version,String reason) {
        audit.lock(); var before=processable(id,version);
        require(mapper.attempt(tenant(),id,version,LocalDateTime.now())==1,"通知尝试版本冲突，请刷新");
        var after=mapper.exact(tenant(),id);
        audit.append("delivery-attempt",MesActiveOrderHandoffDeliveryTransactionService.class.getName()+"#recordAttempt","MES_HANDOFF_DELIVERY",id,after.getRowVersion(),before,after,reason);
        return after;
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public void recordSent(Long id,Integer version,Long message,String reason) {
        require(message!=null&&message>0,"站内信正式回执缺失"); audit.lock(); var before=processable(id,version);
        require(mapper.sent(tenant(),id,version,message,LocalDateTime.now())==1,"通知成功回执版本冲突，请刷新");
        var after=mapper.exact(tenant(),id);
        audit.append("delivery-sent",MesActiveOrderHandoffDeliveryTransactionService.class.getName()+"#recordSent","MES_HANDOFF_DELIVERY",id,after.getRowVersion(),before,after,reason);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class)
    public void recordFailed(Long id,Integer version,String error,String reason) {
        require(error!=null&&!error.isBlank()&&error.length()<=512,"通知失败原因无效"); audit.lock(); var before=processable(id,version);
        require(mapper.failed(tenant(),id,version,error,LocalDateTime.now())==1,"通知失败回执版本冲突，请刷新");
        var after=mapper.exact(tenant(),id);
        audit.append("delivery-failed",MesActiveOrderHandoffDeliveryTransactionService.class.getName()+"#recordFailed","MES_HANDOFF_DELIVERY",id,after.getRowVersion(),before,after,reason);
    }
    private MesActiveOrderHandoffDeliveryDO processable(Long id,Integer version) {
        var row=mapper.exact(tenant(),id);
        require(row!=null&&version!=null&&Objects.equals(version,row.getRowVersion())
                &&SetHolder.PROCESSABLE.contains(row.getStatus()),"通知已发送或版本变化，请刷新投递回执");
        return row;
    }
    private static final class SetHolder { static final java.util.Set<String> PROCESSABLE=java.util.Set.of("PENDING","FAILED"); }
}
