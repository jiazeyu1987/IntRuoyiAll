# 任务：智能排产 smoke 缺失 xlsx 依赖修复

## 任务目标

- 修复 `tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` 在真实运行前因缺失 `xlsx` 依赖直接失败的问题。
- 保证智能排产真实 smoke 脚本的工作簿生成能力与仓库依赖声明一致，不再依赖未声明的本地环境偶然状态。
- 为根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-smart-scheduling-post-p0-gate-refresh\` 重新放行真实 smoke 提供前端侧前置闭环。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-profile-hub-entry\task.md`
- 状态：`completed`
- 处理：上一任务已完成，不阻塞本次缺陷修复。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 长链路真实 E2E 前必须先验证真实登录/入口最小路径。
  - 登录前置失败时必须阻塞，不得伪造 smoke 通过。
  - 本次缺陷修复先做静态 RED/GREEN，再回到根任务执行真实 smoke。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，补齐真实脚本所需正式依赖/契约，不靠本地偶然安装状态
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: smoke 脚本生成反馈工作簿时依赖必须自洽 -> Given smart scheduling smoke 需要在运行时生成 .xlsx 报工文件 / When 执行脚本初始化 prepareFeedbackExcelWorkbook / Then 仓库必须已声明并可解析所需工作簿依赖，不得在真实 smoke 开始前因 Cannot find module 失败。`
- `BDD: 静态合同必须暴露未声明依赖 -> Given e2e 脚本直接 require 外部包 / When 静态合同检查依赖声明 / Then 若 package.json 未声明该包则 RED 失败。`

## 里程碑

1. M1：创建任务包并记录 RED 复现。`DONE`
2. M2：补最小修复与静态合同。`DONE`
3. M3：运行前端侧 GREEN 验证并回填证据。`DONE`

## 预期验证

- `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js`
- `node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` 在依赖阶段不再因 `Cannot find module 'xlsx'` 失败

## 当前状态

completed

## Current Status

completed

## 最终验证

- `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` -> `PASS`
- 根任务补齐真实 `MES_SMOKE_*` 环境变量并恢复本地 Quartz 运行时后，`node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js` -> `PASS`，`smokeRunId=SMART-SCHED-20260622142919`，真实产物位于 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-smart-scheduling-post-p0-gate-refresh\smoke-artifacts\SMOKE-AW-20260622222918\`。

## 完成结论

- 已为真实 smoke 脚本正式声明 `xlsx@0.18.5` 依赖，并同步更新 `pnpm-lock.yaml`。
- 已在 `tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js` 增加依赖合同：脚本若 `require('xlsx')`，则 `package.json` 必须声明该依赖。
- 当前代码侧修复已完成，且已被根任务的真实智能排产 smoke 通过结果再次证明：`xlsx` 缺失不再是业务链路阻塞点。
