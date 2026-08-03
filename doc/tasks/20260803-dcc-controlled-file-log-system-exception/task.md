# 20260803 DCC 文控日志系统异常修复

## Task Goal

修复文控中心 > 文控日志页面加载时出现“系统异常”的问题，确保主查询失败时显示真实错误，正常数据链路不因历史缺失/孤儿审计记录触发 500。

## Milestones

- [x] 建立任务记录并补齐适用门禁
- [ ] 复现文控日志主查询异常并定位根因
- [ ] 增加失败优先的回归测试
- [ ] 实施最小正式修复
- [ ] 执行目标验证并记录结果
- [ ] 收尾、提交并推送

## Expected Verification

- `node tests/e2e/dcc-controlled-file-logs-static.spec.js`
- 目标后端 JUnit：`DccControlledFileLogQueryServiceTest`
- 如本机运行态满足登录前置，再执行 `node tests/e2e/dcc-controlled-file-logs-real.e2e.js`

## Applied Experience Gates

- `docs/frontend-development.md#前端源码目录与-gitignore-门禁`：文控日志源码目录为正式前端源码，不得因 `logs/` 通用忽略规则导致页面缺失。
- `docs/frontend-development.md#前端延迟辅助加载错误归属门禁`：文控日志主查询失败才允许全局错误；不得吞异常、默认空数据或隐藏真实后端错误。
- `docs/powershell-memory.md#git-indexlock-陈旧锁恢复门禁`：处理 `.git/index.lock` 前必须确认锁文件状态与活动 Git 进程。
- `docs/powershell-memory.md#提交后残余改动复扫门禁`：共享分支存在并发改动时，提交后必须复扫并记录残余归属。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标为修复正式查询链路的 500 根因。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

已完成任务记录和前置门禁读取；下一步复现并定位文控日志主查询系统异常。
