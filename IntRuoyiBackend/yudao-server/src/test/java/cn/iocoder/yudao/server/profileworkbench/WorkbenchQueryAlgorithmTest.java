package cn.iocoder.yudao.server.profileworkbench;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;
class WorkbenchQueryAlgorithmTest {
 static class Source implements SourceAdapter {
  final SourceDescriptor descriptor;final List<TaskSummary> data=new ArrayList<>();final Set<String> hidden=new HashSet<>();int chunks;int maximum;
  Source(int rank){descriptor=new SourceDescriptor("FIXTURE_"+rank,"fixture",1,rank,"来源"+rank,"类别","policy","fixture.open"+rank,1);}
  public SourceDescriptor descriptor(){return descriptor;}
  public RowOrderContract rowOrderContract(){return RowOrderContract.UTF8_BINARY_DESC;}
  public SourceIdentityContract identityContract(){return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY;}
  public boolean isAuthorized(long user,long tenant){return true;}
  public TaskSummary resolveVisibilityIdentity(QueryContext q,String key){throw new AssertionError();}
  List<TaskSummary> selected(QueryContext q){
   q.validate();return data.stream().filter(r->q.getVisibility()==null || hidden.contains(r.getTaskKey())=="hidden".equals(q.getVisibility()))
   .filter(r->q.getTaskType()==null||q.getTaskType().equals(r.getTaskType()))
   .filter(r->q.getFilterField()==null||SortContract.text(r,q.getFilterField()).contains(q.getFilterValue()))
   .sorted((a,b)->SortContract.compare(a.cursor(),b.cursor(),q)).toList();
  }
  public long count(QueryContext q){
   // Sixth source implements the required SQL_BINARY_UNIQUE_TASK_KEY contract before all partitions/filters.
   if(data.stream().map(TaskSummary::getTaskKey).distinct().count()!=data.size())throw SOURCE_CONTRACT_INVALID.exception();
   return selected(q).size();
  }
  public TaskChunk readChunk(QueryContext q,ChunkCursor after,int limit){
   assertEquals(100,limit);chunks++;var fetched=selected(q).stream().filter(r->after==null||SortContract.compare(after,r.cursor(),q)<0).limit(limit+1).toList();maximum=Math.max(maximum,fetched.size());return TaskChunk.from(fetched,limit);
  }
  TaskSummary add(String business,String order,int index){
   TaskSummary r=new TaskSummary();r.setSourceId(sourceId());r.setTaskKey(sourceId()+":"+business);r.setBusinessId(business);r.setRowOrderKey(order);r.setTaskType(descriptor.taskType());r.setSource(descriptor.label());r.setDetail(new String[]{"A","a","é","中","😀",""}[index%6]);r.setStatusLabel("待处理");
   r.setDueAt(index%3==0?null:LocalDateTime.of(2026,10,1,0,0).plusSeconds(index%7).plusNanos((index%9)*1000L));r.setCreatedAt(index%5==0?null:LocalDateTime.of(2026,9,1,0,0).plusSeconds(index%11));r.setAction(new ActionEnvelope(descriptor.actionKind(),1,Map.of("key",business)));data.add(r);return r;
  }
 }
 record Setup(ProfileWorkbenchTodoReadTransaction reader,WorkbenchQueryTokens.Claim claim,List<Source> sources){}
 Setup setup(List<Source> sources,WorkbenchQuerySpec spec){
  var ids=sources.stream().map(Source::sourceId).toList();var registry=new WorkbenchSourceRegistry(new ArrayList<SourceAdapter>(sources),new WorkbenchSourceRegistry.Manifest(1,"revision",List.of(new WorkbenchSourceRegistry.Module("fixture",true,ids))));
  var versions=mock(WorkbenchVisibilityVersionMapper.class);when(versions.shared(2,3)).thenReturn(1L);
  var tokens=new WorkbenchQueryTokens(mock(WorkbenchTokenStore.class),new ObjectMapper().findAndRegisterModules());
  var now=Instant.now();var claim=new WorkbenchQueryTokens.Claim(3,"revision",ids,2,3,1,spec,"epoch",now,now.plusSeconds(300));
  return new Setup(new ProfileWorkbenchTodoReadTransaction(registry,versions,tokens),claim,sources);
 }
 @Test void sixSourcesDeepPageMatchesFullReferenceWithBoundedChunksAndStringKeys(){
  var sources=new ArrayList<Source>();for(int source=0;source<6;source++){Source s=new Source(source);for(int i=0;i<241;i++)s.add("key:"+i,String.format("row-é-%04d",i),i);sources.add(s);}
  for(String order:List.of("asc","desc")){
   var spec=new WorkbenchQuerySpec("visible",null,null,new WorkbenchQuerySpec.Sort("detail",order));var setup=setup(sources,spec);var q=spec.context(2,3,1);
   var all=new ArrayList<TaskSummary>();for(Source s:sources)for(TaskSummary r:s.data){r.setSourceRank(s.descriptor.sortRank());all.add(r);}all.sort(SortContract.comparator(q));
   var page=setup.reader.page("opaque",setup.claim,14,100,System.nanoTime());assertEquals(all.subList(1300,1400),page.rows());assertEquals(1446,page.businessTotal());assertEquals(1446,page.filteredTotal());assertEquals(14,page.effectivePageNo());
   var last=setup.reader.page("opaque",setup.claim,999,100,System.nanoTime());assertEquals(15,last.effectivePageNo());assertEquals(all.subList(1400,1446),last.rows());
  }
  assertTrue(sources.stream().allMatch(s->s.maximum<=101&&s.chunks>=3));
 }
 @Test void hiddenAndFilterDoNotRedefineBusinessOrHiddenTotals(){
  Source s=new Source(5);for(int i=0;i<210;i++){var r=s.add("non-numeric-"+i,"key"+i,i);if(i%2==0)s.hidden.add(r.getTaskKey());}
  var spec=new WorkbenchQuerySpec("hidden",null,new WorkbenchQuerySpec.QuickFilter("detail","contains","中"),null);var setup=setup(List.of(s),spec);
  var count=setup.reader.count("opaque",setup.claim,System.nanoTime());assertEquals(210,count.businessTotal());assertEquals(105,count.hiddenTotal());assertEquals(0,count.filteredTotal());
  assertTrue(setup.reader.page("opaque",setup.claim,100,10,System.nanoTime()).rows().isEmpty());
 }
 @Test void separatedDuplicateIdentityFailsEvenWhenHiddenOrFilterWouldExcludeOne(){
  Source s=new Source(5);var first=s.add("same-non-numeric","z",1);var second=s.add("same-non-numeric","a",4);second.setDueAt(null);second.setCreatedAt(LocalDateTime.of(2020,1,1,0,0));first.setDetail("A");second.setDetail("中");
  var setup=setup(List.of(s),new WorkbenchQuerySpec("visible",null,new WorkbenchQuerySpec.QuickFilter("detail","eq","A"),new WorkbenchQuerySpec.Sort("detail","asc")));
  assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->setup.reader.page("opaque",setup.claim,1,10,System.nanoTime())).getCode());
 }
 @Test void rankPrecedesRowOrderAndPrecisionBudgetFailClosed(){
  Source a=new Source(0),b=new Source(5);var ar=a.add("A","a",0);var br=b.add("Z","z",0);ar.setCreatedAt(null);br.setCreatedAt(null);
  var setup=setup(List.of(a,b),new WorkbenchQuerySpec("visible",null,null,null));assertEquals(ar,setup.reader.page("opaque",setup.claim,1,10,System.nanoTime()).rows().get(0));
  assertEquals(QUERY_BUDGET_EXCEEDED.code(),assertThrows(ServiceException.class,()->setup.reader.count("opaque",setup.claim,System.nanoTime()-10_000_000_001L)).getCode());
  ar.setCreatedAt(LocalDateTime.of(2026,1,1,0,0,0,1));assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->setup.reader.page("opaque",setup.claim,1,10,System.nanoTime())).getCode());
 }

 @Test void malformedChunksAndCountsFailTheEntireRead(){
  for(int fault=0;fault<6;fault++){
   final int mode=fault;Source source=new Source(5){
    public TaskChunk readChunk(QueryContext q,ChunkCursor after,int limit){
     TaskChunk normal=super.readChunk(q,after,limit);
     return switch(mode){case 0->new TaskChunk(List.of(),null,false);case 1->new TaskChunk(normal.rows(),null,normal.hasNext());case 2->{var rows=new ArrayList<>(normal.rows());Collections.reverse(rows);yield new TaskChunk(rows,rows.get(rows.size()-1).cursor(),normal.hasNext());}case 3->new TaskChunk(normal.rows(),normal.after(),false);case 4->new TaskChunk(normal.rows().subList(0,1),normal.rows().get(0).cursor(),true);default->normal;};
    }
    public long count(QueryContext q){return mode==5?-1:super.count(q);}
   };
   for(int i=0;i<101;i++)source.add("key"+i,"row"+i,i);
   var setup=setup(List.of(source),new WorkbenchQuerySpec("visible",null,null,null));
   assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->setup.reader.page("opaque",setup.claim,1,10,System.nanoTime())).getCode(),"fault "+mode);
  }
 }
 @Test void repeatedCursorSourceFailureAndTotalOverflowAreDistinctFailures(){
  Source repeated=new Source(5){public TaskChunk readChunk(QueryContext q,ChunkCursor after,int limit){return super.readChunk(q,null,limit);}};
  for(int i=0;i<201;i++)repeated.add("key"+i,"row"+i,i);
  var r=setup(List.of(repeated),new WorkbenchQuerySpec("visible",null,null,null));
  assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->r.reader.page("opaque",r.claim,2,100,System.nanoTime())).getCode());
  Source broken=new Source(5){public long count(QueryContext q){throw new IllegalStateException("module query failed");}};
  var b=setup(List.of(broken),new WorkbenchQuerySpec("visible",null,null,null));assertEquals(SOURCE_QUERY_FAILED.code(),assertThrows(ServiceException.class,()->b.reader.count("opaque",b.claim,System.nanoTime())).getCode());
  Source huge=new Source(0){public long count(QueryContext q){return "hidden".equals(q.getVisibility())?0:Long.MAX_VALUE;}};
  Source one=new Source(5);one.add("one","one",0);var o=setup(List.of(huge,one),new WorkbenchQuerySpec("visible",null,null,null));assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->o.reader.count("opaque",o.claim,System.nanoTime())).getCode());
 }
 @Test void independentExpectedOrderingCoversUtf8NullDatesAndDescendingStringKeys(){
  Source source=new Source(5);var a=source.add("A","a",0);var accent=source.add("accent","é",0);var china=source.add("china","中",0);var emoji=source.add("emoji","😀",0);
  for(var row:source.data){row.setDueAt(null);row.setCreatedAt(null);row.setDetail(row.getRowOrderKey());}
  var spec=new WorkbenchQuerySpec("visible",null,null,new WorkbenchQuerySpec.Sort("detail","asc"));var setup=setup(List.of(source),spec);assertEquals(List.of(a,accent,china,emoji),setup.reader.page("opaque",setup.claim,1,10,System.nanoTime()).rows());
  spec=new WorkbenchQuerySpec("visible",null,null,null);setup=setup(List.of(source),spec);assertEquals(List.of(emoji,china,accent,a),setup.reader.page("opaque",setup.claim,1,10,System.nanoTime()).rows());
  a.setDueAt(LocalDateTime.of(2026,1,1,0,0));accent.setCreatedAt(LocalDateTime.of(2026,1,1,0,0));assertEquals(List.of(a,accent,emoji,china),setup.reader.page("opaque",setup.claim,1,10,System.nanoTime()).rows());
 }

 @Test void sixthSourceStringRowKeyIsBoundedInBothReaderAndCursor(){
  Source source=new Source(5);var row=source.add("opaque","é".repeat(320),0);var setup=setup(List.of(source),new WorkbenchQuerySpec("visible",null,null,null));assertEquals(row,setup.reader.page("opaque",setup.claim,1,10,System.nanoTime()).rows().get(0));
  var q=setup.claim.querySpec().context(2,3,1);assertTrue(q.validatedChunk(row.cursor(),101));row.setRowOrderKey("é".repeat(321));
  assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->setup.reader.page("opaque",setup.claim,1,10,System.nanoTime())).getCode());assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->q.validatedChunk(row.cursor(),101)).getCode());
 }
 @Test void httpCountContractRejectsNonExactJavascriptTotalsBeforeSerialization(){
  Source source=new Source(5){public long count(QueryContext q){return "hidden".equals(q.getVisibility())?0:9_007_199_254_740_991L;}};
  var setup=setup(List.of(source),new WorkbenchQuerySpec("visible",null,null,null));
  assertEquals(SOURCE_CONTRACT_INVALID.code(),assertThrows(ServiceException.class,()->setup.reader.count("opaque",setup.claim,System.nanoTime())).getCode());
 }
}
