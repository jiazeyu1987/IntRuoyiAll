# 执行日志：展厅产品双语 Tab 真实数据 E2E 验证

BDD: 产品基础信息弹窗真实渲染双语 tab -> Given 真实测试租户用户进入 `http://localhost:8081/showroom/product` / When 用户打开某个真实产品的基础信息弹窗 / Then 页面必须真实显示 `中文 / English` tab，且 English tab 中存在英文名称、英文描述、英文讲解稿、`AI翻译`、`生成语音` 与中英文音频播放器容器。

BDD: 产品详细信息弹窗真实渲染英文高级字段 -> Given 用户在同一路径打开真实产品详细信息弹窗 / When 切换到 `English` tab / Then 页面必须真实显示英文高级字段与 `AI翻译` 按钮。

BDD: 产品列表真实移除单条语音按钮 -> Given 用户查看真实产品列表操作列 / When 页面完成真实接口加载 / Then 列表不应再出现单条 `语音` 按钮。

BDD: 双语交互必须命中真实接口或 fail-fast -> Given 用户在 English tab 点击 `AI翻译` 或 `生成语音` / When 当前运行实例尚未加载后端新契约或接口失败 / Then 验证脚本必须直接报出真实接口状态、真实错误消息或真实前端阻塞，不得伪造通过。

BLOCKED: 初次真实 `snapshot` -> FAIL-FAST，`CompanyWorkbench.vue` 旧模板表达式触发 Vite overlay，`showroom-admin/index.vue` 动态导入失败。

GREEN: 修正 `CompanyWorkbench.vue` 模板解析错误后，真实产品页已可重新加载。

GREEN: 真实产品列表加载 -> PASS，产品页真实表格可见，首屏有 20 条真实数据，首行按钮为 `指派 / 基础 / 详细 / 发布 / 删除`，且无单条 `语音` 按钮。

GREEN: 真实基础信息双语 tab -> PASS，基础信息弹窗真实可见 `中文 / English` tab；English tab 中 `英文名称 / 英文讲解稿 / AI翻译 / 生成语音` 都存在。

GREEN: 真实详细信息双语 tab -> PASS，详细信息弹窗真实可见 `中文 / English` tab；English tab 中 `Registration Certificate / Clinical Effect / FIM Status / AI翻译` 都存在。

GREEN: 真实 `生成语音` -> PASS，手动输入英文讲解稿后点击 `生成语音`，真实返回 `httpStatus=200, code=0`，并在 dialog 中挂载出 `audioCount=2`。

BLOCKED: 真实 `AI翻译` -> FAIL，点击 `AI翻译` 后真实返回 `httpStatus=200, code=500, msg=No static resource admin-api/showroom/product/translate-fields-to-en.`，说明当前运行后端实例未加载新翻译接口。
