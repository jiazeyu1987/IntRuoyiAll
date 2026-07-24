# 任务：电子批记录报表视觉保真优化接续（前端验证）

## 任务目标

- 在新前端 worktree 中配合 Automation `automation-2`。
- 使用真实用户路径点击 `清除电子批记录报表`，再点击 `A 直接 doc`，为后端 Jimu 视觉保真优化提供真实生成证据。
- 除非真实入口或接口接线损坏，否则前端侧不修改生产代码。

## 工作范围

- 真实 Playwright 验证和必要证据采集。
- 如入口损坏，仅修复用户可见入口或接口接线问题，并先补 RED 测试。
- 与后端任务编号保持一致：`20260525-automation-2-ebr-visual-fidelity`。

## 非目标

- 不修改 Jimu 报表布局算法。
- 不新增测试专用控件或隐藏开关。
- 不用 API-only 路径替代用户要求的真实点击。

## 前序任务检查

- 前端上一同主题任务：`doc/tasks/20260524-ebr-report-visual-fidelity/task.md`
- 状态：已完成。
- 影响：不阻塞本任务启动。

## 里程碑

- [x] M1：新建前端 worktree 并创建任务文档。
- [x] M2：确认本轮前端入口和后端代理配置。
- [x] M3：使用 Playwright 完成清除与 `A 直接 doc` 真实操作。
- [x] M4：如需前端修复，先补 RED 测试，再最小实现并回归。
- [x] M5：记录真实验证结果，配合后端重新生成对比。

## 预期验证

- Playwright 真实路径验证：登录测试租户后点击清除和 `A 直接 doc`。
- 如发生前端代码修改，执行对应定向测试和 lint。

## 当前状态

- 状态：Round 4 前端真实验证已完成，等待本轮 scoped commit；收尾预览将重新执行，自动清理/合并仍预计受 linked-worktree 条件阻塞。
- 已完成：worktree 创建、前序任务检查、任务文档创建、真实前端清空/生成五轮。
- 当前阻塞：task-closeout-cleanup apply/自动合并阻塞，原因是没有找到前端主分支 `master` 的 checked-out worktree。该阻塞不影响本分支验证证据提交，但影响自动快进合并与删除 worktree。
- 生产前端代码修改：无。
- 任务脚本：本轮使用 task-local Playwright 脚本完成真实 Element Plus 租户下拉选择和清空/生成验证；脚本属于临时验证产物，已在提交前清理。
- Round 2 验证：`2026-05-25 23:03:58` 通过真实前端生成 15 条 Route A Jimu 报表；保留 artifact `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round2-jimu-route-a-summary.json` 作为本轮 DB/Jimu JSON 摘要证据。
- Round 3 验证：`2026-05-25 23:55:39` 通过真实前端生成 15 条 Route A Jimu 报表；保留 artifact `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round3-jimu-route-a-summary.json` 作为本轮固定页脚 DB/Jimu JSON 摘要证据。
- Round 4 验证：`2026-05-26 01:08:38` 通过真实前端生成 15 条 Route A Jimu 报表；保留 artifact `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round4-jimu-route-a-summary.json` 作为本轮续页表头 DB/Jimu JSON 摘要证据。

## Cleanup Keep

- `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round0-jimu-route-a-summary.json`
- `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round1-jimu-route-a-summary.json`
- `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round2-jimu-route-a-summary.json`
- `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round3-jimu-route-a-summary.json`
- `doc/tasks/20260525-automation-2-ebr-visual-fidelity/artifacts/round4-jimu-route-a-summary.json`
