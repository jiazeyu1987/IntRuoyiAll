package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class SimpleModelParticipantEndEventTest {
    @Test
    void reviewAndApprovalReachEndEvent() { verifyModel(true); }

    @Test
    void reviewWithoutOptionalApprovalReachesEndEvent() { verifyModel(false); }

    private void verifyModel(boolean withApproval) {
        BpmSimpleModelNodeVO end = new BpmSimpleModelNodeVO().setId("EndEvent").setType(1).setName("结束");
        BpmSimpleModelNodeVO reviewer = approval("Reviewer", "审核");
        BpmSimpleModelNodeVO approver = approval("Approver", "批准");
        reviewer.setChildNode(withApproval ? approver : end);
        if (withApproval) approver.setChildNode(end);
        BpmSimpleModelNodeVO root = new BpmSimpleModelNodeVO().setId("StartUserNode").setType(10)
                .setName("发起人").setFieldsPermission(Collections.emptyList())
                .setButtonsSetting(Collections.emptyList()).setChildNode(reviewer);
        BpmnModel model = SimpleModelUtils.buildBpmnModel("approval_model_regression", "主管流程", root);
        org.flowable.bpmn.model.Process process = model.getMainProcess();
        assertEquals(1, process.findFlowElementsOfType(StartEvent.class).size());
        assertEquals(1, process.findFlowElementsOfType(EndEvent.class).size());
        assertEquals(withApproval ? 3 : 2, process.findFlowElementsOfType(UserTask.class).size());
        assertInstanceOf(UserTask.class, process.getFlowElement("Reviewer"));
        assertConnected(process, process.findFlowElementsOfType(StartEvent.class).get(0).getId(), "StartUserNode");
        assertConnected(process, "StartUserNode", "Reviewer");
        assertConnected(process, "Reviewer", withApproval ? "Approver" : "EndEvent");
        if (withApproval) {
            assertInstanceOf(UserTask.class, process.getFlowElement("Approver"));
            assertConnected(process, "Approver", "EndEvent");
        }
        assertTrue(((EndEvent) process.getFlowElement("EndEvent")).getOutgoingFlows().isEmpty());
        assertTrue(BpmnModelUtils.getBpmnXml(model).contains("endEvent"));
    }

    private BpmSimpleModelNodeVO approval(String id, String name) {
        return new BpmSimpleModelNodeVO().setId(id).setType(11).setName(name)
                .setApproveType(1).setCandidateStrategy(30).setCandidateParam("1001")
                .setApproveMethod(3).setApproveRatio(100)
                .setRejectHandler(new BpmSimpleModelNodeVO.RejectHandler().setType(1))
                .setTimeoutHandler(new BpmSimpleModelNodeVO.TimeoutHandler().setEnable(false))
                .setAssignEmptyHandler(new BpmSimpleModelNodeVO.AssignEmptyHandler().setType(2))
                .setAssignStartUserHandlerType(1).setFieldsPermission(Collections.emptyList())
                .setButtonsSetting(Collections.emptyList()).setSignEnable(false).setReasonRequire(false);
    }

    private void assertConnected(org.flowable.bpmn.model.Process process, String source, String target) {
        assertTrue(process.findFlowElementsOfType(SequenceFlow.class).stream()
                .anyMatch(flow -> source.equals(flow.getSourceRef()) && target.equals(flow.getTargetRef())),
                source + " must connect to " + target);
    }
}
