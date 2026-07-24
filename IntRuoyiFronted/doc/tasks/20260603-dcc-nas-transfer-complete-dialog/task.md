# 任务：NAS 转移全部完成提示弹框

## 任务目标

在 `NAS 管理 -> 转移到 DCC` 弹窗中，用户点击 `确认转移` 并创建后台转移任务后，当前端轮询到任务全部结束时，显示一个明确弹框提示 `全部转移结束`。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-showroom-client-downloads/task.md`
- 状态：`completed`
- 处理：本任务只修改 NAS 管理转移完成提示、对应回归测试和任务文档，不接管其他未提交改动。

## BDD 场景

- BDD: NAS 转移全部完成后显示结束弹框 -> Given 用户已在 `转移到 DCC` 弹窗中点击 `确认转移` 并创建后台任务 / When 前端轮询到任务状态为 `COMPLETED` / Then 页面必须显示弹框提示 `全部转移结束`，同时保留现有任务统计和权限恢复入口。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：先补 RED 回归测试，锁定完成态必须弹出 `全部转移结束`。
- [x] M3：最小修改 NAS 管理完成态轮询逻辑。
- [x] M4：运行目标测试与必要类型检查。
- [x] M5：记录 GREEN 证据并完成收尾。

## Expected Verification

- RED：`node tests/e2e/dcc-nas-transfer-complete-dialog-static.spec.js` 先失败，指出完成态没有弹出 `全部转移结束`。
- GREEN：同一命令通过。
- GREEN：`pnpm ts:check` 或等价目标类型检查通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅在真实完成态展示用户提示，不改变任务状态判定和错误处理。
- `是否从根因和长期维护角度解决`：是。直接在现有终态轮询分支补齐完成反馈，避免新增旁路状态。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已完成工作

- 已建立任务文档，确认上一前端任务已完成。
- 已新增静态回归测试并确认 RED，现有完成态只显示成功消息，没有弹出 `全部转移结束`。
- 已在 NAS 转移轮询 `COMPLETED` 终态分支中增加 Element Plus 弹框 `全部转移结束`，不改变后台任务创建、轮询停止、失败态或权限恢复逻辑。

## 验证结果

- RED：`node tests\e2e\dcc-nas-transfer-complete-dialog-static.spec.js` -> FAIL，缺少 `ElMessageBox.alert('全部转移结束')`。
- GREEN：`node tests\e2e\dcc-nas-transfer-complete-dialog-static.spec.js` -> PASS。
- GREEN：`node tests\e2e\dcc-nas-transfer-resume-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：frontend feature evidence validator -> PASS。
- GREEN：收尾清理预览 -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-dcc-nas-transfer-complete-dialog/frontend-feature-evidence.md`
- `tests/e2e/dcc-nas-transfer-complete-dialog-static.spec.js`
