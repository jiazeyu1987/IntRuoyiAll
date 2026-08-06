# 批记录压力泵设备参数提取

## Task Goal

从 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 直接读取球囊扩张压力泵生产记录中的工序、设备、设备编码和设备参数，生成按列展开的 Excel 文件，并核对这些设备编码在当前设备台账中的存在情况。

## Milestones

- [x] 确认源 `.doc` 文件可直接读取。
- [x] 提取工序、设备、编码、参数和标准值。
- [x] 生成 Excel 文件并设置可读列宽。
- [x] 验证 Excel 内容、结构和可打开性。
- [x] 核对提取出的设备编码在当前设备台账中的存在情况。
- [x] 生成设备台账匹配 Excel 并验证输出。

## Expected Verification

- 使用直接文本读取方式复核源 `.doc` 中生产操作及自检记录块。
- 使用 OfficeCLI 创建并验证 `.xlsx` 文件。
- 复核输出表头包含 `工序`、`设备`、`编码`、`参数1`、`标准1` 等展开列。
- 复核关键样例：粗洗/超声波清洗机/B09393/清洗次数=2/清洗介质=自来水。
- 使用本地 Docker MySQL `ruoyi-vue-pro.mes_dv_machinery` 未删除台账记录核对设备编码。
- 复核台账匹配输出表头包含 `来源设备`、`设备编码`、`台账匹配`、`未删除记录数`、`总记录数`、`已删除记录数`、`未删除记录详情`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。用户已明确授权不用 OfficeCLI 读取 `.doc`，本任务仅对旧版 `.doc` 采用直接只读解析；Excel 输出仍使用 OfficeCLI。
- `是否从根因和长期维护角度解决`：是。本任务目标为一次性资料提取，保留解析和验证证据。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- 已读取 `docs/experience-index.md`；未命中文档提取到 Excel 的专项经验门禁。
- 已应用 `docs/powershell-encoding.md`：中文读取和写入使用 UTF-8 路径或明确编码。
- 已应用 OfficeCLI Excel 技能：`.xlsx` 创建、逐单元格写入、验证优先使用 OfficeCLI。

## Output

- `C:\Users\BJB110\Desktop\文档\批记录压力泵_工序设备参数_完成版.xlsx`
- `C:\Users\BJB110\Desktop\文档\批记录压力泵_设备台账匹配_完成版.xlsx`

## Verification Result

- PASS：OfficeCLI `validate` 返回 `Validation passed: no errors found.`。
- PASS：OfficeCLI `view issues` 返回 `Found 0 issue(s):`。
- PASS：OfficeCLI `view outline` 确认 `工序设备参数` 工作表为 19 行 × 14 列。
- PASS：关键样例已复核：粗洗 / 超声波清洗机 / B09393 / 清洗次数=2 / 清洗介质=自来水。
- PASS：设备台账匹配输出为 `设备台账匹配` 工作表 13 行 × 7 列。
- PASS：12 个设备编码中 9 个在当前未删除设备台账中存在，3 个不存在：`B04091`、`C01017`、`A05075`。
- PASS：OfficeCLI `validate`、`view issues`、错误值查询和 HTML 预览文本检查均未发现结构错误、公式错误、占位符或 `###` 溢出。
