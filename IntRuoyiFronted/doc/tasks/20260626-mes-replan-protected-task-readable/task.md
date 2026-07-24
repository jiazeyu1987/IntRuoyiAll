# 任务：MES 重排预览受保护任务可读性改造

## 任务目标

- 将重排预览“受保护任务”表从技术字段展示改为业务语义展示。
- 任务列显示 `工单编码 / 工序名称`，不再显示 `PT-xxxx` 任务号。
- 保护原因列显示中文语义，不再直接显示 `FEEDBACK` 等英文原因码。

## 当前状态

已完成。

## 上一任务检查

- 前一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-feedback-attribution-inline-edit\task.md`
- 状态：`已完成`
- 处理说明：已复跑 4 条 MES 归属静态合同并补齐最终验证结果，本次在同一 MES 前端仓内继续处理手动重排预览可读性问题。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮只做前端源码、静态合同与类型校验，不做真实登录或写入型 E2E。
  - 重排预览表格继续沿用 IntPP 紧凑运维表格风格，不做无关视觉重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。未知保护原因直接显示原始码且显式保留，不隐藏数据。
- `是否从根因和长期维护角度解决`：是。通过前端统一语义映射和业务字段拼装，消除同页技术码直出问题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 受保护任务改为业务可读标题 -> Given 用户打开重排预览且存在受保护任务 / When 页面渲染表格 / Then 任务列显示“工单编码 / 工序名称”，不再显示 PT 任务号。`
- `BDD: 保护原因显示中文语义 -> Given 受保护任务原因码为 FEEDBACK/FINISHED/IN_PROGRESS/LOCKED/MANUAL / When 页面渲染保护原因列 / Then 用户看到“已报工/已完成/进行中/已锁定/人工任务”中文语义，而不是英文码。`

## 里程碑

1. M1：创建前端任务包并补静态 RED 合同。
2. M2：最小修改 API 类型与重排预览表展示。
3. M3：运行 GREEN 静态验证、类型检查并回写证据。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-protected-task-readable-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 最终验证结果

- `node tests/e2e/mes-pro-schedule-order-protected-task-readable-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-replan-protected-task-readable\frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260626-mes-replan-protected-task-readable --mode preview` -> PASS，结果 `status=ready`，仅预览提示 `frontend-feature-evidence.md` 属于可清理任务证据，无阻塞项。
