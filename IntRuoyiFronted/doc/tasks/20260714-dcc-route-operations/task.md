# DCC 流程路线操作面板与新增删除

## Task Goal

在 DCC 流程路线主列表增加顶部新增入口和行级操作面板，支持新增路线、修改当前路线版本、删除当前行路线版本；不引入 fallback、降级、吞异常或权限绕过。

## Milestones

- [x] M1 记录 BDD/TDD 证据与前置门禁。
- [x] M2 增加前端静态契约测试，先证明新增/修改/删除入口缺失。
- [x] M3 实现路线列表新增、修改、删除 UI 与前端 API。
- [x] M4 运行前端静态验证与必要构建检查。
- [x] M5 完成任务证据和收尾记录。

## Expected Verification

- `pnpm e2e:dcc:route-summary:static`
- `pnpm e2e:dcc:routes-list-display:static`
- 新增路线操作静态脚本
- 前端构建或类型检查按项目可用脚本执行

## 经验门禁

- PowerShell/Windows shell：设置 UTF-8 后读取中文文件；写中文使用 `apply_patch` 或 UTF-8 aware runtime；不得使用 `&&`。
- DCC 审核矩阵/审批路线：未经用户批准，不得切换、覆盖或改写 live 审核矩阵版本；本任务只改页面入口与当前行路线版本删除能力。
- E2E/真实写入：如执行真实 E2E，必须仅使用测试租户 `aoteman`，通过 Playwright 真实页面操作；不得 mock、接口或 SQL 绕过。
- No fallback：缺类别、缺节点、缺审批对象、删除不存在路线或预览无法解析必须显式失败。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，复用现有路线表单和标准列表模板，补齐正式 API 与契约测试。
- 是否存在临时补丁或绕过：否。

## Verification Evidence

- RED：`pnpm e2e:dcc:route-operations:static` 初跑失败，缺少删除 API 和操作入口。
- GREEN：`pnpm e2e:dcc:route-operations:static` 通过。
- GREEN：`pnpm e2e:dcc:route-summary:static`、`pnpm e2e:dcc:routes-list-display:static`、`pnpm e2e:dcc:routes-node-columns:static` 通过。
- GREEN：`node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` with `NODE_OPTIONS=--max-old-space-size=8192` 通过。
- BLOCKED：`pnpm build:local` 运行 5 分钟超时；`pnpm exec vue-tsc --noEmit --skipLibCheck` 在默认堆下 OOM，提高堆后暴露既有无关 TS 错误，未命中本任务修改文件。

## Current Status

completed
