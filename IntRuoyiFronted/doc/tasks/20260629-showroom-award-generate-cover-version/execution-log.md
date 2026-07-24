# 执行日志：奖项行内生图并自动发布新版本

BDD: 奖项列表行展示生图入口 -> Given 企宣用户进入奖项列表 / When 行操作区渲染 / Then 当前奖项行显示“生图”按钮并可独立进入加载态。

BDD: 奖项生图成功后自动发布新版本 -> Given 奖项当前封面可读且当前版本中英文语音完整 / When 用户点击“生图” / Then 系统生成新封面、创建并发布新的奖项修订版，列表刷新显示新的 revisionNo。

BDD: 奖项缺少封面或语音时生图失败快报错 -> Given 奖项当前封面缺失或当前版本语音不完整 / When 用户点击“生图” / Then 系统显式失败，不替换封面、不发布半成品版本。

RED: node scripts/showroom-admin-award-generate-cover.test.mjs -> FAIL, 断言只匹配 generate-cover，未覆盖当前实现里的 generateCover emit 命名。

GREEN: node scripts/showroom-admin-award-generate-cover.test.mjs -> PASS

GREEN: node scripts/showroom-admin-award-list.test.mjs -> PASS

GREEN: experience-preflight -> PASS

BDD: AWARD-003 真实页面点击生图后必须看到封面更新与版本递增 -> Given 测试租户登录奖项列表并定位 AWARD-003 / When 点击行内“生图”按钮 / Then 成功提示、图片更新、revisionNo 递增且刷新后仍保持。
