package cn.iocoder.yudao.module.signature.service;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ElectronicSignatureV2ProtocolTest {
    private AuthenticatedSignatureRequest request(String name,String image,String prepare) {
        var command=new ElectronicSignatureCommand("BPM","APPROVE","BPM_APPROVAL_TASK","EDHR_RELEASE:7","EDHR1:"+"a".repeat(64),null,"同意","AC:ce75d9a0-9194-4bcb-a26e-ef302b9bc956:CENTER",null,null);
        return new AuthenticatedSignatureRequest(command,new SignatureSubjectSnapshot(command.subjectType(),command.subjectId(),command.expectedSubjectVersion(),"{}",null,null,null,null,"3",null,null),new SignatureActionDefinition("BPM","APPROVE","BPM_APPROVAL_TASK","APPROVE","审批确认","approval-content-v1"),JsonUtils.parseTree("{\"prepareId\":\""+prepare+"\"}"),name,JsonUtils.parseTree(image));
    }
    @Test void stableRequestExcludesDisplayImageAndCredentialVersionButBindsPreparedIdentity() {
        var signer=new AuthenticatedSigner(1L,"SYSTEM_USER",2L,2L,"ce75d9a0-9194-4bcb-a26e-ef302b9bc956",1L,"request");
        var first=ElectronicSignatureV2Protocol.requestHash(request("首次姓名","{\"state\":\"NOT_REQUIRED\",\"snapshot\":null}","p1"),signer);
        assertEquals(first,ElectronicSignatureV2Protocol.requestHash(request("后来姓名","{\"state\":\"CAPTURED\",\"snapshot\":{}}","p1"),new AuthenticatedSigner(1L,"SYSTEM_USER",2L,2L,signer.credentialGeneration(),2L,"request")));
        assertNotEquals(first,ElectronicSignatureV2Protocol.requestHash(request("首次姓名","{}","p2"),signer));
    }
}
