BDD: 无匹配结果提示人工确认 -> Given 后端识别接口返回 `recognitionStatus=NO_MATCH`, When 用户点击识别基础信息, Then 页面提示“识别完成，未匹配到产品名称，请人工确认”并刷新详情。
BDD: 匹配成功仍展示产品信息 -> Given 后端识别接口返回 `recognitionStatus=SUCCESS` 和产品名称/编码, When 用户点击识别基础信息, Then 页面提示已识别基础信息和产品名称/编码。
BDD: 接口异常仍展示失败 -> Given 后端识别接口抛出系统错误, When 用户点击识别基础信息, Then 页面展示基础信息识别失败提示。
RED: pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 run e2e:dcc:project-code-recognition:static -> FAIL，脚本入口缺失，静态契约不可复用。
GREEN: node tests/e2e/dcc-project-code-recognition-static.spec.js -> PASS，静态契约包含 `recognitionStatus`、`NO_MATCH` 和无匹配提示。
GREEN: pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 run e2e:dcc:project-code-recognition:static -> PASS。
GREEN: pnpm.cmd --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/detail/index.vue tests/e2e/dcc-project-code-recognition-static.spec.js --format stylish -> PASS。
