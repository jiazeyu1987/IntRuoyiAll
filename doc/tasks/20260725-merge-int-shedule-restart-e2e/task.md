# 20260725 Merge int_shedule Restart E2E

## Task Goal

融合 `int_shedule` 最新代码到当前 `int_main`，重新启动本地前后端，并使用 Playwright 真实访问主页。

## Milestones

- [x] M1: 读取合并、端口、本地运行、前后端、E2E、登录和 PowerShell 规则。
- [x] M2: 保存当前脏工作区基线，确保合并前工作区干净且可追踪。
- [x] M3: 拉取 `origin/int_shedule` 最新代码并融合到 `int_main`。
- [x] M4: 构建/重启后端和前端，保持 `8081/48081` 端口契约。
- [x] M5: 使用 Playwright 访问主页并记录真实页面证据。
- [x] M6: 运行端口守卫、记录验证报告和收尾状态。

## Expected Verification

- `git fetch origin int_shedule` 成功，合并源明确。
- 合并后 `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- 后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 前端 `http://127.0.0.1:8081/` 返回 `200`。
- Playwright 通过真实浏览器访问主页并确认页面加载完成。

## BDD Scenarios

- `BDD: merge int_shedule latest -> Given current branch is int_main and origin/int_shedule has latest code, When the branch is merged, Then the working tree contains the int_shedule changes without port-contract drift.`
- `BDD: restart merged runtime -> Given merged code is present locally, When backend and frontend restart on int_main ports, Then backend health is UP and frontend entry returns 200.`
- `BDD: homepage real E2E -> Given local frontend and backend are running, When Playwright opens the homepage/login redirect, Then the page renders through the real frontend route without API-only substitution.`

## Current Status

ready_for_closeout

## Verification Evidence

- Dirty-worktree baseline commit: `0683df11`.
- Task start commit: `9529f908`.
- `origin/int_shedule` fetched and merged; merge commit: `5e8a48b1`.
- Final `origin/int_main` sync reached `126e0e62`, then backend was rebuilt again to match final HEAD.
- Backend build: `mvn.cmd -pl yudao-server -am -DskipTests package` returned `BUILD SUCCESS`.
- Runtime restart: backend `48081` PID `47348` health `UP`; frontend `8081` PID `30732` HTTP `200`.
- Playwright homepage E2E: final URL `http://127.0.0.1:8081/index`, title `瑛泰管理系统 - 首页`, API count `18`, failed API count `0`, console/page errors `0`.
- Screenshot: `E:\IntRuoyi\output\playwright\20260725-int-shedule-final-homepage.png`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 Git 合并、构建、运行态和真实 E2E 完整验证。
- `是否存在临时补丁或绕过`：否。
## Final Verification Result

- PASS: cleanup preview/apply 均通过，删除项、阻塞项、警告项均为 `<none>`。
- PASS: 本次无新增长期经验；已复用现有本地运行、端口和 E2E 门禁。
- PASS: 最终运行态与 Playwright 主页 E2E 均通过。