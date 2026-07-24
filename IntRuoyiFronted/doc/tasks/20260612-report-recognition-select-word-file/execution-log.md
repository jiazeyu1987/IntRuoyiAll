# Execution Log: 六路识别选择 Word 文件

BDD: 路线按钮选择 Word 文件后按路线解析 -> Given 用户进入 `报表管理 -> 报表设计器 -> 六路识别` 页签 When 点击 A-F 任一路线按钮并选择一个 `.doc` 文件 Then 前端应上传该文件和对应 `routeKey`，后端按该路线解析，成功后提示导入数量并刷新当前路线列表。

BDD: 用户取消或文件不合法不得触发解析 -> Given 用户点击路线按钮 When 文件选择被取消或选择非 Word 文件 Then 前端不调用解析接口，非 Word 文件应提示错误并保持当前列表不变。

BDD: 后端解析失败必须暴露 -> Given 用户已选择 Word 文件 When 后端返回路线无效、解析失败或依赖缺失 Then 页面应展示后端错误，不使用固定样本或默认成功结果。

RED: `node scripts/report-management-six-route-page.test.mjs` -> FAIL, 新增契约断言失败在缺少 `recognizeUploadedRoute`、隐藏文件输入和 `file + routeKey` 上传调用。

GREEN: `node scripts/report-management-six-route-page.test.mjs` -> PASS, 5 个静态契约测试通过，确认前端 API 暴露上传路线解析、页面按钮通过隐藏 `.doc` 文件输入触发上传解析且错误会展示。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, Vue relaxed type check 通过。

GREEN: Playwright 真实路径文件选择器验证 -> PASS, 使用本机 `http://localhost:8081`、测试租户 `测试租户 / aoteman / admin123` 登录，进入 `/report/jimu-report` 的 `六路识别` 页签，点击 `A 直接 .doc` 捕获到 native file chooser，未选择文件、未触发上传写入。

GREEN: frontend feature evidence validation -> PASS, `validate_frontend_feature.py` 确认证据文件结构有效。

GREEN: task-closeout-cleanup preview -> PASS, `task.md`、`execution-log.md`、`frontend-feature-evidence.md` 全部保留，无待删项、无阻塞。
