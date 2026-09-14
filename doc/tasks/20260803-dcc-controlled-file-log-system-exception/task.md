# 20260803 DCC 文控日志系统异常修复

## Task Goal

修复文控中心 > 文控日志页面加载时出现“系统异常”的问题，确保主查询失败时显示真实错误，正常数据链路不因历史缺失/孤儿审计记录触发 500。

## Milestones

- [x] 建立任务记录并补齐适用门禁
- [x] 复现文控日志重复系统异常提示并定位根因
- [x] 增加失败优先的回归测试
- [x] 实施最小正式修复
- [x] 执行目标验证并记录结果
- [x] 沉淀可复用经验
- [ ] 收尾、提交并推送

## Expected Verification

- `node tests/e2e/dcc-controlled-file-logs-static.spec.js`
- 目标后端 JUnit：`DccControlledFileLogQueryServiceTest` / `DccControlledFileLogControllerTest`（若编译前置可用）
- 如本机 Playwright 浏览器缓存满足前置，再执行 `node tests/e2e/dcc-controlled-file-logs-real.e2e.js`

## Applied Experience Gates

- `docs/frontend-development.md#前端源码目录与-gitignore-门禁`：文控日志源码目录为正式前端源码，不得因 `logs/` 通用忽略规则导致页面缺失。
- `docs/frontend-development.md#前端延迟辅助加载错误归属门禁`：文控日志主查询失败才允许全局错误；不得吞异常、默认空数据或隐藏真实后端错误。
- `docs/powershell-memory.md#git-indexlock-陈旧锁恢复门禁`：处理 `.git/index.lock` 前必须确认锁文件状态与活动 Git 进程。
- `docs/powershell-memory.md#提交后残余改动复扫门禁`：共享分支存在并发改动时，提交后必须复扫并记录残余归属。
- `docs/frontend-development.md#前端主查询错误重复提示门禁`：页面拥有错误展示归属时，API wrapper 必须设置 `ignoreErrorMessage: true`。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标为修复正式查询链路的 500 根因。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

已修复文控日志请求的重复系统异常提示；实现与验证完成，但共享分支上存在非本任务 `git add -A` 进程占用 `.git/index.lock`，且该进程同时包含本任务文件和多个无关任务文件。为避免混入或中断并发任务，当前阻塞在收尾提交/推送。
