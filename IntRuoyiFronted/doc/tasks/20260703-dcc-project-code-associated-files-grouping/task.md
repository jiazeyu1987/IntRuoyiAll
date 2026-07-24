# 任务：DCC 基础条目关联文档按分类展示

## Current Status

completed

## Task Goal

在 `DCC基础条目` 抽屉中，将“关联文档”从平铺文件表格改为按识别分类层级展示，形成 `一级分类 -> 二级分类 -> 文件` 的阅读结构，并保留当前分页、总数、加载态和受控预览入口。

## Milestones

1. 建立任务文档并记录经验门禁。completed
2. 补充前端静态 RED 回归，覆盖分类字段和分组渲染契约。completed
3. 实现关联文档按 `fileTypeLevel1~5` 聚合展示。completed
4. 运行静态回归、类型校验和证据校验。completed
5. 记录最终验证结果并按门禁收尾。completed

## Expected Verification

- `pnpm e2e:dcc:project-code-basic-data:static`
- `node tests/e2e/dcc-project-code-recognition-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-dcc-project-code-associated-files-grouping/frontend-feature-evidence.md`

## 经验门禁

- 已读取 `docs/powershell-memory.md`：PowerShell 中文读写必须显式 UTF-8；本任务写文件使用 `apply_patch`，命令输出设置 UTF-8。
- 已读取 `docs/experience-index.md`：本任务命中 PowerShell 与前端页面/样式门禁。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：关联文档区域保持蓝灰操作台、紧凑表格、白色工作面和浅灰蓝边框，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 及 `references/frontend-contract.md`：前端只消费正式后端契约，不新增 mock、placeholder、fallback 或静默降级。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。无分类层级的真实数据仅归入 `未分类` 展示，不伪造分类、不吞接口错误。
- `是否从根因和长期维护角度解决`：是。使用后端已返回的 `fileTypeLevel1~5` 正式契约建立展示层级，避免硬编码两级分类。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 关联文档按识别分类层级展示 -> Given DCC 基础条目存在多份已识别分类的关联文档 / When 用户打开基础条目详情抽屉 / Then 关联文档按 fileTypeLevel1 到 fileTypeLevel5 聚合展示，并在分类下列出文件。`
- `BDD: 无分类文档不伪造分类 -> Given 关联文档缺少 fileTypeLevel1 到 fileTypeLevel5 / When 用户查看关联文档区域 / Then 该文档显示在 未分类 分组，文件名称、编号、版本、状态和发布时间仍保留。`
- `BDD: 文件预览入口保持不变 -> Given 用户点击任一分组下的文件名称 / When 前端处理点击事件 / Then 仍调用 openControlledFileViewer(router, route, row.id, 'project-code') 打开受控预览页。`

## Current Blockers

- 暂无。

## 当前验证记录

- RED: `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> FAIL，`ControlledFileVO` 尚未声明 `fileTypeLevel1`。
- GREEN: `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:dcc:project-code-basic-data:static` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-dcc-project-code-associated-files-grouping/frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode preview` -> PASS，预览仅删除本任务额外证据文件，保留 `task.md` 和 `execution-log.md`。
- BLOCKER: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> FAIL，cleanup 脚本未识别原状态行 `Current Status: completed`，已改为 `Status: completed` 后重试。
- BLOCKER: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> FAIL，cleanup 脚本仍未识别行内 `Status: completed`；已按脚本解析规则改为 `## Current Status` 章节。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-dcc-project-code-associated-files-grouping --mode apply` -> PASS，已删除本任务额外证据文件，仅保留核心任务记录。

## 完成记录

- 状态：completed。
- 已补齐 `ControlledFileVO.fileTypeLevel1~fileTypeLevel5`。
- `DCC基础条目` 抽屉中的关联文档已按识别分类层级分组展示，缺少分类层级的文件进入 `未分类` 分组。
- 文件名称点击仍打开受控预览页：`openControlledFileViewer(router, route, row.id, 'project-code')`。
- 验证：DCC 两条静态回归、前端类型校验、前端证据校验、cleanup preview 和 cleanup apply 均通过。
