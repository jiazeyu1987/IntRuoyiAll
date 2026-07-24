# Execution Log: DCC 上传叶子目录 E2E 复测

BDD: 上传页选择叶子目录后可成功提交 -> Given 用户打开 DCC 受控文件上传页并选择一个绑定到多层目录树的文件类别 / When 用户上传真实 PDF、选择最后一层叶子目录并提交 / Then 系统应成功提交审批并跳转到我的文件页。

RED: 初始状态 -> FAIL，尚未执行真实 Playwright 上传复测，无法确认叶子目录上传链路是否在当前运行时仍然可用。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-leaf-directory-e2e-retest run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-leaf-directory-e2e-retest\scripts\verify-dcc-upload-leaf-directory-e2e-retest.mjs` -> PASS

GREEN: 真实上传结果 -> PASS
- 绑定路径：`3.DMR/01.图纸`
- 叶子目录：`01成品图纸/00- 作废图纸_成品`
- 提交 payload：`directoryId=4`
- 提交响应：`code=0, data=57`
- 成功提示：`受控文件已提交审批`

GREEN: 残余观察
- 运行期间同时出现两条 `系统内部错误` toast
- 控制台存在 `@vite/client` `ReferenceError: document is not defined`
- 该噪声未阻塞本次真实上传成功
