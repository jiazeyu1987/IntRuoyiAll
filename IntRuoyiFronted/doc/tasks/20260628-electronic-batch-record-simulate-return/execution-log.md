# Execution Log: 模拟填写页来源返回按钮

BDD: 模板说明页进入模拟填写后可返回来源页 -> Given 用户从 eDHR 批次模板说明页点击 模拟填写 / When 进入模拟填写页后点击左上角返回 / Then 页面返回到模板说明页。
BDD: 电子批记录页进入模拟填写后可返回来源页 -> Given 用户从电子批记录表单模板区点击 模拟填写 / When 进入模拟填写页后点击左上角返回 / Then 页面返回到电子批记录模板管理页。
BDD: 模拟填写页显示来源感知返回按钮 -> Given 模拟填写页收到来源页面路由信息 / When 页面渲染头部 / Then 左上角显示返回按钮并使用对应来源页文案。
RED: `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js` -> FAIL，当前模拟填写页缺少来源感知返回按钮，入口也未透传 `returnTo` / `returnLabel`。
GREEN: `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js` -> PASS，两个入口已透传来源页信息，模拟填写页已支持来源感知返回。
GREEN: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS，模拟填写页既有模板加载与新返回合同同时成立。
GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，电子批记录模板管理页入口合同与返回来源页合同同时成立。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-return\frontend-feature-evidence.md` -> PASS。
