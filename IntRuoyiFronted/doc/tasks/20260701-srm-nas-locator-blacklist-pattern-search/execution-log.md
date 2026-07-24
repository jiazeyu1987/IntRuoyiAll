BDD: 黑名单按钮打开规则弹框 -> Given 用户进入 NAS定位 页面 / When 点击黑名单按钮 / Then 页面弹出黑名单规则编辑弹框，并展示每行一条规则与 *.pyc、*MO13*.pdf 示例。
BDD: 保存黑名单后只提示下次刷新生效 -> Given 用户修改黑名单规则 / When 保存成功 / Then 页面提示“黑名单已保存，刷新索引后生效”，且不自动触发 refresh。
BDD: 搜索框提示保留关键词标签并补充通配说明 -> Given 用户查看 NAS定位 搜索栏 / When 页面渲染 / Then 标签仍为关键词，placeholder 明确支持 *MO13*.pdf 这类通配示例。
INFO: previous-task-blocked -> PASS，前端上一任务已因用户切换需求显式标记为 blocked。
RED: inherited-blacklist-frontend-contract-review -> FAIL, 静态合同在实现前要求存在黑名单按钮、弹框、配置 API、权限 `srm:nas-locator:config` 和通配提示，但页面与 API 尚未具备这些入口。
GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js -> PASS
INFO: frontend-summary -> PASS，页面新增黑名单按钮和 `nas-locator-blacklist-dialog`，接入黑名单读写 API，保存后提示“黑名单已保存，刷新索引后生效”，搜索框 placeholder 补充 `*MO13*.pdf` 通配示例。
