# DCC 全类型预览缺失原因修复

## Task Goal

修复 DCC 受控文件预览在 PDF、图片、视频、音频、文本、Office 等类型中遇到预览产物缺失时的表现：前端必须直接展示后端 `previewUnavailableReason`，不得继续请求二进制预览内容并退化为泛化错误。

## Milestones

- [x] 建立 BDD 场景与 TDD 验证路径
- [x] 复现非 Office 预览类型忽略 `previewUnavailableReason` 的失败契约
- [x] 最小修复前端预览加载逻辑与用户可见错误提示
- [x] 运行定向回归、类型检查或记录明确阻塞
- [x] 更新任务证据并完成收尾前准备

## Expected Verification

- `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js`
- `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue IntRuoyiFronted/tests/e2e/dcc-preview-unavailable-reason-static.spec.js doc/tasks/20260803-dcc-preview-all-types-unavailable/task.md doc/tasks/20260803-dcc-preview-all-types-unavailable/execution-log.md doc/tasks/20260803-dcc-preview-all-types-unavailable/verification-report.md`
- 若本地前端依赖与运行态允许，再补充相关既有 DCC viewer 静态契约或 `pnpm ts:check`

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务使用任务专用最小静态契约证明当前行为 RED/GREEN，不修改无关宽契约绕过历史失败。
- `docs/e2e-rules.md#dcc-受控浏览当前有效版与权限隔离门禁`：受控浏览预览必须使用正式当前有效版预览来源，不得用无关草稿/历史版或 API-only 证明。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：技能证据文件 cleanup 前先跑 validator，并把 PASS 摘要复制到保留报告。

## Current Status

completed

实现、定向验证、技能 validator、经验沉淀和 cleanup apply 已完成。最终提交与推送进入 Git 门禁阶段。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。预览不可用应显示正式原因，不做静默降级或默认成功。
- `是否从根因和长期维护角度解决`：是。统一在预览元数据阶段尊重后端不可用原因，避免不同文件类型重复进入二进制失败链路。
- `是否存在临时补丁或绕过`：否。
