BDD: 类别下有历史名称时可选可不选 -> Given 上传人进入 DCC 上传页并选中文件类别 When 页面加载该类别下的历史文件名称 Then 文件名称控件既能下拉选择历史名称，也能继续自由输入新名称

BDD: 选择历史名称后带出当前版本 -> Given 某历史文件名称存在当前版本号 When 上传人在下拉中选中该文件名称 Then 版本号输入框自动显示该同名文件当前版本号，并保持可编辑

BDD: 清空名称或切换类别时状态重置 -> Given 上传人已选中过历史文件名称并看到了版本号 When 用户清空名称或切换到其他文件类别 Then 历史选项和自动带出的版本号按当前上下文重置，不保留旧类别脏状态

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-name-version-linkage-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-name-version-linkage\scripts\verify-dcc-upload-name-version-linkage.mjs` -> FAIL, the real upload page reached `http://127.0.0.1:8081/dcc/controlled-file/upload` but raised `history_name_picker_missing`, proving the file-name field still had no historical suggestion picker

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS, the upload-page autocomplete and new upload-name API typings compiled cleanly

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-name-version-linkage-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-name-version-linkage\scripts\verify-dcc-upload-name-version-linkage.mjs` -> PASS, real login selected category `产品技术要求`, picked historical file name `DCC-FULL-CHAIN-1778939065187-文件`, and the version input auto-filled `1.0`
