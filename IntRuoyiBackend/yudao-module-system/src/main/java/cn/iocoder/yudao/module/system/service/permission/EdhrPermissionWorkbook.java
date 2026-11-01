package cn.iocoder.yudao.module.system.service.permission;

import cn.idev.excel.FastExcelFactory;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/** Versioned, human-editable workbook. Only the two explicitly labelled grant sheets are input. */
public final class EdhrPermissionWorkbook {
    public static final String VERSION = "EDHR-PERMISSION-2";
    public static final String SOURCE = "芋道源码";
    public static final String TARGET = "瑛泰集团";
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 10000;
    static final List<String> ACCOUNT_HEAD = List.of("来源账号", "来源姓名", "来源状态", "目标账号", "来源角色编码", "目标角色编码");
    static final List<String> ROLE_HEAD = List.of("来源角色编码", "目标角色编码", "目标角色名称", "数据范围", "权限标识", "菜单路径", "业务用途");
    private static final List<String> SHEETS = List.of("文件说明", "账号角色", "角色权限", "权限用途");

    private EdhrPermissionWorkbook() { }

    public record AccountRow(int row, String sourceUsername, String sourceName, String sourceStatus,
                             String targetUsername, String sourceRole, String targetRole) { }
    public record RoleRow(int row, String sourceRole, String targetRole, String name, String dataScope,
                          String permission, String menuPath) { }
    public record Content(List<AccountRow> accounts, List<RoleRow> roles) { }

