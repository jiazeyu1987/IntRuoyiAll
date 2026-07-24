# Execution Log

BDD: Login page tenant input should match the live backend tenant name -> Given the live backend resolves tenant `芋道源码` and rejects the retired label `瑛泰源码`, When a user opens the login page or reloads a remembered login form, Then the visible tenant input should show `芋道源码`.
RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs` -> FAIL, the script raised `login_default_tenant_mismatch: expected=芋道源码, actual=瑛泰源码`.
GREEN: `node --check D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs` -> PASS.
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default open http://127.0.0.1:8081/login` -> PASS.
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session login-tenant-name-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\login-tenant-name-default.test.mjs` -> PASS, the visible tenant input showed `芋道源码` both on a clean load and after injecting a remembered legacy `loginForm` cache.
GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS.
