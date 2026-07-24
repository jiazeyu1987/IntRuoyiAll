# Execution Log: 系统管理 NAS 管理页签目录列表（前端）

BDD: 测试成功后允许刷新目录 -> Given 管理员已经测试 NAS 连接成功 / When 页面显示“刷新目录”按钮 / Then 按钮变为可用，点击后调用目录树接口

BDD: 目录树同步展示 -> Given 后端返回 NAS 目录树 / When 点击“刷新目录” / Then 页面在 NAS 管理页签下渲染目录结构

BDD: 未测试成功前禁止刷新目录 -> Given 当前还未测试成功或测试失败 / When 用户查看页面 / Then “刷新目录”按钮保持禁用

RED: node --test scripts\\system-nas-management.test.mjs -> FAIL, 当前页面尚未暴露刷新目录按钮、目录树状态和目录树接口调用

GREEN: node --test scripts\\system-nas-management.test.mjs -> PASS, 2 tests green，已覆盖刷新目录按钮、目录树接口和页面树状态契约

GREEN: node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json -> PASS

GREEN: pnpm exec eslint src/api/system/nas/index.ts src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish -> PASS
