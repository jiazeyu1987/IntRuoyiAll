# Verification Report

## Summary

- 已补齐账号锁定/解锁、15 分钟空闲退出和普通用户高权限分配拦截三项控制，并完成定向回归与 `int_main` 真实 E2E。

## Results

- RED: `mvn -pl yudao-module-system "-Dtest=AdminAuthServiceImplTest,AdminUserServiceImplTest,PermissionServiceTest,OAuth2GrantServiceImplTest" test`、`node tests/e2e/system-login-security-idle-logout-static.spec.js`
- GREEN: 同一组后端测试全绿；`node tests/e2e/system-login-security-idle-logout-static.spec.js` 全绿。
- GREEN: `node E:\IntRuoyi\doc\tasks\20260827-login-security-int-main-e2e\system-login-security-real.e2e.js` -> PASS，真实页面验证 5 次失败锁定、解锁恢复、普通用户高权限拦截和 15 分钟空闲退出。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit` 仍被 `src/views/form-center/template/index.vue` 的既有类型错误阻塞，和本任务改动无关。

## Final Status

- completed。
