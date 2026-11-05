package cn.iocoder.yudao.module.mes.service.pro.testdata;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

public final class MesLocalTestRunContracts {
  private MesLocalTestRunContracts() {}

  @Data
  public static class Request {
    private String source;
    private String runId;
    private Long requestId;
    private Long projectId;
    private Long catalogId;
    private Long relationId;
    private Long mesProductId;
    private List<OrderInput> orders;
  }

  @Data
  public static class OrderInput {
    private String slot;
    private BigDecimal quantity;
    private String batchCode;
    private Boolean includeReplenishment;
  }

  public record ApprovedProduct(
      @JsonSerialize(using = ToStringSerializer.class) Long requestId,
      @JsonSerialize(using = ToStringSerializer.class) Long projectId,
      @JsonSerialize(using = ToStringSerializer.class) Long catalogId,
      @JsonSerialize(using = ToStringSerializer.class) Long relationId,
      @JsonSerialize(using = ToStringSerializer.class) Long mesProductId,
      String productCode,
      String productName,
      String projectCode) {}

  public record Check(String code, String message, String path, boolean ready) {}

  public record OrderResult(
      String slot,
      @JsonSerialize(using = ToStringSerializer.class) Long manifestId,
      @JsonSerialize(using = ToStringSerializer.class) Long workOrderId,
      String workOrderCode,
      Integer status,
      boolean reused) {}

  public record Preflight(
      boolean ready, List<Check> checks, List<Check> missing, List<OrderResult> orders) {}

  public record Result(String source, String runId, List<OrderResult> orders) {}
}
