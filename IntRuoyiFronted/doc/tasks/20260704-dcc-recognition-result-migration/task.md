# DCC 识别结果迁移包前端

## 任务目标

在 DCC 文件浏览页新增识别结果迁移包导出、导入、预览和确认入口，使测试服务器识别结果能通过前端操作导出并在正式服务器导入应用。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。前端展示完整预览状态和失败原因，不隐藏后端不可应用结果。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 前端风格：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用当前 DCC 文件浏览页操作区和弹窗风格。
- BDD/TDD：先扩展静态测试固定按钮、API、导出参数和预览确认契约，再实现页面。
- 真实 E2E：当前仅做本地静态/单元验证；后续若上测试服真实导入导出，需先执行服务器和登录前置门禁。

## 里程碑

1. 前端 RED：扩展 DCC 浏览页静态测试，要求迁移包导出、导入、预览和确认契约存在。
2. 前端 GREEN：实现 API 方法、按钮、上传弹窗、预览表格和确认逻辑。
3. 前端回归：运行 DCC 浏览页静态测试和 ESLint。
4. 前端提交：只提交本任务产生的前端与前端任务文档改动。

## 预期验证

- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:browser-batch-recognition:static`
- `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/views/dcc/controlled-file/browser/index.vue tests/e2e/dcc-browser-batch-recognition-static.spec.js --format stylish`

## 当前状态

- 状态：已完成。
- 已完成：前端 API、导出按钮、导入按钮、上传弹窗、预览统计、逐行失败原因、确认导入。
- 最终结果：DCC 文件浏览页已提供“导出识别迁移包”和“导入识别迁移包”完整入口。
