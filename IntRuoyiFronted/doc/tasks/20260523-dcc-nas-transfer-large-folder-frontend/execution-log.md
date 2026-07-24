# Execution Log：DCC NAS 大目录转移前端联调与验证

BDD: 用户可通过真实 NAS 管理页发起大目录转移 -> Given 用户已通过真实登录进入 `NAS管理` 页并选中一个约 `100` 个文件的 NAS 目录 / When 用户打开 `转移到 DCC` 对话框并确认转移 / Then 前端必须正确提交所选目录、模板类别与生效日期到真实后端

BDD: 转移失败时前端必须暴露真实错误 -> Given 真实 NAS 转移链路在前端或后端任一环节失败 / When 页面收到失败响应或异常 / Then 页面必须显示真实错误信息，不得隐藏、吞掉或伪装成成功

BDD: 修复后用户路径必须能完成真实转移 -> Given 已针对 RED 失败完成最小修复 / When 用户重新通过真实页面执行 NAS 大目录转移 / Then 页面必须展示成功结果摘要，且与后端最终返回保持一致

GREEN: `node --test scripts/system-nas-management.test.mjs` -> PASS

GREEN: `C:\Users\BJB110\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe` + bundled `playwright` 验证脚本 `doc/tasks/20260523-dcc-nas-transfer-large-folder-frontend/scripts/verify-nas-transfer-large-folder-playwright.cjs` -> PASS，真实进入 `http://127.0.0.1:8081/system/nas`，完成 `测试连接 -> 刷新目录 -> 选择 -> 展开 2.DHF/大文控-研发转移项目 -> 勾选 48 气囊式股动脉止血带 PB -> 打开 转移到 DCC 对话框`，且 `确认转移` 按钮可点击

GREEN: 同轮 live 后端实际转移验证 `selectedNasPaths=["2.DHF/大文控-研发转移项目/48 气囊式股动脉止血带 PB"]` -> PASS，`createdFileCount=97`，`failedFileCount=0`
