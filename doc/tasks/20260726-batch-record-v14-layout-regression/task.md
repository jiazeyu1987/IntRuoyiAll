# 20260726 Batch Record V14 Layout Regression

## Task Goal

修复 V14 批记录导入后仍存在的全局表格识别错位问题：物料/自检类密集表格中，视觉上只属于“物料名称”的勾选文本不得被复制或错位到“批号 / 生产数量 / 自检合格数量”等相邻列；修复必须适用于所有同类表格，不得按表单名、工序名、文件名或压力泵模板做特例。

## Milestones

- [ ] 复现 V14 当前错误，并确认错误来自后端解析/布局 JSON 而不是截图工具或前端渲染单点问题。
- [ ] 编写先失败的回归测试，覆盖同类表格的全局行/单元格形态。
- [ ] 在共享解析/布局逻辑中做最小根因修复，不引入 fallback、降级或吞异常。
- [ ] 用用户指定 Word 与 V14 页面完成验证，并输出截图证据。
- [ ] 更新任务证据、验证报告和收尾状态。

## Expected Verification

- 后端目标回归测试 RED 后 GREEN。
- 相关批记录解析/布局测试通过。
- 使用 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 通过新识别前后端导入或校验 V14 结果。
- 批记录表单页签中截图确认错误列不再出现重复的 `□ 30atm压力表` / `□ 40atm压力表` 错位内容。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标为共享表格结构识别规则。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

### eDHR 批记录 Word 表格解析门禁

- Trigger: 批记录 Word 导入、packed 物料矩阵、生产自检/合格标准/检验方法说明块、截图位置错位。
- Preflight check: 先用真实源 DOC 与最小合成表格复现结构偏差，定位到共享 parser/calibrator/row-type/json-builder 规则；对 packed 宽单元格按视觉 token 处理，对短标题 + 长说明行按说明区行形态判断。
- Blocker: 缺少真实源 DOC、测试类硬编码本地 fixture 不存在、或 RED 不能稳定复现时，不得宣称修复完成。
- Verification: 回归必须同时包含合成 RED/GREEN 和用户指定真实 DOC 样本；至少断言 packed/密集表格不新增错误物料项、不把物料名称串到后续业务列、说明块不被吞入明细区域。
- Forbidden action: 禁止用表单名、工序名、文件名、压力泵模板名硬编码特例；禁止只靠截图人工判断完成。
- Evidence: `docs/backend-development.md#eDHR 批记录 Word 表格解析门禁`。
