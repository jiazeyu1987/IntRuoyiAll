package cn.iocoder.yudao.module.system.api.workbench;
import java.util.List;
public record TaskChunk(List<TaskSummary> rows, ChunkCursor after, boolean hasNext) {
 public static TaskChunk from(List<TaskSummary> fetched,int limit) {
  if (limit!=100 || fetched==null || fetched.size()>limit+1) throw new IllegalStateException("Invalid chunk");
  List<TaskSummary> rows=List.copyOf(fetched.subList(0,Math.min(limit,fetched.size())));
  return new TaskChunk(rows,rows.isEmpty()?null:rows.get(rows.size()-1).cursor(),fetched.size()>limit);
 }
}
