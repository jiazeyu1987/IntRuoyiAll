# Execution Log: DCC OnlyOffice 只读预览关闭匿名协作姓名弹窗

BDD: 只读预览不提示匿名协作名称 -> Given 用户打开 DCC Office 文件只读预览 / When OnlyOffice DocEditor 初始化 / Then 页面直接进入只读预览，不弹出“输入用于协作的名称”对话框。

BDD: 只读权限保持不变 -> Given 用户打开 DCC Office 文件只读预览 / When OnlyOffice 配置完成 / Then 仍保持 edit/comment/review/download/print/copy 全部关闭。

GREEN: node tests/e2e/dcc-controlled-file-protection.contract.test.js -> PASS.

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS.
