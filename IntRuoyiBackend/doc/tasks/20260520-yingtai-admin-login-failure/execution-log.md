# Execution Log: 排查瑛泰医疗租户 admin 登录失败

BDD: 瑛泰医疗租户管理员登录 -> Given 当前本地系统存在租户 `瑛泰医疗` 且用户尝试使用 `admin / admin123` 登录 When 通过真实租户解析与认证链路执行登录 Then 必须明确该组合失败的真实原因，并给出可复核的账号存在性与认证结果
INFO: 已核对上一任务 `20260520-showroom-product-codex-bilingual-narration` 的 `task.md`，状态为 `Completed`。
GREEN: `SELECT t.id AS tenant_id, t.name AS tenant_name ... LEFT JOIN system_users u ON u.tenant_id = t.id AND u.username = 'admin' WHERE t.name = '瑛泰医疗'` -> PASS，确认 `tenant_id=162` 的 `瑛泰医疗` 租户存在，但不存在 `username='admin'` 的用户记录。
GREEN: `SELECT tenant_id, COUNT(*) ...; SELECT id, username, nickname, status, deleted FROM system_users WHERE tenant_id = 162 ...` -> PASS，确认 `tenant_id=162` 下仅有 `910201 / yingtai / 瑛泰医疗管理员` 这一条有效用户。
GREEN: `GET /admin-api/system/tenant/get-id-by-name?name=瑛泰医疗` -> PASS，返回 `162`。
RED: `POST /admin-api/system/auth/login` with header `tenant-id=162` and body `admin / admin123` -> FAIL，返回 `登录失败，账号密码不正确`。
GREEN: `POST /admin-api/system/auth/login` with header `tenant-id=162` and body `yingtai / admin123` -> PASS，返回 `userId=910201` 和有效 token。
RED: `npx --yes @playwright/cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-yingtai-admin-login-failure\scripts\playwright-yingtai-login-check.js` 中 `瑛泰医疗 / admin / admin123` 登录尝试 -> FAIL，前端控制台抛出 `登录失败，账号密码不正确`。
GREEN: 同一 Playwright 脚本中的 `瑛泰医疗 / yingtai / admin123` 登录尝试 -> PASS，页面进入 `http://localhost:8081/index`，标题为 `瑛泰管理系统 - 首页`。
GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-yingtai-admin-login-failure\bug-regression-evidence.md` -> PASS。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-yingtai-admin-login-failure --mode preview` -> PASS，preview 结果 `ready`。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-yingtai-admin-login-failure --mode apply` -> PASS，已清理任务临时脚本与中间证据文件，仅保留正式记录。
