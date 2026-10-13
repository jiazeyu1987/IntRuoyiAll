package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
class WorkbenchQueryTokensTest {
 static class Store implements WorkbenchTokenStore {
  final Map<String,String> data=new HashMap<>(); Duration ttl;
  public boolean create(String key,String record,Duration duration){ttl=duration;return data.putIfAbsent(key,record)==null;}
  public String read(String key){return data.get(key);}
 }
 final Instant now=Instant.parse("2026-10-09T00:00:00Z");
 final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
 WorkbenchQueryTokens tokens(Store store,Instant time){return new WorkbenchQueryTokens(store,mapper,Clock.fixed(time,ZoneOffset.UTC));}
 WorkbenchQueryTokens.Issued issue(WorkbenchQueryTokens t){return t.issue(2,3,1,"revision",List.of("SIX"),new WorkbenchQuerySpec("visible",null,null,null),"epoch:1");}
 void fails(int code,Runnable call){assertEquals(code,assertThrows(ServiceException.class,call::run).getCode());}
 @Test void serverIssuedOpaqueTokenWorksAcrossInstancesWithoutClientVersionInput(){
  Store s=new Store();var first=tokens(s,now);var issued=issue(first);
  assertTrue(issued.queryVersion().matches("[A-Za-z0-9_-]{43}"));assertEquals(Duration.ofSeconds(300),s.ttl);
  assertEquals(issued.claim(),tokens(s,now.plusSeconds(299)).verify(issued.queryVersion(),"epoch:1",2,3));
  assertNotEquals(issued.queryVersion(),issue(first).queryVersion());
  fails(QUERY_CONTEXT_CHANGED.code(),()->first.verify(issued.queryVersion(),"epoch:2",2,3));
  fails(QUERY_CONTEXT_CHANGED.code(),()->first.verify(issued.queryVersion(),"epoch:1",9,3));
  fails(QUERY_CONTEXT_CHANGED.code(),()->first.verify(issued.queryVersion(),"epoch:1",2,9));
  fails(QUERY_TOKEN_EXPIRED.code(),()->tokens(s,now.plusSeconds(300)).verify(issued.queryVersion(),"epoch:1",2,3));
 }
 @Test void arbitraryStructurallyValidTokensAreNeverServerIssued(){
  Store s=new Store();var t=tokens(s,now);
  fails(QUERY_TOKEN_INVALID.code(),()->t.verify("client-version-100","epoch:1",2,3));
  fails(QUERY_TOKEN_EXPIRED.code(),()->t.verify("A".repeat(43),"epoch:1",2,3));
  var i=issue(t);s.data.put(i.queryVersion(),"{}");
  fails(QUERY_TOKEN_INVALID.code(),()->t.verify(i.queryVersion(),"epoch:1",2,3));
 }
 @Test void storeFailuresAndCollisionDoNotRetryOrFallback(){
  WorkbenchTokenStore broken=new WorkbenchTokenStore(){public boolean create(String a,String b,Duration c){throw new IllegalStateException("store offline");}public String read(String a){throw new IllegalStateException("store offline");}};
  var t=new WorkbenchQueryTokens(broken,mapper,Clock.fixed(now,ZoneOffset.UTC));
  fails(QUERY_TOKEN_STORE_FAILED.code(),()->issue(t));fails(QUERY_TOKEN_STORE_FAILED.code(),()->t.verify("A".repeat(43),"epoch:1",2,3));
  Store collision=new Store(){public boolean create(String a,String b,Duration c){return false;}};
  fails(QUERY_TOKEN_STORE_FAILED.code(),()->issue(tokens(collision,now)));
 }
 @Test void canonicalSpecAndMicrosecondContractRejectUnrestrictedInputs(){
  var spec=new WorkbenchQuerySpec("visible",null,new WorkbenchQuerySpec.QuickFilter("detail","contains","  文本  "),new WorkbenchQuerySpec.Sort("detail","asc"));
  assertEquals("文本",spec.normalized().quickFilter().value());
  fails(INVALID_QUERY.code(),()->new WorkbenchQuerySpec("all",null,null,null).normalized());
  fails(INVALID_QUERY.code(),()->new WorkbenchQuerySpec("visible",null,null,new WorkbenchQuerySpec.Sort("id;drop","asc")).normalized());
 }
}
