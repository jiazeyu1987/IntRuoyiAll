# Execution Log：排产单手动重排真实 E2E 样本可见性复核

- `2026-06-30 任务创建`：建立前端任务文档，准备为排产单手动重排补充真实 Playwright 排查脚本。
- `GREEN: experience-preflight -> PASS`，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `BDD: 真实排产单样本在测试租户可见时可进入手动重排 -> Given 测试租户页面中仍存在目标排产单 / When Playwright 登录并按工单编码筛选后打开手动重排 / Then 脚本应能命中目标排产单并继续验证后续阻塞是否仍为“工单缺少生产用料清单”。`
- `BDD: 真实样本漂移时应显式暴露 -> Given 目标排产单已不在测试租户页面可见范围 / When Playwright 登录并按工单编码筛选目标样本 / Then 脚本应以找不到目标行失败，并把失败点暴露为样本可见性问题。`
- `GREEN: real-e2e-probe-script-added -> PASS`，已新增 `tests/e2e/mes-schedule-order-replan-881mo090863-real-flow.e2e.js`，脚本覆盖真实登录、按工单编码筛选、选择目标排产单、打开手动重排、采集 toast / console / API 响应并判断是否仍命中“工单缺少生产用料清单”。
- `BLOCKER: real-e2e-sample-visibility -> 现有真实执行证据表明脚本失败点已前移为“筛选后 30 秒内找不到 SCH-881MO090863-20260612-0001 对应表格行”，说明当前阻塞属于真实样本/租户可见性漂移，而不是前端脚本或页面逻辑缺失。`
