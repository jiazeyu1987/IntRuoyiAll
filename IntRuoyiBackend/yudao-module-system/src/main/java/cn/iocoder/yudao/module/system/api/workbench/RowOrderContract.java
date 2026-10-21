package cn.iocoder.yudao.module.system.api.workbench;
import java.nio.charset.StandardCharsets;
/** Fixed UTF-8 binary descending total tie breaker; no numeric assumptions or truncation. */
public enum RowOrderContract {
 UTF8_BINARY_DESC;
 // Same maximum storage byte budget as a 160-character utf8mb4 stable task key.
 public static final int MAX_UTF8_BYTES=640;
 public static void validate(String key){
  if(key==null||key.isEmpty()||key.getBytes(StandardCharsets.UTF_8).length>MAX_UTF8_BYTES)throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
 }
}
