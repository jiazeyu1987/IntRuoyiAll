# Task: 批记录纯净预览与纸单质感继续优化

## Goal

在新的 worktree 中继续优化批记录报表的视觉呈现，针对以下四个问题分别收敛：
- 当前看到的是设计器视图，仍带工具栏和侧栏，不是纯净打印视图
- 空白格的 `请填写` 占位符让画面比纸质原图更密
- 右侧一些密集列在当前生成结果里仍然偏挤
- 线框和灰底虽然更稳定了，但和纸质原图还有一点质感差异

## Scope

- 必须使用 4 个并行子 agent，各自负责一个明确问题面
- 主线程仅负责 worktree、任务记录、review、集成验证和收口
- 不允许按模板名加特例分支，继续保持通用规则路线
- 变更完成后，需要重新做 Route B 的真实重生与三张工序页 live 复核

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-three-process-post-commit-compare/task.md`
- Status before this follow-up: completed
- Impact: 上一轮已确认三张工序页继续优化，但仍存在设计器 chrome、占位符密度、密集列宽和纸单质感四类剩余问题

## Milestones

- [x] M1: 4 个子 agent 完成各自责任改动和定向测试
- [x] M2: 主线程 review 并集成改动
- [x] M3: 真实打包、重启 backend
- [x] M4: 真实重生 Route B 并抓取三张最新 live 截图
- [ ] M5: 对图复核、更新证据、收口并提交

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=... test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- 任务目录 `artifacts/` 下生成三张最新 live 截图

## Current Status

Blocked. The backend-side changes and targeted tests are green, and isolated verification on `48082` shows cleaner output than the original designer view, but the target “pure print view with no toolbar chrome” is not fully closed.

## Blocker And Impact

- Blocker 1: `JMReport /view` still renders its built-in top viewer toolbar (`首页 / 上一页 / 打印 / 导出`), so switching away from `/index` removes the sidebars but does not produce a completely chrome-free print surface.
- Blocker 2: the shared `48081` runtime is frequently preempted by external non-worktree backend jars, so stable end-to-end verification had to be isolated onto `48082`.
- Blocker 3: the currently running `8081` admin frontend is served from the external sibling frontend repo outside this worktree, and its page flow still embeds `/jmreport/index/...`, so the worktree backend change alone cannot fully replace the live page path seen through that external frontend runtime.
- Impact: the worktree now contains validated backend-side improvements for preview path, placeholder density, dense tail columns, and paper-like styling, but the final “completely pure print page in the live admin UI” still needs either frontend integration in the sibling frontend repo or deeper JMReport viewer-level override work.
