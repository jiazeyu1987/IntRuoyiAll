# Task: 电子批记录报表列表取消高度限制

## 任务目标

修复电子批记录页面中间“报表名称”列表被固定面板高度限制的问题，使列表随内容自然展开，不再在黄框区域内形成内部滚动。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，后续中文文件读写使用 UTF-8 明确路径，不使用 PowerShell 默认编码或 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次只做局部样式修复，不引入额外视觉重设计。
- 前端功能修复：已读取 `frontend-feature-delivery` 与 `bug-regression-fix-loop` 技能及引用契约，按 BDD + RED/GREEN 记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除目标列表容器自身的高度占满与内部滚动约束。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 报表名称列表自然展开 -> Given 电子批记录某批记录下存在多条报表模板 / When 用户查看中间“报表名称”列表 / Then 列表容器应随内容自然展开，不应通过自身高度限制产生内部滚动。

## 里程碑

- [x] M1: 读取项目经验门禁、前端统一样式和相关技能契约。
- [x] M2: 为列表高度限制新增先失败的静态回归测试。
- [x] M3: 修改电子批记录页面列表样式，取消中间列表高度限制。
- [x] M4: 运行静态回归验证并记录 RED/GREEN 证据。
- [x] M5: 任务收尾、提交本次直接相关改动。

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- 证据契约校验：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260703-electronic-batch-record-report-list-height/bug-regression-evidence.md`
- 前端证据契约校验：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-electronic-batch-record-report-list-height/frontend-feature-evidence.md`

## 当前状态

Completed. 已取消电子批记录中间报表列表自身高度限制；静态回归验证、缺陷证据契约校验、前端证据契约校验、收尾预览均已通过，待 Git 提交落库。

## Cleanup Keep

- `doc/tasks/20260703-electronic-batch-record-report-list-height/bug-regression-evidence.md`
- `doc/tasks/20260703-electronic-batch-record-report-list-height/frontend-feature-evidence.md`
