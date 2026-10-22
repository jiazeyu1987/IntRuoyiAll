package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class DccControlledFileBodyReadContextTest {
    @Test void canonicalBytesMatchFrontendTransportAndPurposeFitsExistingStorage() {
        var context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                "9223372036854775000", "A/1-2", "REVISION", "round-exact", "task-exact");
        String json = "{\"intent\":\"APPROVAL_TASK\",\"controlledFileId\":\"9223372036854775000\",\"expectedVersionNo\":\"A/1-2\",\"applicationType\":\"REVISION\",\"bpmRound\":\"round-exact\",\"taskId\":\"task-exact\"}";
        assertEquals(json, context.canonicalJson());
        assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8)), context.wire());
        assertEquals(context, DccControlledFileBodyReadContext.fromWire(9223372036854775000L, context.wire()));
        assertEquals(46, context.purpose().length());
        assertTrue(DccControlledFileBodyReadContext.isBodyPurpose(context.purpose()));
        assertNotEquals(context.purpose(), DccControlledFileBodyReadContext.daily(9223372036854775000L).purpose());
    }

    @Test void mixedUnknownCrossFileAndNonCanonicalInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DccControlledFileBodyReadContext(
                DccControlledFileBodyReadContext.Intent.DAILY,"1","A/1",null,null,null));
        assertThrows(IllegalArgumentException.class, () -> new DccControlledFileBodyReadContext(
                DccControlledFileBodyReadContext.Intent.AUTHORING,"1","A/1",null,"round",null));
        assertThrows(IllegalArgumentException.class, () -> new DccControlledFileBodyReadContext(
                DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,"1","A/1","UPLOAD","round",null));
        assertThrows(IllegalArgumentException.class, () -> DccControlledFileBodyReadContext.fromWire(2L,DccControlledFileBodyReadContext.daily(1L).wire()));
        String mixed="{\"intent\":\"DAILY\",\"controlledFileId\":\"1\",\"isApproval\":\"true\"}";
        assertThrows(IllegalArgumentException.class, () -> DccControlledFileBodyReadContext.fromWire(1L,
                Base64.getUrlEncoder().withoutPadding().encodeToString(mixed.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> DccControlledFileBodyReadContext.fromWire(1L,"bad-wire!"));
        for (String json : new String[]{"{\"intent\":\"DAILY\",\"controlledFileId\":\"1\",\"controlledFileId\":\"1\"}",
                "{\"controlledFileId\":\"1\",\"intent\":\"DAILY\"}",
                "{\"intent\":\"DAILY\",\"controlledFileId\":1}",
                "{\"intent\":\"DAILY\",\"controlledFileId\":\"1\",\"expectedVersionNo\":null}"}) {
            String wire=Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> DccControlledFileBodyReadContext.fromWire(1L,wire));
        }
        byte[] invalidUtf8 = new byte[]{(byte)0xc3,(byte)0x28};
        assertThrows(RuntimeException.class, () -> DccControlledFileBodyReadContext.fromWire(1L,
                Base64.getUrlEncoder().withoutPadding().encodeToString(invalidUtf8)));
    }

    @Test void officeActorBindsExactContextInsteadOfCurrentMutableRound() {
        var context=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,"1","A/1","REVISION","old-round","old-task");
        var actor=new DccVerifiedOfficeReadActor(31L,99L,1L,"A/1","old-round",context.purpose(),2L,"event","trace",context);
        var file=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(1L).tenantId(31L).versionNo("A/1").processInstanceId("new-round").build();
        assertTrue(actor.matches(99L,file)); assertFalse(actor.matches(88L,file));
        file.setVersionNo("B/1"); assertFalse(actor.matches(99L,file));
        assertThrows(IllegalArgumentException.class, () -> new DccVerifiedOfficeReadActor(31L,99L,1L,"A/1","other-round",context.purpose(),2L,"event","trace",context));
    }
}
