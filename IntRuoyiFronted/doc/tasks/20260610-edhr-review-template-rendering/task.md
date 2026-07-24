# eDHR 复盘模板渲染前端任务

## 目标

把 eDHR 批次复盘页里的“已填写批记录”从自定义字段表单改成按电子批记录原始模板布局展示，保留行列、合并单元格、边框和填写值。

## 里程碑

- [x] RED：确认当前 `EdhrExecutionReadonlyForm` 只用 `el-form` / `el-input` 展示字段清单，不按模板网格渲染。
- [x] GREEN：实现只读模板表格渲染，解析 `executionSnapshotJson.layout`、`sheetLayoutJson`、`cellValuesJson`，按 row/column 坐标填值。
- [x] REGRESSION：运行类型检查和真实复盘页 E2E，确认复盘页不再默认展示 JSON 或字段输入框，而是模板表格。

## 预期验证

- `pnpm ts:check` 通过。
- Playwright 打开真实批次复盘页，可见 `产品信息`、`产品名称`、填写值和模板表格边框。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少可渲染模板布局时明确显示错误，不降级为字段清单。
- `是否从根因和长期维护角度解决`：是，复盘页直接使用执行快照里的模板布局和值坐标。
- `是否存在临时补丁或绕过`：否。

## 当前状态

已完成。真实复盘页已验证 15 张已填写批记录按模板表格渲染。
