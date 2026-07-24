# Execution Log: 修复展厅主页页签与公司页签报错

BDD: 展厅前台主页与公司页签可稳定切换 -> Given 用户已经进入数字展厅前台入口 / When 在“主页”和“公司”页签之间切换 / Then 页面应加载对应真实数据并完成首屏渲染，而不是抛出字段解析错误或展示加载失败提示。

BDD: 缺失字段必须暴露精确根因 -> Given 展厅前台依赖真实接口返回的数据结构 / When 某个页签缺少必需字段 / Then 系统应报告精确缺失字段和来源，便于修复契约不一致，而不是通过 fallback 隐藏问题。

- M1: In progress. 已创建任务文档，正在收集主页页签与公司页签当前路由、接口与 payload 解析链路。
- M1: Completed. 通过真实登录后的接口复现、`v3/api-docs` 路由清单与前端 API 模块对照，确认“主页 / 公司”页签先命中错误的 `/admin-api/showroom/display/*` 前缀。
- M2: Completed.
- RED: `node --test scripts/showroom-frontstage-runtime.test.mjs` -> FAIL，当前运行时通过前端既有路径访问时，`/admin-api/showroom/display/home` 与 `/admin-api/showroom/display/company` 返回 `No static resource ...`。
- RED: `node --test scripts/showroom-frontstage.test.mjs` -> FAIL，展厅前台 API 模块没有使用 `import.meta.env.VITE_BASE_URL` 直连公开 display 路由，导致请求继承了默认 `/admin-api` 基础前缀。
- M3: Completed. 已将 `src/api/showroom-frontstage/index.ts` 改为通过 `buildDisplayUrl()` 直连 `${VITE_BASE_URL}/showroom/display/*`。
- GREEN: `node --test scripts/showroom-frontstage.test.mjs` -> PASS，前台 API 模块现在显式走公开 display 路由，不再依赖默认 `/admin-api` 基础前缀。
- GREEN: `pnpm exec eslint src/api/showroom-frontstage/index.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs` -> PASS。
- RED: `node --test scripts/showroom-frontstage-runtime.test.mjs` -> FAIL，公开 display 路由已可达，但真实响应变为 `SHOWROOM_TARGET_NOT_FOUND: live company revision not found`；随后补充公司讲解断言后，`/showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` 同样返回 `500`。
- M4: Completed. 依赖后端任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-local-company-live-publish\` 补齐本地公司现行内容与公司讲解 live 数据后，前台运行时恢复。
- GREEN: `node --test scripts/showroom-frontstage-runtime.test.mjs` -> PASS，主页、公司页与公司讲解公开 display 接口均返回 `code=0`。
- GREEN: Playwright CLI real-browser smoke -> PASS，登录后访问 `/showroom/home` 与 `/showroom/company-intro`，页面标题与内容可见，`.showroom-frontstage .el-alert--error` 数量为 `0`。
- M5: Completed. 当前任务文档已更新为完成状态；后续后端任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-live-persistence\` 已消除“公司讲解 live 数据依赖运行中后端进程内存”的残余风险。
