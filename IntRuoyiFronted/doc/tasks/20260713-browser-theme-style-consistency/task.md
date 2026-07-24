# 任务：统一不同浏览器/账号的前端主题样式

## Task Goal

- 修复 Chrome 与 115 浏览器、管理员与普通账号访问同一系统页面时主题、侧栏样式和菜单状态不一致的问题。
- 目标样式以用户本地管理员截图为准：白色侧栏、青绿色主题色、选中菜单浅青背景。
- 保持真实接口、路由、菜单权限和业务数据不变，不通过后端数据、mock 或隐藏错误来制造视觉一致。

## Milestones

- [x] 建立任务记录、技能与经验门禁。
- [x] 定位主题/布局差异来源，包括旧主题缓存和账号菜单缓存。
- [x] 补充回归测试，证明浏览器本地旧主题和旧菜单缓存不会覆盖当前账号状态。
- [x] 最小实现修复，使默认主题与管理员本地样式一致，并使菜单来自当前 token 的实时权限响应。
- [x] 执行目标测试和只读真实浏览器验证。

## Expected Verification

- RED：构造浏览器本地保存旧主题/旧布局/旧用户菜单缓存的场景，当前实现仍会读取旧 `USER` 缓存并吞掉权限接口错误。
- GREEN：修复后登录写入新 token 前会清理旧用户/菜单缓存，进入系统时必须重新请求当前账号权限菜单；旧主题/布局/深色模式缓存被清除。
- REGRESSION：保留菜单权限和真实数据来源，不改变 API 请求参数、后端响应、路由注册或业务权限。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文文件读写显式 UTF-8，不使用 `&&`。
- 前端统一样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；主题统一按本地管理员截图和项目统一前端样式执行。
- 登录/E2E：已读取 `docs/login-access.md`；真实验证只使用本机 `localhost:8081` 和测试租户只读路径，不访问测试服/正式服。
- 真实数据与接口：本任务只改前端状态初始化与缓存边界；未修改后端接口、DTO、数据库、租户数据或 mock 数据。

## 技能适用

- `replicate-frontend-ui`：按用户目标截图复刻真实前端视觉，保护 API/数据契约。
- `bug-regression-fix-loop`：按回归缺陷闭环执行 RED/GREEN 证据。
- `frontend-feature-delivery`：前端行为修复需记录 BDD、测试和验证边界。

## BDD Scenarios

- BDD: 旧浏览器主题缓存不覆盖统一主题 -> Given 浏览器本地保存了旧版暗色侧栏主题 When 用户登录并加载任一管理页面 Then 应显示统一白色侧栏和青绿色选中态。
- BDD: 不同账号菜单权限不影响主题 -> Given 管理员与普通账号拥有不同菜单集合 When 分别访问同一前端入口 Then 菜单项可不同但主题、色彩和基础布局应一致。
- BDD: 旧账号菜单缓存不污染当前账号 -> Given 浏览器本地保存了旧账号 `USER` 与 `roleRouters` When 用户用当前账号重新登录 Then 菜单必须来自当前 token 的 `get-permission-info` 响应。
- BDD: 不改变真实业务数据 -> Given 用户打开 DCC 项目代码或文件查阅页面 When 样式统一后刷新页面 Then API 数据和权限结果仍来自原真实接口。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修复主题初始化、视觉缓存和账号菜单缓存根因，避免单页临时样式补丁。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `tests/output/20260713-browser-theme-style-consistency/`

## Current Status

completed

## Closeout Evidence

- task-closeout-cleanup preview：PASS，无 blocked / warnings。
- task-closeout-cleanup apply：PASS，已清理临时 `bug-regression-evidence.md`、`frontend-feature-evidence.md` 和 `tests/output/20260713-browser-theme-style-consistency/`。
- 保留核心任务记录：`task.md`、`execution-log.md`、`verification-report.md`。
