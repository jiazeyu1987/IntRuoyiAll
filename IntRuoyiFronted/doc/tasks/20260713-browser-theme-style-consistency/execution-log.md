# 执行日志：统一不同浏览器/账号的前端主题样式

## 2026-07-13

- BDD: 旧浏览器主题缓存不覆盖统一主题 -> Given 浏览器本地保存了旧版暗色侧栏主题 When 用户登录并加载任一管理页面 Then 应显示统一白色侧栏和青绿色选中态。
- BDD: 不同账号菜单权限不影响主题 -> Given 管理员与普通账号拥有不同菜单集合 When 分别访问同一前端入口 Then 菜单项可不同但主题、色彩和基础布局应一致。
- BDD: 旧账号菜单缓存不污染当前账号 -> Given 浏览器本地保存了旧账号 `USER` 与 `roleRouters` When 用户用当前账号重新登录 Then 菜单必须来自当前 token 的 `get-permission-info` 响应。
- BDD: 不改变真实业务数据 -> Given 用户打开 DCC 项目代码或文件查阅页面 When 样式统一后刷新页面 Then API 数据和权限结果仍来自原真实接口。
- GREEN: experience-preflight -> PASS, 已读取 `docs/experience-index.md`、`docs/powershell-memory.md`、`docs/login-access.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本任务未执行服务器写入、数据库修改或真实租户写入。
- RED: node tests/e2e/browser-theme-menu-cache-static.spec.js -> FAIL，当前 `setUserInfoAction` 仍从旧 `CACHE_KEY.USER` 读取缓存并空 `catch` 吞掉 `get-permission-info` 失败，可能导致旧浏览器继续显示旧账号菜单。
- GREEN: node tests/e2e/browser-theme-menu-cache-static.spec.js -> PASS，登录写入新 token 前清理旧 `USER` / `roleRouters` / `visitTenantId`，权限菜单必须实时请求当前账号。
- GREEN: node tests/e2e/dcc-browser-admin-style-static.spec.js -> PASS，默认主题、首屏 CSS 和文件查阅管理员样式契约保持通过。
- GREEN: node tests/e2e/dcc-browser-unified-list-template-static.spec.js -> PASS，文件查阅仍接入标准列表模板。
- GREEN: node tests/e2e/user-table-column-config-static.spec.js -> PASS，用户列配置契约未回退。
- GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check -> PASS，TypeScript relaxed 检查通过。
- GREEN: login-preflight -> PASS，真实登录本机 `http://localhost:8081`，租户 `测试租户`，账号 `aoteman`，进入 `/index`。
- GREEN: browser-theme-menu-cache-probe -> PASS，带旧主题/布局/深色/旧账号菜单缓存登录后，实际 CSS 为白底绿主题，旧主题缓存为空，旧账号菜单哨兵值被当前账号权限响应替换。
- GREEN: task-closeout-cleanup preview -> PASS，无 blocked / warnings。
- GREEN: task-closeout-cleanup apply -> PASS，已清理临时证据和 Playwright 输出，保留核心任务记录。
