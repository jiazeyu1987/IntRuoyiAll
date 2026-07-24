# 任务：排产工单人工完成与未完成筛选

- Task ID: `20260629-mes-schedule-order-manual-finish-filter`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

在排产工单页面增加“设为已完成 / 撤销已完成”交互与“未完成 / 全部 / 已完成”筛选。页面默认只看未完成；人工完成后列表强制展示为 100% 已完成，但工序详情继续展示真实报工口径，并显式提示该差异。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-scheduler-workbench-full-config-package\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成；本任务独立修改排产工单页面与 API 合同，不接续工作台全量包改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 读取/写入中文任务文档、日志、静态测试与 E2E 输出时统一显式使用 UTF-8；命令不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 本任务真实验证仅限本机 `http://localhost:8081`。
  - 写入型真实 E2E 仅使用测试租户账号 `smokeplan1` 与 `smokeappr1`，不触碰测试服/正式服。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持排产工单白底紧凑操作台样式，在现有查询栏与行操作区内增补完成筛选和人工完成动作，不做无关布局重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接接正式人工完成接口与完成筛选合同，不通过前端本地覆盖状态伪装成功。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产工单默认只看未完成 -> Given 用户首次进入排产工单页 / When 页面发起分页请求 / Then 默认请求 completionFilter=INCOMPLETE，列表不展示已完成工单。`
- `BDD: 排产员可人工设完成并二次确认 -> Given 某行工单未完成且当前用户具备人工完成权限 / When 用户填写完成原因并确认二次弹窗 / Then 页面调用人工完成接口，刷新后该行显示已完成、100% 并从未完成筛选中消失。`
- `BDD: 已人工完成工单保持真实工序提示 -> Given 某工单已人工完成 / When 用户打开工艺排产路线弹窗 / Then 页面提示该工单列表口径已人工完成，但工序表仍按真实报工展示。`
- `BDD: 撤销已完成仅对撤销权限用户显示 -> Given 某工单已人工完成 / When 当前用户没有撤销权限 / Then 页面不显示撤销已完成按钮。`

## Milestones

1. M1：创建任务文档、确认前序任务状态并锁定交互合同。`completed`
2. M2：补前端 RED 静态测试并实现筛选、完成/撤销交互与展示。`completed`
3. M3：回归静态测试、E2E 脚本与证据。`completed`
4. M4：配合后端真实 E2E 与收尾。`completed`

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-mes-schedule-order-manual-finish-filter\frontend-feature-evidence.md`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js`

## Final Result

- 查询栏已新增“完成筛选”，默认值为“未完成”，并透传 `completionFilter=INCOMPLETE`。
- 页面已接入人工完成与撤销人工完成接口，动作为：
  - 原因弹窗必填
  - 二次确认
  - 调接口后刷新列表
- 行操作权限已拆分：
  - `mes:pro-schedule-order:manual-finish` 控制“设为已完成”
  - `mes:pro-schedule-order:revoke-complete` 控制“撤销已完成”
- 人工完成后的列表表现已统一切换到已完成态：
  - 状态、进度条、数量、工单编码颜色
  - 紧凑“人工完成”标签
  - tooltip 展示时间与原因
- 工艺路线弹窗已补提示：该工单列表按 `100%` 展示，但下方工序仍显示真实报工进度。

## Verification Results

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-static.spec.js` -> PASS。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js` -> PASS，`scheduleOrderId=9`、`workOrderCode=CODexERP20260610B`、`plannerUsername=smokeplan1`、`adminUsername=smokeappr1`。

## Current Blockers

- 无。
