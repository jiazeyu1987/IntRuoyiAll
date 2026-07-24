# 任务：DCC 截图需求分析（前端）

- 任务编号：`20260525-dcc-requirements-analysis`
- 创建日期：`2026-05-25`
- 状态：`已完成`
- 仓库：`yudao-ui-admin-vue3`
- Worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-requirements-analysis\yudao-ui-admin-vue3`
- 分支：`task/20260525-dcc-requirements-analysis`

## 任务目标

在 IntRuoyi 前端仓库中记录 DCC 截图需求对后续页面、表单、流程操作、下载提示、发放记录、打印导出和账号交互的影响，配合后端同名任务文档作为后续 UI 实现输入。

## 证据来源

- 用户提供的 DCC 需求截图。
- 后端同名任务分析：`ruoyi-vue-pro/doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md`。
- 前端上一同仓任务 `20260524-ebr-report-visual-fidelity` 已完成。

## 里程碑

- [x] M1：确认 IntRuoyi 前端 worktree 和同名分支。
- [x] M2：创建前端任务文档和 BDD 记录。
- [x] M3：记录后续 UI 影响范围。
- [x] M4：完成 UTF-8 读取、Git 状态检查和提交。
- [x] M5：子 agent 补充前端开发设计并由主 reviewer 放行。

## 预期验证

- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/task.md`
- `git status --short`

## 当前状态

已完成。前端仓库未改生产代码，仅记录 DCC 截图需求对后续前端实现的影响，并补充了子 agent 前端开发设计。

## 前端影响初判

- 文件受控审批表单：源文件上传、图纸 PDF 伴随上传、文件类别、现行有效版本、14 位产品编号、是否需要培训。
- 流程操作区：回退、转交、加签、撤回后删除或重提、会签人选择。
- 待办与消息：展示 `有流程回退，需处理`。
- 下载交互：下载前非受控文件提醒。
- 发放管理：电子发放接收人加签、纸质发放回收记录表格、导出、打印。
- 视图状态：文件 `修改中` 标识。
- 账号交互：密码强度提示和定期强制修改入口。
- 子 agent 前端设计明确复用现有 DCC 页面、BPM 打印/流程能力、系统用户选择器、站内信、下载工具和现有发放/培训入口，不新增独立 DCC 前端体系。

## 阻塞与待确认

- 无阻塞影响本次分析交付。
- 后续 UI 实现前需读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 并按真实用户路径进行 Playwright E2E。

## 最终验证

- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/task.md` -> PASS，中文可读。
- `rg -n "TODO|TBD|fill in later|to be decided" doc/tasks/20260525-dcc-requirements-analysis` -> PASS，无弱占位词。
- `rg -n "BDD:|RED:|GREEN:|Subagent|复用|不得|阻塞" doc/tasks/20260525-dcc-requirements-analysis/frontend-development-design.md` -> PASS，前端设计包含 BDD/TDD/subagent-driven、复用约束和 blocker。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-dcc-requirements-analysis --mode preview --worktree-closeout off --extra-keep doc/tasks/20260525-dcc-requirements-analysis/frontend-development-design.md` -> READY，delete none，blocked none。
- `git status --short` -> PASS，仅包含本任务前端任务文档和前端开发设计文档。

## Cleanup Keep

- `doc/tasks/20260525-dcc-requirements-analysis/frontend-development-design.md`
