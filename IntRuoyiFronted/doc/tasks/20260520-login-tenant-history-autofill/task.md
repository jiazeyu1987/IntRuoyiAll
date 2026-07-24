# 任务：登录页租户历史下拉与凭据回填

## 目标

在登录页为租户输入增加历史下拉记录；当用户选择某个已成功登录过的租户时，自动回填该租户最近一次成功登录的用户名和密码，便于快速重复登录。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\Login\components\LoginForm.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\utils\auth.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\**`

## 非范围

- 不改后端登录接口、租户解析接口或密码加解密协议。
- 不改手机登录、二维码登录、注册、找回密码流程。
- 不引入 fallback、mock 成功、静默降级或兼容分支。

## 前置任务检查

- 上一个同仓库前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-company-menu-direct-save\task.md`
- 启动前状态：`Blocked on 2026-05-20`
- 影响：上一任务已明确因当前更高优先级请求暂停，且与本次登录页改动文件范围不重叠，可继续处理本次登录增强。

## 里程碑

- [x] M1：检查上一任务状态并创建本次任务文档。
- [x] M2：补登录页真实路径 RED 验证，锁定“租户下拉历史 + 选中后回填凭据”行为。
- [x] M3：实现租户维度的成功登录历史存储与读取。
- [x] M4：实现登录页租户下拉选择与自动回填。
- [x] M5：执行 GREEN 验证、更新证据、预览收尾清理并准备任务提交。

## 预期验证

- `node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\scripts\verify-login-tenant-history.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-history run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\scripts\verify-login-tenant-history.mjs`
- `pnpm exec eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\Login\components\LoginForm.vue D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\utils\auth.ts`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-login-tenant-history-autofill --mode preview`

## 当前状态

Completed on 2026-05-20. 登录页已支持租户历史下拉选择，并可在选择租户后自动回填该租户最近一次成功登录保存的用户名和密码。

## 最终验证结果

- PASS：`pnpm exec eslint D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\Login\components\LoginForm.vue D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\Login\SocialLogin.vue D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\utils\auth.ts D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\hooks\web\useCache.ts`
- PASS：`node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\scripts\verify-login-tenant-history.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-history-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\scripts\verify-login-tenant-history.mjs`
- PASS：`python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-tenant-history-autofill\frontend-feature-evidence.md`
- PASS：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-login-tenant-history-autofill --mode preview`
