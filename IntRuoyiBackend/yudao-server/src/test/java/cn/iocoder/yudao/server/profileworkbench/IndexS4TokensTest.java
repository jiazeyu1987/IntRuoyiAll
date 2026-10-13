package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.ProjectionFence;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

class IndexS4TokensTest {
    static class Time extends Clock {
        Instant value=Instant.parse("2026-10-10T01:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zone){return this;} public Instant instant(){return value;}
    }
    final Time clock=new Time();final Map<String,String> stored=new HashMap<>();
    final WorkbenchTokenStore store=mock(WorkbenchTokenStore.class);
    final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    IndexQueryTokens tokens(){
        when(store.create(anyString(),anyString(),any())).thenAnswer(i->{stored.put(i.getArgument(0),i.getArgument(1));return true;});
        when(store.read(anyString())).thenAnswer(i->stored.get(i.getArgument(0)));
        return new IndexQueryTokens(store,mapper,clock);
    }
    WorkbenchQuerySpec spec(){return new WorkbenchQuerySpec("visible",null,null,null).normalized();}
    ProjectionFence.Version version(){return new ProjectionFence.Version("g","r",List.of("SIX"),1,2,3,4,5,clock.value,clock.value.plusSeconds(5),"hash");}
    @Test void futureGrantBoundaryShortensTokenAndExactBoundaryRejectsReplay(){
        var tokens=tokens();var issued=tokens.issue(2,1,"epoch",spec(),version());
        assertEquals(clock.value.plusSeconds(5),issued.claim().expiresAt());assertEquals(43,issued.token().length());
        assertEquals(issued.claim(),tokens.verify(issued.token(),"epoch",2,1));
        clock.value=clock.value.plusSeconds(5);
        assertEquals(VERSION_EXPIRED.code(),assertThrows(ServiceException.class,()->tokens.verify(issued.token(),"epoch",2,1)).getCode());
    }
    @Test void serverIdentityAndEpochCannotBeReplayedAcrossContexts(){
        var tokens=tokens();var issued=tokens.issue(2,1,"epoch",spec(),version());
        for(Runnable action:List.<Runnable>of(()->tokens.verify(issued.token(),"other",2,1),()->tokens.verify(issued.token(),"epoch",3,1),()->tokens.verify(issued.token(),"epoch",2,9)))
            assertEquals(QUERY_CONTEXT_CHANGED.code(),assertThrows(ServiceException.class,action::run).getCode());
    }
    @Test void storageFailureAndMalformedClaimRemainExplicit(){
        var tokens=tokens();when(store.create(anyString(),anyString(),any())).thenReturn(false);
        assertEquals(QUERY_TOKEN_STORE_FAILED.code(),assertThrows(ServiceException.class,()->tokens.issue(2,1,"epoch",spec(),version())).getCode());
        stored.put("a".repeat(43),"{}");
        assertEquals(QUERY_TOKEN_INVALID.code(),assertThrows(ServiceException.class,()->tokens.verify("a".repeat(43),"epoch",2,1)).getCode());
    }
}
