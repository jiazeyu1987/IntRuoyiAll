# Verification Report

## Scope

验证 `C:\Users\BJB110\Desktop\文档\批记录压力泵_工序设备参数_完成版.xlsx` 是否按要求保存工序、设备、编码、参数/标准展开列，并验证 `C:\Users\BJB110\Desktop\文档\批记录压力泵_设备台账匹配_完成版.xlsx` 是否按当前设备台账核对设备编码存在情况。

## Result

PASS

## Evidence

- OfficeCLI validate：`Validation passed: no errors found.`
- OfficeCLI issues：`Found 0 issue(s):`
- OfficeCLI outline：工作表 `工序设备参数` 为 19 行 × 14 列。
- 表头：`工序`、`设备`、`编码`、`参数1`、`标准1`、`参数2`、`标准2`、`参数3`、`标准3`、`参数4`、`标准4`、`参数5`、`标准5`、`备注`。
- 关键样例：粗洗 / 超声波清洗机 / B09393 / 清洗次数=2 / 清洗介质=自来水 / 清洗功率=20-30% / 清洗温度=室温 / 清洗时间=30min。
- 尾部样例：单包装多设备行、中包装行、大包装行均可读。
- 设备台账来源：本地 Docker MySQL `int-ruoyi-mysql` / `ruoyi-vue-pro.mes_dv_machinery`，按设备编码核对未删除记录。
- 台账匹配 Excel validate：`Validation passed: no errors found.`
- 台账匹配 Excel issues：`Found 0 issue(s):`
- 台账匹配 Excel outline：工作表 `设备台账匹配` 为 13 行 × 7 列。
- 台账匹配表头：`来源设备`、`设备编码`、`台账匹配`、`未删除记录数`、`总记录数`、`已删除记录数`、`未删除记录详情`。
- 台账匹配结果：12 个设备编码中 9 个存在、3 个不存在。
- 不存在编码：`B04091`（箱型干燥机）、`C01017`（撤压机）、`A05075`（光固机）。
- 已存在编码：`B09393`、`B09353`、`A05059`、`B09026`、`G01143`、`A05199`、`A05203`、`A05048`、`A03274`。
- 错误值与视觉文本检查：未发现 `#REF!`、`#DIV/0!`、`#VALUE!`、`#NAME?`、`#N/A`、`###`、`<TODO>` 或 `{{`。

## Notes

- 源 `.doc` 中单包装区域可读文本包含 4 台设备、3 组温度/时间标准；Excel 已在备注列标记未能唯一对应的自动热合机温度/时间，未臆造。
- 源 `.doc` 中未见设备编码字段或设备参数表的工序已在备注列标记。
