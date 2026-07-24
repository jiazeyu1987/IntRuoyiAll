# 展厅产品 BU 改为受限下拉执行日志

- `BDD: 中文 BU 只能从固定 6 项里选择 -> Given 用户打开展厅产品编辑弹窗中文页签 / When 用户编辑 BU 字段 / Then 页面展示下拉选择器且只能选择 6 个合法 BU，不能再自由手填`
- `GREEN: previous-task-check -> PASS, 前一个 frontend 任务 20260623-dcc-browser-batch-recognition 已按真实测试服阻塞显式转为 BLOCKED。`
- `GREEN: experience-preflight -> PASS, 已读取 experience-index 与 FRONTEND_STYLE，本轮仅在前端仓内做静态合同与类型校验。`
- `RED: node tests/e2e/showroom-product-bu-select-static.spec.js -> FAIL, product contracts must expose the fixed BU option list，证明当前中文 BU 仍是自由文本输入且缺少固定 6 项枚举。`
- `GREEN: apply_patch -> PASS, 已新增 SHOWROOM_PRODUCT_BU_OPTIONS，共享字段契约将 pipeline_layout 收敛为 select，并将中文 BU 表单改为固定 6 项 el-select。`
- `GREEN: node tests/e2e/showroom-product-bu-select-static.spec.js -> PASS`
