# 20260601-unocss-entry-module-not-found

## 任务目标

修复前端开发服务器提示 `[unocss] Entry module not found. Did you add import 'uno.css' in your main entry?` 的问题，确保 UnoCSS 样式入口被 Vite 正确识别，开发页不再出现该错误遮罩。

## 前置检查

- 上一前端任务 `doc/tasks/20260601-showroom-product-import-timeout/task.md` 状态为 `completed`。
- 当前工作区存在既有未提交改动，本任务只读取并保护，不纳入本次修改范围。

## BDD 场景

### Scenario: Vite development entry loads UnoCSS

Given 前端开发服务器加载 `src/main.ts`
When Vite 和 UnoCSS 插件解析应用入口
Then UnoCSS CSS 入口必须可被插件识别
And 页面不应显示 `[unocss] Entry module not found` 遮罩。

## 里程碑

1. 复现并定位 UnoCSS 入口解析失败原因。
2. 增加失败优先的回归检查，覆盖入口文件必须直接声明 UnoCSS 入口。
3. 最小化修复入口声明。
4. 运行目标验证并记录结果。

## 预期验证

- `node scripts/check-unocss-entry.mjs` 先失败再通过。
- 相关构建或开发入口验证不再出现 UnoCSS entry module missing 报错。

## 当前状态

status: completed

已完成修复、回归检查、真实浏览器验证和证据校验。

## Current Status

completed

## 完成记录

- 代码修复：`src/main.ts` 直接导入 `uno.css`，删除 `src/plugins/unocss/index.ts` 间接入口。
- 回归检查：新增 `scripts/check-unocss-entry.mjs`，防止主入口再次改为间接 UnoCSS 引入。
- 验证结果：`node scripts/check-unocss-entry.mjs` 通过；Playwright 分别验证 `http://127.0.0.1:8098/` 与 `http://127.0.0.1:8081/` 等待 25 秒均未出现 UnoCSS entry missing overlay；bug regression evidence 校验通过。
- 剩余阻塞：无。

## Cleanup Keep

- `doc/tasks/20260601-unocss-entry-module-not-found/bug-regression-evidence.md`
