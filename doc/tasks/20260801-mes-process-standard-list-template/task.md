# MES工序列表标准列表模板

## Task Goal

将 `MES工序` 页面从自定义列表外观改为项目标准列表模板，同时保持压力泵工序.xlsx 数据来源、只读能力、12 个 Excel 原始列、独立分页接口和“不多不少”的数据契约不变。

## Milestones

- [x] 建立任务记录、读取前端交付与列表样式门禁
- [x] 先补静态合同 RED，锁定标准列表模板结构
- [x] 修改 MES工序页面为标准列表模板
- [x] 运行定向验证与前端类型检查
- [x] 更新验证报告与收尾状态

## Expected Verification

- `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260801-mes-process-standard-list-template/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## Applicable Experience Gates

- 前端列表/表格样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。列表页应采用标准查询工具栏 + 表格 + 分页结构，避免一页一套自定义外观；表格需保持紧凑、可扫描、白底浅边框、标准 Element Plus 表格语义。
- 前端静态合同：已读取 `docs/frontend-development.md` 与 `docs/e2e-rules.md`。修改 `tests/e2e/*static.spec.js` 时需按 Windows 换行门禁归一化 CRLF/LF，先跑 RED，再跑 GREEN；若宽合同有无关历史失败，需隔离并记录。
- 严格无 fallback：本任务不允许通过保留自定义 wrapper、隐藏提示、模拟数据、切换接口或默认成功来冒充标准模板完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按项目标准列表模板重构页面结构，不保留自定义列表外观分支。
- `是否存在临时补丁或绕过`：否。
