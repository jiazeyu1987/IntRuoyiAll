# 执行日志：确认公司字段修复是否部署到芋道源码/admin

BDD: 正式账号可见公司完整字段 -> Given 正式环境 `芋道源码/admin` 已登录, When 打开 `/showroom/company` 并点击“编辑公司”, Then 中文 tab 应可见“核心制造能力 / 荣誉资质”。

BDD: 正式账号可见公司英文完整字段 -> Given 正式环境 `芋道源码/admin` 已登录并打开编辑公司弹窗, When 切换到 English tab, Then 应可见 `Core Manufacturing Capability / Honors and Awards`，且 `Translate English Content` 入口可用。

INFO: 已采用 `independent-verification-gate` 工作流。
INFO: 已读取 `docs/server-access.md` 与 `docs/login-access.md`，确认 `芋道源码/admin` 对应正式环境 `http://172.30.30.57:8081`。
INFO: 已确认上一前端任务 `20260525-runtime-control-button-e2e-suite` 状态为 completed。
INFO: 正式前端入口 `http://172.30.30.57:8081/` 返回 200，后端健康检查 `http://172.30.30.57:48081/actuator/health` 返回 UP。
BLOCKED: Browser 正式登录页 `http://172.30.30.57:8081/login?redirect=/index` -> 页面默认显示“测试租户 / aoteman”，搜索/切换 `芋道源码` 后租户下拉无数据，无法完成正式 `芋道源码/admin` UI 登录验收。
FAIL: 正式前端静态产物 `http://172.30.30.57:8081/assets/contracts-BytrMKsU.js` -> 仍包含 `m=o.filter(a=>a.key!=="core_manufacturing_capability"&&a.key!=="honors_awards")`，说明“核心制造能力 / 荣誉资质”过滤修复未部署到正式前端产物。
PASS: 测试服静态产物 `http://172.30.30.58:8081/assets/contracts-QihT9fHN.js` -> 包含 `core_manufacturing_capability / honors_awards` 且无排除过滤条件。
PASS: Playwright `http://172.30.30.58:8081` 使用 `芋道源码/admin` 进入 `/showroom/company` -> 编辑公司弹窗中文 tab 可见“核心制造能力 / 荣誉资质”，English tab 可见 `Core Manufacturing Capability / Honors and Awards`，`Translate English Content` 可见。
FAIL: Playwright `http://172.30.30.57:8081` 使用 `芋道源码/admin` 进入 `/showroom/company` -> 登录后 30 秒仍停留 `http://172.30.30.57:8081/login?redirect=/showroom/company`，页面正文仍显示“测试租户”，无法进入公司页验收。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260525-prod-admin-company-fields-deployment-check --mode preview` -> PASS, keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为空。
