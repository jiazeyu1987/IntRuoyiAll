# Jimu 签名日期单元格组件类型修复

## Task Goal

修复 eDHR 批记录模板中“记录人/日期”等签名日期填写单元格被后端生成成 Jimu 多行文本组件的问题，确保该类日期/签名日期单元格不会显示为“当前组件：多行文本”。

## Milestones

- [x] 建立复现与 BDD/TDD 证据。
- [x] 新增后端回归测试，先证明当前逻辑会把签名日期宽空白单元格误判为 textarea。
- [x] 在批记录 Jimu JSON 生成链路中做最小修复。
- [x] 运行定向 Maven 验证和相关证据校验。

## Expected Verification

- 定向后端测试先 RED 后 GREEN。
- Jimu JSON 生成后的目标控件 `componentFlag` 不再是 `input-textarea`。
- 不引入 fallback、默认成功、吞异常或模板特例硬编码。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在共享单元格语义判断中修复签名日期类宽空白填写单元格的组件类型判定。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### eDHR 批记录 Word 表格解析门禁

- Trigger: 批记录 Word 导入、Jimu JSON 生成、截图位置或控件类型与单元格语义不一致。
- Preflight check: 先定位共享 parser / cell-rule / JSON builder 规则，避免按产品名、表单名或文件名硬编码特例。
- Blocker: 若不能稳定用测试复现结构或控件类型偏差，不得宣称修复完成。
- Verification: 新增合成回归测试覆盖目标行形态，并运行定向 Maven 测试。
- Forbidden action: 禁止截图裁剪、前端隐藏、表单名特例、产品名特例或 API-only 口头判断。
- Evidence: `docs/backend-development.md#eDHR 批记录 Word 表格解析门禁`。

### 旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁

- Trigger: Jimu 当前 JSON 的 `fillForm.componentFlag` / `edhrCellRule.componentFlag` 与已修复语义不一致。
- Preflight check: 同时审计静态文本、`fillForm` 控件类型、`edhrCellRule` 值类型与组件类型。
- Blocker: 若业务列仍残留未确认 AUTO 规则的错误组件类型，不得只通过前端显示掩盖。
- Verification: 回归测试断言目标 `fillForm` 不再生成 `input-textarea`。
- Forbidden action: 禁止只重新导入新版本、前端隐藏或手工改 JSON 后宣称完成。
- Evidence: `docs/backend-development.md#旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁`。

## Cleanup Keep

- doc/tasks/20260727-jimu-signature-date-cell-type/bug-regression-evidence.md
- doc/tasks/20260727-jimu-signature-date-cell-type/backend-api-evidence.md
