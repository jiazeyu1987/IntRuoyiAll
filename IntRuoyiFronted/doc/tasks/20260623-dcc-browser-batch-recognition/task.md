# 任务：DCC 受控浏览页批量识别产品名称编号

## 任务目标

在 DCC 受控浏览页增加批量识别产品名称/编号能力，支持按当前浏览上下文创建服务端异步识别任务，并显示实时进度、覆盖策略和最终结果。

## 当前状态

BLOCKED

## Current Status

BLOCKED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-dcc-batch-recognition-browser\doc\tasks\20260623-test-randomized-erp-order-create\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成，不阻塞本次 DCC 浏览页批量识别前端实现。

## 经验门禁

- 已读取：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务适用强制门禁：
  - 前后端仅在当前成对 worktree 中修改，不把主工作区或其他任务残留混入本次提交。
  - 浏览页按钮、弹窗、表格和进度展示继续沿用 IntPP 紧凑操作台风格，不做无关视觉重构。
  - 前端必须真实暴露任务状态与后端错误，不得用 mock、默认成功、静默 catch 或 fallback 掩盖识别失败。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是；复用后端异步任务接口，在浏览页建立可持续的批量识别入口与进度反馈。
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 文控角色可见批量识别按钮 -> Given 文控角色进入 DCC 受控浏览页 / When 页面渲染查询操作区 / Then 刷新列表右侧显示批量识别产品名称/编号按钮，非文控角色不可见。`
- `BDD: 当前目录未选中时阻断创建 -> Given 当前范围为当前目录且左侧未选中目录 / When 用户点击批量识别按钮 / Then 页面提示请先选择目录，不创建任务。`
- `BDD: 创建任务后显示确认上下文与默认覆盖策略 -> Given 用户已处于可识别的浏览上下文 / When 点击批量识别按钮 / Then 弹窗展示范围、目录、筛选条件、默认仅识别空值，并固定说明成功后会同步 fileName/title/productName/productCode。`
- `BDD: 存在活动任务时直接回到进度 -> Given 已有 WAITING 或 RUNNING 的批量识别任务 / When 用户再次点击批量识别按钮 / Then 页面直接打开该任务进度，不重复创建任务。`
- `BDD: 前端进度展示真实统计 -> Given 批量任务运行中 / When 前端轮询任务状态 / Then 页面显示总数、已处理、成功、失败、跳过、剩余、当前状态与最后错误，并在完成后自动刷新列表。`

## 里程碑

1. 建立前端任务台账与静态契约。`COMPLETED`
2. 实现浏览页按钮、确认弹窗和进度弹窗。`COMPLETED`
3. 接入批量识别 API 与轮询状态同步。`COMPLETED`
4. 运行静态验证、类型检查并补齐证据。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-browser-batch-recognition-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 本地完成结果

- 浏览页查询区已新增 `批量识别产品名称/编号` 按钮，仅 `doc_control` 角色可见。
- 已接入批量识别创建/查询 API，支持活动任务复用、进度轮询、关闭弹窗后后台继续执行，以及任务完成后自动刷新当前列表。
- 已新增静态契约测试，锁定按钮位置、请求参数、进度字段与“请先选择目录”阻断行为。

## 本地核验

- `node tests/e2e/dcc-browser-batch-recognition-static.spec.js` -> PASS
- `pnpm install` -> PASS；当前前端 worktree 原先缺少 `node_modules`，安装时暴露坏锁文件并自动修复 `pnpm-lock.yaml`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-dcc-batch-recognition-browser\doc\tasks\20260623-dcc-browser-batch-recognition\verify-dcc-browser-batch-recognition-admin.e2e.mjs` -> PASS
  - 真实运行态：`http://127.0.0.1:8087 -> http://127.0.0.1:48087`
  - 真实账号：`芋道源码/admin/admin123`
  - 目录样本：`质量管理/3.DMR/10.产品技术要求`，总数 `24`
  - 任务结果：`success=7 / failed=17 / skipped=0`
  - 数据库核验：7 条成功记录已写回 `product_name / product_code / dcc_project_code_id / project_code_recognition_type`

## 剩余阻塞

- 还未将本地前后端改动发布到测试服务器，也未执行真实浏览页联调。
- 当前前端 worktree 的 `pnpm-lock.yaml` 因坏锁文件前置问题产生 diff；在提交前需决定是否把锁文件修复纳入本次提交，或在干净依赖环境重放验证。
- 2026-06-24 测试服已完成两轮真实发布验证，但“内容识别全部跑通”仍未成立：
  - `release-20260623-dcc-batch-recognition-test-v1`：测试服真实批量任务 `33/33` 全失败，错误从页面/UI 层定位到后端仍在走 `cmd.exe /c codex.cmd`。
  - `release-20260624-dcc-batch-recognition-codex-v2`：测试服 backend 已拿到 `DCC_PROJECT_CODE_CODEX_CLI_COMMAND=/opt/intruoyi/runtime/tools/codex` 与 `CODEX_HOME=/opt/intruoyi/runtime/backend-codex-home`，但同一目录真实任务 `33/33` 仍未成功，当前错误演化为 `Codex CLI timed out after 120 seconds` 与 `returned no DCC basic-data match`。
  - 结论：测试服“批量识别入口 + 异步任务 + 进度统计 + Linux Codex 启动链路”已打通，但“内容识别结果可用”仍被 Codex 超时/无匹配阻塞。
- 本地样本目录中仍有 `17` 条记录因源文件对象缺失而失败，最后错误为 `S3 404 The specified key does not exist`；这是样本数据/文件存储前置问题，不是按钮、任务调度或进度弹窗链路阻塞。

## 阻塞影响

- 当前任务已具备“按钮、进度与运行链路”本地完成度，但测试服真实内容识别仍被 `Codex CLI timed out after 120 seconds` 与“无基础数据匹配”阻塞，未满足完整联调收口条件。
- 在该阻塞解除前，前端仓不能把本任务继续视为 `IN_PROGRESS` 的可直接收尾状态；本轮已按仓库规则显式转为 `BLOCKED`。

## 用户额外授权

- 2026-06-23：用户明确选择方案 `2`，允许在本机使用 `芋道源码/admin` 执行本次写入型 DCC 批量识别 E2E，用于替代缺少 `doc_control` 的测试租户账号阻塞。
