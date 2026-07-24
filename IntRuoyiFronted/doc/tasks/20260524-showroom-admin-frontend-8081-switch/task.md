# 任务：切换 8081 到当前展厅管理前端

## 任务目标

- 将本机 `http://127.0.0.1:8081` 当前占用的旧 worktree 前端，切换为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 当前最新源码启动的前端。
- 保证用户后续在 `8081/showroom/company` 看到的是当前已提交的展厅按钮与权限修复结果。

## 非目标

- 不修改后端代码或数据库。
- 不修改前端源码逻辑；本次只切换运行中的前端进程。
- 不清理无关 worktree 或其他运行中的端口，除非它们直接占用 `8081`。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-manual-release-super-admin-visibility\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成；当前只做本机前端运行环境切换，不阻塞继续执行。

## 里程碑

- [ ] M1：确认当前 `8081` 占用进程和来源 worktree。
- [ ] M2：停止占用 `8081` 的旧前端进程。
- [ ] M3：从 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 启动最新前端到 `8081`。
- [ ] M4：验证 `8081/showroom/company` 已加载当前前端。

## 预期验证

- `netstat -ano | findstr :8081`
- 进程命令行应指向 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- 真实页面 `http://127.0.0.1:8081/showroom/company`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已确认原 `8081` 被旧 worktree `automation-2-ebr-visual-fidelity-20260524-review` 前端占用。
- 已停止旧 worktree 的 `8081` 前端实例。
- 已将 `8081` 重新切换为从 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 启动的当前前端。

## Final Verification

- `netstat -ano | findstr :8081` -> PASS，`8081` 已重新监听。
- `http://127.0.0.1:8081/login` -> PASS，返回 `200`。
