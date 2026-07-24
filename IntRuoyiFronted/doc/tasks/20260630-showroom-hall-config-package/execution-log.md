# Execution Log：展柜管理数据包导入导出（前端）

- `2026-06-30 任务创建`：建立前端任务文档，锁定展柜页签单入口按钮与 zip API 合同。
- `BDD: 展柜页签显示配置包按钮 -> Given 用户进入展厅后台的展柜管理页签 / When 页面渲染工具条 / Then 可见导出数据包与导入数据包按钮。`
- `BDD: 展柜页签保留单文件选择器合同 -> Given 用户点击导入数据包 / When 页面准备接收配置包文件 / Then 页面通过隐藏文件选择器接收单个 zip 包并触发导入。`
- `BDD: 前端 API 指向正式 hall config package 接口 -> Given 用户执行展柜配置包导出或导入 / When 前端发起请求 / Then 请求命中新后端聚合接口并反馈导入摘要或明确错误。`
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-static.spec.js -> FAIL（补丁前预期）, 原页面缺少展柜配置包按钮、隐藏 zip 选择器和 `/showroom/hall/config-package/*` API 合同。`
- `2026-06-30 实现推进`：已补 `src/api/showroom-admin/index.ts` 的展柜配置包导出/导入 API 与响应类型；已在 `HallListTable.vue` 新增“导出数据包 / 导入数据包”按钮和隐藏 zip 文件选择器；已在 `showroom-admin/index.vue` 接入下载、上传、摘要提示和错误直出；已新增静态门禁 `tests/e2e/showroom-hall-config-package-static.spec.js` 与 `package.json` 脚本。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-static.spec.js -> PASS，展柜页签按钮、隐藏 zip 选择器、导入导出 API 合同与错误直出静态门禁已通过。`
- `GREEN: login-preflight-source-and-target -> PASS，`芋道源码/admin` 与 `测试租户/aoteman` 均可真实进入 `/showroom/hall`，前端入口与权限态满足真实 E2E 前置。`
- `GREEN: real-error-surface-contract -> PASS，真实导入链路已直接拿到后端 `400 SHOWROOM_HALL_CONFIG_PACKAGE_REFERENCE_MISSING` 业务错误；页面不再被通用 `系统异常` toast 抢占。`
