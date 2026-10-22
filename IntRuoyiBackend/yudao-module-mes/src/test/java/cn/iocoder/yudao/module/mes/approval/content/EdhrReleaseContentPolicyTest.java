package cn.iocoder.yudao.module.mes.approval.content;
import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EdhrReleaseContentPolicyTest {
    @Test void absentFormalRootsCannotBeUsedAsApprovalEvidence() {
        var policy=new EdhrReleaseContentPolicy();
        assertThrows(IllegalArgumentException.class,()->policy.validate(ApprovalContentJson.parse("{}")));
        assertThrows(IllegalArgumentException.class,()->policy.validate(ApprovalContentJson.parse("{\"contentPolicyVersion\":\"approval-content-v1\",\"scopeCode\":\"EDHR_RELEASE\",\"identity\":{},\"workTask\":{},\"release\":{},\"finalization\":{}}")));
    }
    @Test void aDifferentPolicyCannotSilentlyUseTheAcceptedSchema() {
        assertThrows(IllegalArgumentException.class,()->new EdhrReleaseContentPolicy().validate(ApprovalContentJson.parse("{\"contentPolicyVersion\":\"future-policy\",\"scopeCode\":\"EDHR_RELEASE\"}")));
    }
}
