# Execution Log：DCC 受控浏览目录仅显示当前层

BDD: 父目录仅显示直属文件 -> Given 父目录下既有直属文件也有子目录文件 / When 用户点击父目录 / Then 列表只显示父目录直属文件，不显示任何子目录文件。
BDD: 子目录单独显示自身文件 -> Given 用户已经进入父目录 / When 用户继续点击某个子目录 / Then 列表切换为只显示该子目录直属文件。
BDD: 浏览页默认非递归请求 -> Given 用户在当前目录模式查看受控浏览 / When 页面请求列表 / Then 请求只携带当前目录编号，不再显式发送递归目录参数。

INFO: task-created -> 前端任务文档已创建，等待 RED 静态回归与最小修复。
RED: `node tests/e2e/dcc-browser-search-usability-static.spec.js` -> FAIL，旧实现仍在当前目录模式固定发送 `includeDescendantDirectories: true`。
GREEN: `node tests/e2e/dcc-browser-search-usability-static.spec.js` -> PASS
