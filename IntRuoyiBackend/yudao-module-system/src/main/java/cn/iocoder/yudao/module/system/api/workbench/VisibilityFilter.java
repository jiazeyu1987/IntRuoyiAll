package cn.iocoder.yudao.module.system.api.workbench;
/** Fixed same-database EXISTS scope. Never carries keys or SQL supplied by a caller. */
public record VisibilityFilter(long tenantId, long userId, long version, String partition) {
 public VisibilityFilter {
  if (tenantId < 0 || userId <= 0 || version < 1 ||
   (partition != null && !partition.equals("visible") && !partition.equals("hidden")))
   throw new IllegalArgumentException("Invalid visibility filter");
 }
}
