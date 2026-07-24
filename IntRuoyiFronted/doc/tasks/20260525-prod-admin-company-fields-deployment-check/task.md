# 任务：确认公司字段修复是否部署到芋道源码/admin

## 任务目标

- 使用正式环境 `芋道源码/admin` 真实登录路径确认公司信息页是否已部署“核心制造能力 / 荣誉资质”可编辑、可翻译修复。
- 只做查看和打开编辑弹窗，不保存、不修改正式环境数据。

## 非目标

- 不执行发布、重启或回滚。
- 不修改业务代码。
- 不切换到测试租户或其他账号替代正式账号验证。

## 前置任务检查

- 最近前端任务：`20260525-runtime-control-button-e2e-suite`。
- 最近任务状态：completed。
- 影响：上一任务已完成，不阻塞本次部署确认。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：读取服务器与登录基线，确认目标环境和账号。
- [x] M3：使用正式环境 `芋道源码/admin` 真实前端登录。
- [x] M4：检查 `/showroom/company` 编辑弹窗字段是否已部署。
- [x] M5：记录结论、运行 closeout 预览并按需提交任务记录。

## BDD 场景

- BDD: 正式账号可见公司完整字段 -> Given 正式环境 `芋道源码/admin` 已登录, When 打开 `/showroom/company` 并点击“编辑公司”, Then 中文 tab 应可见“核心制造能力 / 荣誉资质”。
- BDD: 正式账号可见公司英文完整字段 -> Given 正式环境 `芋道源码/admin` 已登录并打开编辑公司弹窗, When 切换到 English tab, Then 应可见 `Core Manufacturing Capability / Honors and Awards`，且 `Translate English Content` 入口可用。

## 预期验证

- Browser / Playwright 真实前端路径：`http://172.30.30.57:8081/login?redirect=/index`
- 账号：租户 `芋道源码`，用户名 `admin`
- 检查路径：`http://172.30.30.57:8081/showroom/company`
- 对照路径：`http://172.30.30.58:8081/showroom/company`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-prod-admin-company-fields-deployment-check --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已确认上一前端任务完成。
  - 已建立本任务记录。
  - 已确认测试服 `172.30.30.58:8081` 的 `芋道源码/admin` 路径可见“核心制造能力 / 荣誉资质”和英文对应字段。
  - 已确认正式服 `172.30.30.57:8081` 无法完成 `芋道源码/admin` 登录验证，页面仍停留登录页且租户显示“测试租户”。
  - 已确认正式服前端产物 `assets/contracts-BytrMKsU.js` 仍包含旧过滤逻辑 `core_manufacturing_capability / honors_awards` 排除条件。
  - 已记录 verification report。
  - 已完成 task-closeout-cleanup 预览，无需删除文件。
- 阻塞与影响：
  - 正式服未通过部署验收；不能确认已部署到正式 `芋道源码/admin`。

## Final Verification Result

- FAIL: 正式服 `http://172.30.30.57:8081` 未通过 `芋道源码/admin` 部署确认；登录仍停留登录页，页面租户显示“测试租户”，正式前端产物仍包含旧字段过滤逻辑。
- PASS: 测试服 `http://172.30.30.58:8081` 使用 `芋道源码/admin` 已通过字段可见性验证。
- PASS: task-closeout-cleanup preview。

## Cleanup Keep

- `doc/tasks/20260525-prod-admin-company-fields-deployment-check/task.md`
- `doc/tasks/20260525-prod-admin-company-fields-deployment-check/execution-log.md`
- `doc/tasks/20260525-prod-admin-company-fields-deployment-check/verification-report.md`
