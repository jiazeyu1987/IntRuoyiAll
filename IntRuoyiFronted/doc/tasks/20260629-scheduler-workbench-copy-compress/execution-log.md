# 执行日志：排产工作台设置区文案压缩

- BDD: 班次与策略标题压缩 -> Given 排产员打开工作台设置区 / When 页面渲染设置面板 / Then 标题、副标题与班次说明均使用 4 字以内的简短文案。
- BDD: 策略表单标签压缩 -> Given 排产员查看策略表单 / When 表单渲染 / Then 各字段标签、选项与保护项文案均压缩为 4 字以内且保留原业务含义。
- BDD: 冒烟测试区文案压缩 -> Given 具备冒烟测试权限的排产员进入工作台 / When 冒烟设置区渲染 / Then 区块标题、审批开关和启停按钮文案均使用 4 字以内的简短文案。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js` -> FAIL, 静态契约仍要求旧文案“班次小时设置”。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js` -> FAIL, 静态契约仍要求旧策略标签文案。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-smoke-toggle-static.spec.js` -> FAIL, 静态契约仍要求旧冒烟测试按钮与开关文案。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-density-layout-static.spec.js` -> FAIL, 设置面板断言仍要求旧标签文案。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-shift-hours-static.spec.js` -> PASS
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-policy-settings-static.spec.js` -> PASS
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-smoke-toggle-static.spec.js` -> PASS
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-scheduler-workbench-density-layout-static.spec.js` -> PASS
