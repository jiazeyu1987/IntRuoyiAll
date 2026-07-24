# 任务：排查瑛泰医疗租户 admin 登录失败

## Goal

排查 `http://localhost:8081` 上使用租户 `瑛泰医疗`、用户名 `admin`、密码 `admin123` 登录失败的问题，确认根因，并在需要代码修复前明确真实阻塞点与影响。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-yingtai-admin-login-failure\**`
- 当前运行中的本地前后端登录链路核对
- 当前运行库 `127.0.0.1:23306/ruoyi-vue-pro` 中租户与用户只读核对

## Non-Scope

- 不直接修改租户、用户或密码数据，除非用户明确要求。
- 不引入 fallback、mock、兼容分支或静默降级。
- 不顺带处理与本次登录失败无关的其他系统问题。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\task.md`
- Status before this task: completed
- Impact: 无，当前仓库可继续处理新任务。

## Milestones

- [x] M1: 确认上一任务状态并创建本次任务文档/执行日志。
- [x] M2: 复核当前运行库中的 `瑛泰医疗` 租户与管理员账号数据。
- [x] M3: 通过真实登录链路复核失败现象并记录证据。
- [x] M4: 输出根因、影响与下一步建议。

## Expected Verification

- 明确当前运行库中 `瑛泰医疗` 的租户编号、账号列表和目标账号状态。
- 明确 `admin/admin123` 失败的真实原因。
- 若存在可用账号，明确其登录结果。

## Current Status

Completed.

## Final Result

当前运行库中的 `瑛泰医疗` 租户（`tenant_id=162`）存在，但该租户下没有 `username='admin'` 的用户记录；因此 `瑛泰医疗 / admin / admin123` 登录失败不是前端或认证代码拦截，而是账号前置数据不存在。该租户当前唯一有效管理员用户为：

- `910201 / yingtai / 瑛泰医疗管理员`

已通过后端正式认证接口和 Playwright 前端真实登录路径确认：

- `瑛泰医疗 / admin / admin123` -> 失败，返回 `登录失败，账号密码不正确`
- `瑛泰医疗 / yingtai / admin123` -> 成功，可进入首页

## Impact

- 若继续使用 `admin` 作为 `瑛泰医疗` 的登录账号，后续人工测试、E2E 和联调会持续失败。
- 当前若只需要进入系统，可直接改用 `瑛泰医疗 / yingtai / admin123`。
- 若业务上必须保留 `admin` 账号，需要单独执行账号创建或账号重置任务。

## Final Verification Result

- PASS: `GET /admin-api/system/tenant/get-id-by-name?name=瑛泰医疗`
- PASS: `system_users` 只读查询确认 `tenant_id=162` 下仅存在 `yingtai`
- PASS: `POST /admin-api/system/auth/login` with `tenant-id=162`, `yingtai / admin123`
- PASS: Playwright 真实登录 `瑛泰医疗 / yingtai / admin123`
- FAIL（按预期暴露问题）: `POST /admin-api/system/auth/login` with `tenant-id=162`, `admin / admin123`
- FAIL（按预期暴露问题）: Playwright 真实登录 `瑛泰医疗 / admin / admin123`
- PASS: `validate_bug_regression.py --evidence ...\20260520-yingtai-admin-login-failure\bug-regression-evidence.md`
- PASS: `task_closeout.py --mode preview`
- PASS: `task_closeout.py --mode apply`

## Blockers

- 运行库账号前置数据缺失：`瑛泰医疗` 租户没有 `admin` 用户。
