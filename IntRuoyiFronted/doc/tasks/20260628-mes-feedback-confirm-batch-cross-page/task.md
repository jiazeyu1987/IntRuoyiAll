# 任务：报工整批确认跨分页漏填修复

## 任务目标

- 修复待归属页“确认报工”在当前锁定批次超过一页时只提交当前页行数据，导致补齐后仍反复提示“当前批次存在漏填记录”的问题。
- 保持“整批确认”语义不变：仍按当前锁定批次的全部真实工序草稿统一校验、统一提交。
- 不引入前端兜底或后端降级，不改变“其他订单”跳过规则和现有后端 fail-fast 校验。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-route-use-hourly-capacity-integer\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本次只收敛报工整批确认与其后续编译回归，不混入排产用途整数化之外的其他页面改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持现有报工页面与紧凑表格交互，不做无关视觉改版。
  - PowerShell 读取和记录中文文件时必须显式使用 UTF-8。
  - 本轮先做静态契约和定向代码回归，不执行真实 Playwright、登录写入、服务器操作或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接修正整批确认的数据收集边界，使“整批”真正覆盖全批次，而不是依赖当前分页。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 锁定批次跨分页时整批确认仍覆盖全量记录 -> Given 当前导入批次包含超过一页的已归属真实工序草稿 / When 班组长在任意分页点击确认报工 / Then 前端必须按当前锁定批次拉取全量待归属记录并构建确认 payload，而不是只提交当前页。`
- `BDD: 补齐最后一条后不再反向提示其他页漏填 -> Given 第一页已有 10 条真实工序草稿且第二页剩余 1 条缺字段 / When 用户补齐第二页最后 1 条后确认报工 / Then 系统不应再把第一页 10 条误判为漏填，而应提交当前锁定批次的全部真实工序草稿。`
- `BDD: 其他订单行继续只跳过不阻断 -> Given 当前锁定批次同时存在真实工序草稿和其他订单行 / When 前端构建整批确认 payload / Then 仍只包含真实工序草稿，其他订单行继续被排除。`
- `BDD: 整批拉取请求必须满足后端分页契约 -> Given 当前锁定批次已锁定 N 条导入记录 / When 前端为整批确认拉取全量待归属记录 / Then 请求必须使用大于等于 1 的 pageSize，并且 pageSize 至少覆盖当前锁定批次 importRecordIds 数量，不能再传 -1 触发后端校验失败。`

## 里程碑

1. M1：补任务文档、命令记录并建立跨分页整批确认 RED 静态契约。`COMPLETED`
2. M2：最小化修改 `反馈/index.vue` 的整批确认数据收集逻辑。`COMPLETED`
3. M3：运行定向静态回归并更新执行证据。`COMPLETED`
4. M4：修复 `buildConfirmBatchPayload` 引入的 SFC 编译回归并补解析验证。`COMPLETED`
5. M5：修复整批拉取使用 `pageSize=-1` 导致的后端分页校验回归，并补静态契约。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js`
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js`
- `node tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js`（更新后需验证不再接受 `pageSize=-1`）
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js`

## 最终验证结果

- `@'...vue/compiler-sfc parse...'@ | node -` -> PASS，`src/views/mes/pro/feedback/index.vue` 解析恢复正常，不再出现 `Unexpected token, expected ","`
- `node tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js`（升级后的正数分页契约）-> PASS
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260628-mes-feedback-confirm-batch-cross-page --mode preview` -> PASS，预览保留 `task.md` / `execution-log.md`，其余任务证据文件按规则可清理

## 完成记录

- 跨分页整批确认逻辑已按当前锁定批次全量拉取待归属记录，并合并当前页草稿后统一校验和提交。
- `buildConfirmBatchPayload` 的返回对象已补回 `rows:` 属性名，消除本次 `vue/compiler-sfc` 语法错误。
- 本轮追加修复会把整批拉取参数改回满足后端契约的正数 `pageSize`，避免 `Validation failed ... pageSize=-1` 再次中断确认报工。
- 整批拉取当前改为 `Math.max(currentImportRecordIds.value.length, 1)`，既保持“按锁定批次全量拉取”的根因修复，又满足后端 `pageSize >= 1` 的分页契约。

## Current Status

completed
