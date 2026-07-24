# 任务：运行控制台隐藏傻瓜式运维顶部错误

## 任务目标

运行控制台加载傻瓜式运维数据超时时，不再在顶部错误条显示 `傻瓜式运维：timeout ...`。运维矩阵自身错误仍可显示在顶部错误条，傻瓜式运维接口失败仍需影响连接状态，不引入 fallback、不吞掉运维动作错误。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-dcc-download-openable-pdf/task.md`
- 状态：`completed`
- 处理：本任务只修改运行控制台页面错误聚合与对应静态回归测试，不接管 DCC 下载任务，也不回滚其他未提交改动。

## BDD 场景

- BDD: 傻瓜式运维超时不进入顶部错误条 -> Given 运行控制台傻瓜式运维数据接口超时 / When 页面聚合加载错误 / Then 顶部错误条不得显示 `傻瓜式运维：timeout ...`，避免与旁侧已有超时提示重复。
- BDD: 运维矩阵错误仍显示 -> Given 运行控制台运维矩阵接口失败 / When 页面聚合加载错误 / Then 顶部错误条仍显示 `运维矩阵：...`，保留关键连接错误提示。
- BDD: 傻瓜式运维失败仍影响连接状态 -> Given 傻瓜式运维数据加载失败 / When 页面刷新完成 / Then 页面连接状态不得被错误标记为完全正常。

## Milestones

- [x] M1：确认用户要求与现有前端任务边界。
- [x] M2：先更新静态回归测试形成 RED。
- [x] M3：最小修改运行控制台错误聚合逻辑。
- [x] M4：运行目标测试并记录 GREEN。
- [x] M5：完成收尾记录。

## Expected Verification

- RED：`node tests/e2e/runtime-control-foolproof-static.spec.js` 先失败，指出页面仍包含顶部 `傻瓜式运维：` 错误上下文。
- GREEN：同一命令通过。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` 通过，确认基础运行控制台契约未破坏。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅调整顶部错误聚合展示，傻瓜式运维接口失败仍影响连接状态。
- `是否从根因和长期维护角度解决`：是。通过明确区分运维矩阵错误与傻瓜式运维辅助数据错误，避免重复提示。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## Current Status

completed

## 已完成工作

- 新增任务级静态回归测试，覆盖顶部错误条不得显示 `傻瓜式运维：...`。
- 调整运行控制台 `loadOverview` 错误聚合：傻瓜式运维加载失败只标记连接状态，不拼接到 `lastError`。
- 同步更新原有运行控制台傻瓜式运维静态契约中的本次断言。

## 验证结果

- RED：`node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> FAIL，页面仍包含 `傻瓜式运维：`，且缺少 `foolproofLoadFailed` 状态标记。
- GREEN：`node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- CHECK：`node tests/e2e/runtime-control-foolproof-static.spec.js` -> 本次断言已通过，但脚本仍因既有无关断言失败：`.env.local` 期望 `8098/48098` 而当前为 `8081/48081`，以及旧 `镜像标签` 文案断言。
- GREEN：`git diff --check` 目标文件 -> PASS，仅 Windows 换行提示。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-runtime-control-hide-foolproof-error/frontend-feature-evidence.md`
- `tests/e2e/runtime-control-hide-foolproof-error-static.spec.js`