    public static byte[] write(List<List<String>> accounts, List<List<String>> roles,
                               List<List<String>> purposes, String exportedAt) throws IOException {
        if (roles.isEmpty()) throw invalid("来源租户没有可导出的显式 eDHR 业务角色授权");
        var out = new ByteArrayOutputStream();
        try (var writer = FastExcelFactory.write(out).autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()).build()) {
            List<List<String>> notes = List.of(
                    List.of("文件版本", VERSION), List.of("来源租户", SOURCE), List.of("目标租户", TARGET),
                    List.of("导出时间", exportedAt),
                    List.of("填写方法", "仅修改账号角色表的目标账号；按需删除不迁移的账号关系或角色权限行。目标账号必须已存在且启用，不创建账号，不复制密码或电子签名。"),
                    List.of("授权范围", "仅迁移来源自定义非管理员角色的显式 eDHR 权限；不展开超级管理员、临时角色或动态权益。目标角色编码由来源编码确定，名称不作为匹配依据。"),
                    List.of("数据范围", "导出如实标记来源范围；导入仅支持全部数据、仅本人。部门范围缺少明确目标部门映射时禁止导入，请移除该角色及其账号关系，不得修改为更大范围。本工具不推断部门映射。"),
                    List.of("权限影响", "必要父菜单也将授权并在预览列明；共有权限可能同时控制多个业务入口，请核对权限用途。已有角色只允许相同数据范围且无额外菜单，新增权限将影响已有成员。"),
                    List.of("菜单匹配", "按完整菜单路径与权限标识精确匹配目标菜单；同权限的其他入口不会自动授予。带权限的父菜单也须在文件明确列出，路径缺失或不唯一时停止导入。"),
                    List.of("确认规则", "先在瑛泰集团上传预览，核对完整授权和已有成员影响后确认；整批成功或回滚，保留用户原有角色，不移除授权。"),
                    List.of("历史任务", "仅配置未来权限。不会重写历史任务候选、签名、生产记录、人员归属、班组关系及审批快照。"));
            writeSheet(writer, 0, SHEETS.get(0), List.of("项目", "说明"), notes);
            writeSheet(writer, 1, SHEETS.get(1), ACCOUNT_HEAD, accounts);
            writeSheet(writer, 2, SHEETS.get(2), ROLE_HEAD, roles);
            writeSheet(writer, 3, SHEETS.get(3), List.of("权限标识", "业务用途", "代码依据"), purposes);
        }
        byte[] data = out.toByteArray();
        if (data.length > MAX_BYTES || accounts.size() >= MAX_ROWS || roles.size() >= MAX_ROWS)
            throw invalid("授权数据超过单文件上限，请先缩小来源授权范围");
        return data;
    }

    private static void writeSheet(cn.idev.excel.ExcelWriter writer, int index, String name,
                                   List<String> head, List<List<String>> rows) {
        writer.write(rows, FastExcelFactory.writerSheet(index, name)
                .head(head.stream().map(List::of).toList()).build());
    }

    public static Content read(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
            throw invalid("请选择非空且不超过 5MB 的 .xlsx 文件");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != SHEETS.size()) throw invalid("工作表数量与导出模板不符");
            if (!workbook.getExternalLinksTable().isEmpty() || workbook.isMacroEnabled())
                throw invalid("文件不得包含外部链接或宏");
            for (int s = 0; s < SHEETS.size(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (!SHEETS.get(s).equals(sheet.getSheetName()) || workbook.isSheetHidden(s) || workbook.isSheetVeryHidden(s))
                    throw invalid("工作表名称、顺序或可见性与模板不符");
                if (sheet.getLastRowNum() >= MAX_ROWS) throw invalid(sheet.getSheetName() + "超过10000行");
                for (Row row : sheet) for (Cell cell : row) {
                    if (cell.getCellType() != CellType.STRING && cell.getCellType() != CellType.BLANK)
                        throw invalid(sheet.getSheetName() + "第" + (row.getRowNum() + 1) + "行必须为文本，禁止公式或数值账号");
                    if (cell.getHyperlink() != null) throw invalid("文件不得包含超链接");
                    if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().length() > 4000)
                        throw invalid(sheet.getSheetName() + "存在过长单元格");
                }
            }
            Sheet info = workbook.getSheetAt(0);
            if (!VERSION.equals(cell(info.getRow(1), 1)) || !SOURCE.equals(cell(info.getRow(2), 1))
                    || !TARGET.equals(cell(info.getRow(3), 1))) throw invalid("文件版本或来源/目标租户不符，请重新导出");
            validateHead(workbook.getSheetAt(1), ACCOUNT_HEAD);
            validateHead(workbook.getSheetAt(2), ROLE_HEAD);
            List<AccountRow> accounts = new ArrayList<>();
            Sheet accountSheet = workbook.getSheetAt(1);
            for (int i = 1; i <= accountSheet.getLastRowNum(); i++) {
                Row r = accountSheet.getRow(i);
                if (empty(r)) continue;
                accounts.add(new AccountRow(i + 1, cell(r, 0), cell(r, 1), cell(r, 2), cell(r, 3), cell(r, 4), cell(r, 5)));
            }
            List<RoleRow> roles = new ArrayList<>();
            Sheet roleSheet = workbook.getSheetAt(2);
            for (int i = 1; i <= roleSheet.getLastRowNum(); i++) {
                Row r = roleSheet.getRow(i);
                if (empty(r)) continue;
                roles.add(new RoleRow(i + 1, cell(r, 0), cell(r, 1), cell(r, 2), cell(r, 3), cell(r, 4), cell(r, 5)));
            }
            if (roles.isEmpty()) throw invalid("角色权限表没有待导入数据");
            return new Content(accounts, roles);
        }
    }

    private static void validateHead(Sheet sheet, List<String> head) {
        if (sheet.getRow(0) == null || sheet.getRow(0).getLastCellNum() != head.size())
            throw invalid(sheet.getSheetName() + "列数量不符");
        for (int i = 0; i < head.size(); i++) if (!head.get(i).equals(cell(sheet.getRow(0), i)))
            throw invalid(sheet.getSheetName() + "第" + (i + 1) + "列标题不符");
        for (Row row : sheet) if (row.getLastCellNum() > head.size()) throw invalid(sheet.getSheetName() + "存在模板以外的列");
    }

    private static String cell(Row row, int col) {
        if (row == null || row.getCell(col) == null) return "";
        return row.getCell(col).getStringCellValue().trim();
    }
    private static boolean empty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) if (!cell.toString().isBlank()) return false;
        return true;
    }
    static RuntimeException invalid(String message) {
        return cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0(400, message);
    }
}
