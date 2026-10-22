package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesActiveOrderHandoffAuditTest {
    private MesActiveOrderHandoffAudit helper;
    private GxpAuditService ledger;
    @BeforeEach void fixture(){
        helper=new MesActiveOrderHandoffAudit();ledger=mock(GxpAuditService.class);
        ReflectionTestUtils.setField(helper,"audit",ledger);
    }
    @Test void actualHelperCapturesAbsentCreationAndPresentTaskVersion(){
        var after=new MesActiveOrderHandoffTaskDO().setId(10L).setRowVersion(0).setStatus("TODO");
        helper.append("task-created",MesActiveOrderHandoffService.class.getName()+"#create","MES_HANDOFF_TASK",10L,0,null,after,"正式创建");
        var command=capture();assertEquals("ABSENT",command.getBeforeState().getState());
        assertNull(command.getBeforeState().getObjectVersion());assertEquals("0",command.getAfterState().getObjectVersion());
        assertEquals("0",command.getSubjectVersion());assertEquals("TODO",JsonUtils.parseTree(command.getAfterState().getCanonicalJson()).path("status").asText());
    }
    @Test void actualHelperPreservesTaskCloseBeforeAndAfterVersions(){
        var before=new MesActiveOrderHandoffTaskDO().setId(10L).setRowVersion(4).setStatus("TODO");
        var after=new MesActiveOrderHandoffTaskDO().setId(10L).setRowVersion(5).setStatus("DONE");
        helper.append("task-closed",MesActiveOrderHandoffService.class.getName()+"#close","MES_HANDOFF_TASK",10L,5,before,after,"正式关闭");
        var command=capture();assertEquals("4",command.getBeforeState().getObjectVersion());assertEquals("5",command.getAfterState().getObjectVersion());
        assertEquals(4,JsonUtils.parseTree(command.getBeforeState().getCanonicalJson()).path("rowVersion").asInt());
        assertEquals(5,JsonUtils.parseTree(command.getAfterState().getCanonicalJson()).path("rowVersion").asInt());
        assertEquals("5",command.getSubjectVersion());
    }
    @Test void actualHelperPreservesAttemptSentAndFailedDeliveryVersions(){
        for(String operation:java.util.List.of("delivery-attempt","delivery-sent","delivery-failed")){
            reset(ledger);var before=new MesActiveOrderHandoffDeliveryDO().setId(11L).setRowVersion(6).setStatus("PENDING");
            var after=new MesActiveOrderHandoffDeliveryDO().setId(11L).setRowVersion(7).setStatus(operation.equals("delivery-sent")?"SENT":"FAILED");
            helper.append(operation,MesActiveOrderHandoffDeliveryTransactionService.class.getName()+"#record"+Character.toUpperCase(operation.charAt(9))+operation.substring(10),"MES_HANDOFF_DELIVERY",11L,7,before,after,"正式投递");
            var command=capture();assertEquals("6",command.getBeforeState().getObjectVersion());assertEquals("7",command.getAfterState().getObjectVersion());
            assertEquals("7",command.getSubjectVersion());assertEquals(6,JsonUtils.parseTree(command.getBeforeState().getCanonicalJson()).path("rowVersion").asInt());
        }
    }
    @Test void mismatchedMissingOrUnrecognizedObjectVersionStopsBeforeLedgerWrite(){
        assertThrows(RuntimeException.class,()->helper.append("task-closed","x","MES_HANDOFF_TASK",10L,5,null,new MesActiveOrderHandoffTaskDO().setRowVersion(4),"原因"));
        assertThrows(RuntimeException.class,()->helper.append("task-closed","x","MES_HANDOFF_TASK",10L,5,new MesActiveOrderHandoffTaskDO(),new MesActiveOrderHandoffTaskDO().setRowVersion(5),"原因"));
        assertThrows(RuntimeException.class,()->helper.append("task-closed","x","MES_HANDOFF_TASK",10L,5,null,new Object(),"原因"));
        verifyNoInteractions(ledger);
    }
    @Test void registeredTaskCreateLocatorHasExactlyOneRealWriter(){
        assertEquals(1,Arrays.stream(MesActiveOrderHandoffService.class.getDeclaredMethods()).filter(method->method.getName().equals("create")).count());
        assertEquals(1,Arrays.stream(MesActiveOrderHandoffService.class.getDeclaredMethods()).filter(method->method.getName().equals("createDefaultRound")).count());
    }
    private GxpAuditCommand capture(){var captured=ArgumentCaptor.forClass(GxpAuditCommand.class);verify(ledger).append(captured.capture());return captured.getValue();}
}
