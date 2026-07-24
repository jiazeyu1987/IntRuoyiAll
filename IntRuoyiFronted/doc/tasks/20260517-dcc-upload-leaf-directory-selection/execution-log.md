# Execution Log: DCC 上传目录下钻到叶子目录

BDD: 上传页要求继续选到叶子目录 -> Given 用户已选择某个绑定到多层目录树的文件类别 / When 用户打开上传页并选择目录 / Then 页面必须展示绑定路径并要求继续选到最后一层叶子目录才能提交。

BDD: 浏览页点击父目录可汇总子孙文件 -> Given 用户在目录浏览页选择一个父目录 / When 页面查询受控文件列表 / Then 列表应显示该父目录及其子孙目录中的文件记录。

RED: 初始前端实现检查 -> FAIL，上传页只有文件类别与文件元数据，没有绑定路径展示、叶子目录级联选择，也没有 `directoryId` 提交参数；浏览页请求也未传 `includeDescendantDirectories`。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-leaf-directory-selection run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-leaf-directory-selection\scripts\verify-dcc-upload-leaf-directory-selection.mjs` -> PASS

GREEN: Playwright 真实验证结果 -> PASS
- 绑定路径：`3.DMR/01.图纸`
- 未选叶子目录时表单阻止提交
- 叶子目录提交 payload：`directoryId=4`
- 浏览页父目录查询可看到刚提交记录
