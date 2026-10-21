package cn.iocoder.yudao.module.system.api.workbench;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
public final class SortContract {
 private SortContract() { }
 public static String rejectQuery() { throw WorkbenchFailure.INVALID_QUERY.exception(); }
 public static final LocalDateTime EPOCH=LocalDateTime.of(1970,1,1,0,0);
 public static int binaryCompare(String a,String b) {
  return Arrays.compareUnsigned((a==null?"":a).getBytes(StandardCharsets.UTF_8),(b==null?"":b).getBytes(StandardCharsets.UTF_8));
 }
 public static String text(TaskSummary row,String key) {
  return text(row.cursor(),key);
 }
 public static String text(ChunkCursor row,String key) {
  return switch(key) {
   case "taskType" -> row.taskType(); case "source" -> row.source();
   case "detail" -> row.detail(); case "statusLabel" -> row.statusLabel();
   default -> throw WorkbenchFailure.INVALID_QUERY.exception();
  };
 }
 public static int compare(ChunkCursor a,ChunkCursor b,QueryContext q) {
  if(q.getSortKey()!=null) {
   int custom=binaryCompare(text(a,q.getSortKey()),text(b,q.getSortKey()));
   if(custom!=0) return "desc".equals(q.getSortOrder())?-custom:custom;
  }
  if((a.dueAt()==null)!=(b.dueAt()==null)) return a.dueAt()==null?1:-1;
  int date=a.dueAt()!=null?a.dueAt().compareTo(b.dueAt()):
   (b.createdAt()==null?EPOCH:b.createdAt()).compareTo(a.createdAt()==null?EPOCH:a.createdAt());
  return date!=0?date:-binaryCompare(a.rowOrderKey(),b.rowOrderKey());
 }
 public static Comparator<TaskSummary> comparator(QueryContext q) {
  return (a,b)-> {
   // Source rank precedes the module row key, so compare the common prefix first.
   ChunkCursor ac=a.cursor(),bc=b.cursor();
   int common=compare(new ChunkCursor(ac.sourceId(),ac.taskType(),ac.source(),ac.detail(),ac.statusLabel(),ac.createdAt(),ac.dueAt(),""),
    new ChunkCursor(bc.sourceId(),bc.taskType(),bc.source(),bc.detail(),bc.statusLabel(),bc.createdAt(),bc.dueAt(),""),q);
   if(common!=0)return common;
   int rank=Integer.compare(a.getSourceRank(),b.getSourceRank());
   return rank!=0?rank:-binaryCompare(ac.rowOrderKey(),bc.rowOrderKey());
  };
 }
 public static void validateTime(LocalDateTime value) {
  if(value!=null && value.getNano()%1000!=0)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
 }
}
