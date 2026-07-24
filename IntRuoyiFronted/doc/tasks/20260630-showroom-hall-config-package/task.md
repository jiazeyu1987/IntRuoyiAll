# 任务：展柜管理数据包导入导出（前端）

- Task ID: `20260630-showroom-hall-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在展厅后台 `展柜管理` 页签增加“导出数据包 / 导入数据包”单入口，直接接入后端正式 zip 配置包接口，用于迁移展柜配置、关键词、预览图与语音。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-dcc-admin-full-config-package\task.md`
- 状态：`blocked`
- 处理说明：已因用户优先级切换到展柜配置包需求而显式阻塞；本次仅在展厅后台页签与 API 合同内推进。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 静态测试、任务文档与日志统一 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实 E2E 验收前必须先跑官方登录预检。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 维持展厅后台现有白底紧凑运营台风格，只在展柜工具条增补按钮与文件选择器。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；前端直接命中正式配置包接口，不拼接多个展柜/关键词/语音接口。
- `是否存在临时补丁或绕过`：否。导入失败需直接展示后端错误。

## BDD 场景

- `BDD: 展柜页签显示配置包按钮 -> Given 用户进入展厅后台的展柜管理页签 / When 页面渲染工具条 / Then 可见导出数据包与导入数据包按钮。`
- `BDD: 展柜页签保留单文件选择器合同 -> Given 用户点击导入数据包 / When 页面准备接收配置包文件 / Then 页面通过隐藏文件选择器接收单个 zip 包并触发导入。`
- `BDD: 前端 API 指向正式 hall config package 接口 -> Given 用户执行展柜配置包导出或导入 / When 前端发起请求 / Then 请求命中新后端聚合接口并反馈导入摘要或明确错误。`

## Milestones

1. M1：建立前端任务文档并锁定按钮/API 合同。`completed`
2. M2：补 RED 静态检查。`completed`
3. M3：实现展柜工具条按钮、隐藏文件选择器与 API 接入。`completed`
4. M4：运行静态验证并回填 evidence。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-static.spec.js`

## Current Blockers

- 无。此前目标租户主数据缺失导致的真实导入阻塞已解除；前端真实链路已完成 source->target 回导闭环并通过。

## Completed Work

- 已在展柜管理工具条新增“导出数据包 / 导入数据包”按钮与隐藏 zip 文件选择器。
- 已接入 `/showroom/hall/config-package/export` 与 `/showroom/hall/config-package/import` API 合同。
- 已实现 zip 后缀校验、导入摘要提示与后端结构化错误直出。
- 已按 `showroom_publicity` / `super_admin` 控制前端按钮可见性。

## Verification Evidence

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-static.spec.js` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /showroom/hall --target-text 展柜管理 --timeout 90000` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /showroom/hall --target-text 展柜管理 --timeout 90000` -> `PASS`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-real.e2e.js` -> `PASS`，已完成 `芋道源码/admin` 导出 -> `测试租户/aoteman` 导入 -> `测试租户` 回导 -> `manifest/asset hash` 深比较闭环。
- 真实运行态摘要：`hallCount=10`、`keywordCount=34`、`previewAssetCount=10`、`narrationCount=20`、`backgroundAssetCount=0`、`validatedProductCount=164`、`validatedAwardCount=46`。
