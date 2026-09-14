# 表单解析一线预览隐藏顶部信息卡

## Goal

按用户截图要求，隐藏表单解析一线生产预览顶部红框内的产品信息卡、员工信息卡和主页按钮，只保留工序切换区域。

## Milestones

- [x] M1: 确认现有表单解析页面结构、静态合同和任务范围
- [x] M2: 先补充静态合同 RED，锁定红框内元素不再渲染
- [x] M3: 修改表单解析一线预览顶部布局
- [x] M4: 运行静态合同、类型检查和差异检查
- [x] M5: 记录验证证据并提交任务范围代码

## Expected Verification

- `node tests\e2e\form-parser-json-download-static.spec.cjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `git diff --check`

## Design Constraints Check

- 只修改表单解析页的一线生产预览展示和对应静态合同。
- 不修改后端解析接口、JSON 数据结构、权限、菜单和数据库。
- 隐藏红框内顶部展示，不影响 JSON 编辑、应用、下载、工序切换、物料和设备参数展示。

## Current Status
completed

Implementation, targeted verification, cleanup, and local commit are complete.
