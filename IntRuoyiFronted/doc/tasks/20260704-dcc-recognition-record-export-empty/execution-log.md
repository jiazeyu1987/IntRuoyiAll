# 执行日志

- BDD: 当前目录及子目录识别记录可导出 -> Given 用户在当前目录模式下识别当前文件夹及子文件夹 / When 点击导出识别记录 / Then 导出请求必须携带 includeDescendantDirectories=true。
- RED: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:browser-batch-recognition:static` -> FAIL，`buildBrowserRequestParams()` 缺少 `includeDescendantDirectories`。
- GREEN: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 e2e:dcc:browser-batch-recognition:static` -> PASS。
- GREEN: `pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/views/dcc/controlled-file/browser/index.vue tests/e2e/dcc-browser-batch-recognition-static.spec.js --format stylish` -> PASS。
