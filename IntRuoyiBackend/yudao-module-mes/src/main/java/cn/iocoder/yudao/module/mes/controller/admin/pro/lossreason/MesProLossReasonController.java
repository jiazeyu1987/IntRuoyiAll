package cn.iocoder.yudao.module.mes.controller.admin.pro.lossreason;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.service.pro.lossreason.MesProLossReasonService;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/loss-reason")
@Validated
public class MesProLossReasonController {
    @Resource private MesProLossReasonService service;
    @Data public static class CreateRequest {
        @NotBlank @Size(max = 128) private String reasonName;
    }
    @Data public static class ImportRow {
        @ExcelProperty("损耗原因描述") private String reasonName;
    }
    @Data public static class ExportRow {
        @ExcelProperty("损耗原因编码") private String reasonCode;
        @ExcelProperty("损耗原因描述") private String reasonName;
        @ExcelProperty("创建时间") private String createTime;
    }
    @GetMapping("/list")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:query','mes:pro-route:version-query')")
    public CommonResult<List<MesProLossReasonService.Reason>> list(@RequestParam(value="keyword", required=false) String keyword) {
        return success(service.list(keyword));
    }
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<MesProLossReasonService.Reason> create(@Valid @RequestBody CreateRequest request) {
        return success(service.create(List.of(request.getReasonName())).get(0));
    }
    @GetMapping("/import-template")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public void template(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "损耗原因导入模板.xlsx", "损耗原因", ImportRow.class, List.of());
    }
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<Integer> importFile(@RequestParam("file") MultipartFile file) throws IOException {
        // Read by header name, preserving physical row indices (including empty rows) for diagnostics.
        List<Map<Integer,String>> rows = new ArrayList<>();
        try (var stream = file.getInputStream()) {
            FastExcelFactory.read(stream, new AnalysisEventListener<Map<Integer,String>>() {
                @Override public void invoke(Map<Integer,String> row, AnalysisContext context) {
                    int index = context.readRowHolder().getRowIndex();
                    // XLSX can omit blank row elements entirely; retain their actual worksheet position.
                    while (rows.size() < index) rows.add(Map.of());
                    rows.add(row);
                }
                @Override public void doAfterAllAnalysed(AnalysisContext context) { }
            }).headRowNumber(0).ignoreEmptyRow(false).sheet(0).doRead();
        }
        if (rows.isEmpty()) throw MesProLossReasonService.invalid("导入文件缺少表头");
        Integer column = rows.get(0).entrySet().stream().filter(e -> "损耗原因描述".equals(e.getValue()))
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> MesProLossReasonService.invalid("缺少表头：损耗原因描述"));
        List<String> descriptions = new ArrayList<>();
        for (int i=1; i<rows.size(); i++) descriptions.add(rows.get(i).get(column));
        return success(service.create(descriptions).size());
    }
    @GetMapping("/export")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public void export(@RequestParam(value="keyword",required=false) String keyword, HttpServletResponse response) throws IOException {
        List<ExportRow> rows = service.list(keyword).stream().map(reason -> {
            ExportRow row = new ExportRow(); row.setReasonCode(reason.reasonCode());
            row.setReasonName(reason.reasonName()); row.setCreateTime(reason.createTime().toString()); return row;
        }).toList();
        ExcelUtils.write(response, "损耗原因.xlsx", "损耗原因", ExportRow.class, rows);
    }
    // No update/delete/disable routes: both UI and API expose append-only operations.
}
