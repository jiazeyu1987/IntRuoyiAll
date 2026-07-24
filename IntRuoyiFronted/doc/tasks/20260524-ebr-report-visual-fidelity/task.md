# 任务：电子批记录报表视觉保真优化（前端验证）

## 任务目标

- 使用本任务专用本地入口 `http://localhost:18081`，后端指向 `http://localhost:18083`，通过真实用户路径执行 `电子批记录 -> 清除电子批记录报表 -> A 直接 doc`。
- 前端侧仅承担真实路径验证、必要的入口问题修复和证据记录；不得新增测试专用控件或改变页面逻辑来掩盖后端报表问题。

## 工作范围

- 电子批记录页面中已有清除和 `A 直接 doc` 入口的真实 Playwright 验证。
- 如真实入口不可用，仅修复用户可见入口或接口接线问题，并按 BDD + TDD 记录。
- 与后端任务文档保持同一任务编号：`20260524-ebr-report-visual-fidelity`。

## 非目标

- 不修改 Jimu 报表布局算法本身。
- 不绕过真实前端操作改用接口代替清除或生成。
- 不使用 mock 数据，不新增测试隐藏开关。

## 前序任务检查

- 前端上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\worktrees\automation-2-ebr-visual-fidelity-20260524-review\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-live-narration-empty-fix\task.md`
- 状态：`已完成`
- 影响：上一前端任务已完成，当前前端 worktree 干净，不阻塞本任务启动。

## 里程碑

- [x] M1：确认 worktree、前序任务和任务文档；记录 BDD 场景。
- [x] M2：定位电子批记录真实入口和按钮文案。
- [x] M3：使用 Playwright 完成清除与 `A 直接 doc` 真实操作。
- [x] M4：如需前端修复，先补 RED 测试，再最小实现并回归。
- [x] M5：记录验证结果，并配合后端重新生成对比。

## 预期验证

- Playwright 真实路径验证：登录测试租户后进入电子批记录页面，点击 `清除电子批记录报表` 和 `A 直接 doc`。
- 如发生前端代码修改，执行对应定向测试和 lint。

## 当前状态

- 状态：已完成。
- 已完成：worktree 复核、前序任务检查、任务文档创建；真实入口定位；API 解包和错误暴露修复；按用户要求切换到专用前端端口 `18081` 与后端端口 `18083`；真实清空 + A 路重新生成；15 张 Route A viewer 截图采集。
- 阻塞：无影响交付的阻塞。task-closeout-cleanup 已完成 preview，但 apply 因前端主分支 worktree 未检出而阻塞，未删除证据文件。

## 最终验证

- 前端定向测试：`node --test scripts\report-management-six-route-page.test.mjs`、`node --test scripts\electronic-batch-record-jimu-list.test.mjs scripts\electronic-batch-record-open-button-proxy.test.mjs` 通过。
- lint：`pnpm exec eslint src\api\mes\pro\batchrecordreport\index.ts src\views\report\jmreport\index.vue scripts\report-management-six-route-page.test.mjs` 通过。
- 真实路径：前端 `http://127.0.0.1:18081`、后端 `http://127.0.0.1:18083`，Playwright 真实点击清除和 `A 直接 .doc` 通过，并重新采集 15 张 viewer 截图。
