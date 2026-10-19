package cn.iocoder.yudao.module.mes.service.pro.processpool;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MesCorrectionBaselineGuardTest {
    @Test void twoPagesCannotOverwriteACommittedCorrectionEvenIfPayloadReturnsToOldValue() {
        var revisions=mock(MesProProcessPoolEventRevisionMapper.class);
        var guard=new MesCorrectionBaselineGuard(revisions);
        var event=MesProProcessPoolEventDO.builder().id(4L).rawPayload("{\"quantity\":10}").build();
        String hash=MesCorrectionBaselineGuard.payloadHash(event.getRawPayload());
        when(revisions.selectListByEventIdForUpdate(4L)).thenReturn(List.of());
        guard.requireCurrent(event,0L,hash);
        when(revisions.selectListByEventIdForUpdate(4L)).thenReturn(List.of(MesProProcessPoolEventRevisionDO.builder().id(9L).build()));
        assertThrows(RuntimeException.class,()->guard.requireCurrent(event,0L,hash));
        guard.requireCurrent(event,9L,hash);
        event.setRawPayload("{\"quantity\":12}");
        assertThrows(RuntimeException.class,()->guard.requireCurrent(event,9L,hash));
    }
    @Test void formattingDoesNotCauseFalseConflictAndMissingBaselineFails() {
        var revisions=mock(MesProProcessPoolEventRevisionMapper.class);
        var guard=new MesCorrectionBaselineGuard(revisions);
        var event=MesProProcessPoolEventDO.builder().id(4L).rawPayload("{\"a\":1,\"b\":2}").build();
        when(revisions.selectListByEventIdForUpdate(4L)).thenReturn(List.of());
        guard.requireCurrent(event,0L,MesCorrectionBaselineGuard.payloadHash("{ \"b\": 2, \"a\": 1 }"));
        assertThrows(RuntimeException.class,()->guard.requireCurrent(event,null,null));
    }
    @Test void signatureChallengeBindsBeforeAndAfterContent() {
        assertNotEquals(MesCorrectionBaselineGuard.challenge(1L,"{}","{\"a\":2}","correct"),
                MesCorrectionBaselineGuard.challenge(1L,"{\"a\":1}","{\"a\":2}","correct"));
    }
}
