# 执行日志：展厅产品基础附件保存与查看修复

## BDD

- BDD: 附件上传后显示可点击文件名 -> Given 用户打开可编辑产品基础弹框 / When 上传图片、视频或文本附件成功 / Then 附件列表显示原始文件名，文件名可点击打开正式文件 URL。
- BDD: 附件保存不因可选字段缺失报错 -> Given 附件记录来自上传接口且可选字段可能为空 / When 用户点击保存草稿或提交 / Then 前端构建 payload 时不读取 undefined.trim，且请求体包含排序后的附件信息。
- BDD: 只读附件仍可查看 -> Given 产品基础弹框不可编辑 / When 附件列表存在文件 / Then 上传、排序和删除不可用，但文件名仍可点击查看。

## TDD 记录

- RED: `node scripts/showroom-product-attachments.test.mjs` -> FAIL，新增附件 URL 契约与 payload 归一化断言未满足。
- GREEN: `node scripts/showroom-product-attachments.test.mjs` -> PASS，6 tests，覆盖上传契约、文件名链接、payload 不再直接调用 `undefined.trim`、只读状态。
- CHECK: `pnpm ts:check` -> FAIL，Node 默认堆限制下 `vue-tsc` OOM，未返回类型错误。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/product/contracts.ts scripts/showroom-product-attachments.test.mjs doc/tasks/20260606-showroom-product-attachment-save-preview-fix` -> PASS。
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-attachment-save-preview run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260606-showroom-product-attachment-save-preview-fix\scripts\verify-product-attachment-preview-save.mjs` -> FAIL，登录选择器修正后进入产品页，但点击“基础”抛出 `展柜公司信息缺失，无法设置产品归属`；根因是产品页未加载 `companyCurrent`。
- RED: `node scripts/showroom-admin-product-company-field-layout.test.mjs` -> FAIL，新增断言要求 `shouldLoadCompanyCurrent` 覆盖 `product` 分区。
- GREEN: `node scripts/showroom-admin-product-company-field-layout.test.mjs` -> PASS，3 tests，产品页会加载 `companyCurrent`，基础弹框可使用瑛泰医疗固定归属。
- RED: `node scripts/showroom-product-attachments.test.mjs` -> FAIL，新增断言要求 `uploadProductAttachment` 解包 `request.upload` 的 `response.data`；真实页面已复现文件名与大小显示为 `undefined`。
- GREEN: `node scripts/showroom-product-attachments.test.mjs` -> PASS，6 tests，附件上传 helper 返回正式 payload，上传后文件名、大小、URL 可用于弹框展示。
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-attachment-save-preview run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260606-showroom-product-attachment-save-preview-fix\scripts\verify-product-attachment-preview-save.mjs` -> PASS，真实测试租户 `测试租户/aoteman` 打开产品基础弹框，上传文本附件 `product-attachment-preview-fixture.txt` 后文件名可见，点击命中 `/admin-api/infra/file/28/get/showroom/product-attachments/20260606/product-attachment-preview-fixture.txt`，保存草稿成功，重开详情返回附件 `url`。
- GREEN: `node scripts/showroom-admin-product-company-field-layout.test.mjs; node scripts/showroom-product-attachments.test.mjs; $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/product/contracts.ts scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-product-attachments.test.mjs doc/tasks/20260606-showroom-product-attachment-save-preview-fix` -> PASS。
