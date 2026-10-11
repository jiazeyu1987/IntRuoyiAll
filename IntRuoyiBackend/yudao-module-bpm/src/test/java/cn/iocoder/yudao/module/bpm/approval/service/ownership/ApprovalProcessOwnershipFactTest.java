package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.module.bpm.api.task.dto.ApprovalProcessAuthorityDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalProcessOwnershipFactTest {
    @Test void refusesDefaultOwnerWrongWriterMissingSourceAndTamperedTenant() {
        assertThrows(IllegalStateException.class,()->ApprovalProcessOwnershipRegistry.require(null));
        assertThrows(IllegalStateException.class,()->ApprovalProcessOwnershipRegistry.require(new ApprovalProcessAuthorityDTO(
                1L,"NATIVE_BPM","BPM_START","42","v1","FORM_ACTION_SUBMIT",null)));
        assertThrows(IllegalStateException.class,()->ApprovalProcessOwnershipService.verify(null));
        var row=fact();assertDoesNotThrow(()->ApprovalProcessOwnershipService.verify(row));
        row.setTenantId(2L);assertThrows(IllegalStateException.class,()->ApprovalProcessOwnershipService.verify(row));
    }
    @Test void rejectsSourceAuthorityCapacityInsteadOfTruncation() {
        assertThrows(IllegalArgumentException.class,()->ApprovalProcessOwnershipRegistry.require(new ApprovalProcessAuthorityDTO(
                1L,"NATIVE_BPM","BPM_START","x".repeat(65),"v1","BPM_MANUAL_START",null)));
    }
    private static BpmApprovalProcessOwnershipDO fact() {
        var row=new BpmApprovalProcessOwnershipDO();row.setId(1L);row.setTenantId(1L);row.setProcessInstanceId("process");
        row.setProcessDefinitionId("definition:1");row.setProcessDefinitionKey("definition");row.setProjectionCode("NATIVE_BPM");
        row.setAuthorityType("BPM_START");row.setAuthorityId("start-request");row.setAuthorityVersion("version");
        row.setWriterCode("BPM_MANUAL_START");row.setCreatedAt(LocalDateTime.of(2026,10,10,12,0));
        row.setEvidenceDigest(ApprovalProcessOwnershipService.digest(row));return row;
    }
}
