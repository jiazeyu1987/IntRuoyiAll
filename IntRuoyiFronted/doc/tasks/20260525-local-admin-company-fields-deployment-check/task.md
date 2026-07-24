# 任务：确认公司字段修复是否部署到本机芋道源码/admin

## 任务目标

- 使用本机前端 `http://localhost:8081` 的真实登录路径确认 `芋道源码/admin` 是否已可见公司字段修复。
- 只做登录、打开公司信息页和编辑弹窗检查，不保存、不修改数据。

## 非目标

- 不发布、不重启服务。
- 不修改业务代码。
- 不用测试租户或其他账号代替本机 `芋道源码/admin` 验证。

## 前置任务检查

- 最近同仓任务：`20260525-prod-admin-company-fields-deployment-check`。
- 最近任务状态：completed。
- 影响：上一任务已完成，不阻塞本次本机账号确认。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：确认本机 `localhost:8081` 服务可访问。
- [x] M3：使用本机 `芋道源码/admin` 真实前端登录。
- [x] M4：检查 `/showroom/company` 编辑弹窗中文和英文字段。
- [x] M5：记录结论、运行 closeout 预览并提交任务记录。

## BDD 场景

- BDD: 本机账号可见公司完整字段 -> Given 本机前端 `localhost:8081` 使用 `芋道源码/admin` 登录, When 打开 `/showroom/company` 并点击“编辑公司”, Then 中文 tab 应可见“核心制造能力 / 荣誉资质”。
- BDD: 本机账号可见公司英文完整字段 -> Given 本机前端 `localhost:8081` 使用 `芋道源码/admin` 登录并打开编辑公司弹窗, When 切换到 English tab, Then 应可见 `Core Manufacturing Capability / Honors and Awards`，且 `Translate English Content` 入口可见。

## 预期验证

- Playwright 真实前端路径：`http://localhost:8081/login?redirect=/showroom/company`
- 账号：租户 `芋道源码`，用户名 `admin`
- 检查路径：`http://localhost:8081/showroom/company`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-local-admin-company-fields-deployment-check --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一同仓任务完成。
  - 已建立本任务记录。
  - 已确认本机 `http://localhost:8081/` 可访问。
  - 已用 Playwright 全新上下文选择租户 `芋道源码`，使用 `admin/admin123` 登录本机前端。
  - 已确认 `/showroom/company` 编辑公司弹窗中文 tab 可见“核心制造能力 / 荣誉资质”。
  - 已确认 English tab 可见 `Core Manufacturing Capability / Honors and Awards`，且 `Translate English Content` 可见。
  - 已完成 task-closeout-cleanup 预览，无需删除文件。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: 本机 `http://localhost:8081` 的 `芋道源码/admin` 已部署并可见公司完整字段修复。
- PASS: Playwright 真实前端路径检查，未执行保存动作。
- PASS: task-closeout-cleanup preview。

## Cleanup Keep

- `doc/tasks/20260525-local-admin-company-fields-deployment-check/task.md`
- `doc/tasks/20260525-local-admin-company-fields-deployment-check/execution-log.md`
- `doc/tasks/20260525-local-admin-company-fields-deployment-check/verification-report.md`
