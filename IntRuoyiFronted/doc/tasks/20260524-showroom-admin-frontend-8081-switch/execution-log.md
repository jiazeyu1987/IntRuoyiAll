# 执行日志：切换 8081 到当前展厅管理前端

BDD: 8081 应加载当前最新展厅管理前端 -> Given 用户访问 `http://127.0.0.1:8081/showroom/company` / When 本机 `8081` 前端进程切换到当前源码目录启动 / Then 页面应不再来自旧 worktree，而是加载当前最新前端

BDD: 端口切换只影响 8081 前端 -> Given 后端 `48081` 已正常运行 / When 切换 `8081` 前端进程 / Then 后端和其他无关端口不得被一起重启或修改

INVESTIGATION: 2026-05-24 -> 已确认当前 `8081` 占用进程来自 `D:\ProjectPackage\Int\IntRuoyi\worktrees\automation-2-ebr-visual-fidelity-20260524-review\...`，不是当前 `yudao-ui-admin-vue3` 源码目录。
GREEN: 停止旧 worktree `8081` 前端并重新启动当前前端 -> PASS。
GREEN: `netstat -ano | findstr :8081` -> PASS，当前监听实例已切换。
GREEN: `Invoke-WebRequest http://127.0.0.1:8081/login` -> PASS，返回 `200`。
