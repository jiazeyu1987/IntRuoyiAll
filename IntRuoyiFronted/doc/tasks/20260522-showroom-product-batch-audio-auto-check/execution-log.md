# 执行日志：展厅产品一键语音定时续跑（前端）

BDD: 产品列表页加载时读取一键语音自动检查状态 -> Given 企宣用户进入 `http://localhost:8081/showroom/product` / When 页面完成初始化 / Then 前端必须读取后端批量语音自动检查状态，并在工具栏或结果区呈现“定时检查中 / 已停止”等真实状态。

BDD: 一键语音首轮结果展示新增跳过与续跑信息 -> Given 用户点击 `一键语音` / When 首轮批量接口返回 / Then 前端必须展示“跳过已有语音”“跳过缺讲解稿”“是否继续定时检查”“剩余待处理数量”等真实汇总，不得沿用旧的简化统计。

BDD: 工具栏继续保留一键语音入口与当前筛选语义 -> Given 用户在产品列表设置筛选条件 / When 点击 `一键语音` / Then 前端仍使用当前筛选条件发起批量处理，并保留现有 `一键语音` 按钮名称和工具栏布局。

RED: `node --test scripts/showroom-admin-product-list.test.mjs` -> FAIL，现有前端尚未声明自动检查状态契约与新增批量统计字段，也未在页面初始化时读取批量语音自动检查状态。

GREEN: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js` -> PASS，前端已补齐批量语音自动检查状态 API、产品页初始化读取、工具栏状态标签与批量结果补充统计。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，新增批量语音自动检查状态类型和页面状态读取逻辑通过 `vue-tsc`。
