# Execution Log：SRM NAS定位通配搜索真实浏览器验证脚本（前端）

- `BDD: 真实浏览器查询 *MO13*.pdf 时直接观察接口结果 -> Given 本机前后端运行正常且账号可登录 / When 脚本在 NAS定位 页面输入 *MO13*.pdf 并点击搜索 / Then 应记录真实接口 payload、页面 toast 与 console/page error，用于判断修复前后的真实表现。`
- `GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md、docs\login-access.md、docs\powershell-memory.md，并确认本轮仅补真实浏览器验证脚本，不修改 NAS定位 页面业务代码。`
- `CHANGE: tests/e2e/srm/nas-locator-wildcard-search-debug.e2e.js，新增真实登录、定位输入框、触发搜索并抓取 /admin-api/srm/nas-locator/page 响应的浏览器级验证脚本。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-wildcard-search-debug.e2e.js -> PASS，修复后真实查询返回 code=0，pageToast=false。`
