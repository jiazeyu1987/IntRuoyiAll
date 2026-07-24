# 任务：DCC 受控浏览默认显示最新版本

## 目标

将 DCC `受控浏览` 页面调整为“每个文件默认仅显示最新版本”，并允许用户通过版本下拉切换查看该文件的历史版本；切换后详情、预览、下载等操作必须作用于所选版本。

## 非目标

- 不修改 `我的受控文件` 页面当前按提交记录展示的语义。
- 不改动 DCC 详情页已有版本历史表结构。
- 不引入 mock 数据、兼容分支或前端假分页。

## 前置任务检查

- 最近前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\task.md`
- 启动前状态：已于 2026-05-19 显式标记为阻塞，原因是当前线程切换到新的 DCC 受控浏览需求。
- 影响：旧任务源码边界与本任务无直接重叠，可独立推进。

## 范围

- `src/views/dcc/controlled-file/browser/**`
- 新增或更新受控浏览页定向前端回归脚本
- 本任务目录下的执行记录与证据

## 里程碑

- [x] M1：完成任务建档、读取现有页面与接口语义。
- [x] M2：补前端 RED 测试，证明当前页面直接按版本逐行展示且没有版本下拉。
- [x] M3：实现浏览页“默认最新版本 + 版本下拉切换”交互，并保持现有样式基线。
- [x] M4：运行前端 GREEN 验证并补齐证据。
- [x] M5：执行收尾预览，准备仅包含本任务改动的提交。

## 预期验证

- `node --test scripts/dcc-controlled-browser-version-selector.test.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-dcc-browser-latest-version-default/frontend-feature-evidence.md`
- 若本地前端入口可用：从 `http://localhost:8081` 进入 DCC 受控浏览，使用真实路径复核“默认最新版本 + 版本下拉切换历史版本”。

## 当前状态

已完成。

## 写入边界

- `src/views/dcc/controlled-file/browser/**`
- `scripts/dcc-controlled-browser-version-selector.test.mjs`
- `doc/tasks/20260519-dcc-browser-latest-version-default/**`

## 风险与约束

- 页面必须继续遵守 Int 统一前端列表页风格，不额外引入装饰性重构。
- 版本切换不能依赖假分页或静默拉大页大小的绕行方案。
- 若后端未提供浏览页所需聚合语义，必须显式通过后端真实参数支持，而不是在前端吞掉分页问题。

## Final Verification Result

- PASS：`node --test scripts/dcc-controlled-browser-version-selector.test.mjs`
- PASS：`$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-browser-latest-version-default open http://127.0.0.1:8081/login?redirect=%2Fdcc%2Fcontrolled-file%2Fbrowser`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-browser-latest-version-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-browser-latest-version-default\scripts\verify-dcc-browser-latest-version-default.mjs`

## Cleanup Keep

- doc/tasks/20260519-dcc-browser-latest-version-default/frontend-feature-evidence.md
- doc/tasks/20260519-dcc-browser-latest-version-default/scripts/verify-dcc-browser-latest-version-default.mjs
