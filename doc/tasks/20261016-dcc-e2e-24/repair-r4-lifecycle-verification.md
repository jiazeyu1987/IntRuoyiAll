# R4 lifecycle 独立候选修复：RED/GREEN 与 review

## 状态
ready_for_closeout — 定向单测完成，等待Root review；Git收尾按明确禁止保持blocked，不声称completed。主任务仍为20261016-dcc-e2e-24。没有浏览器授权，真实前台验证NOT_RUN。

## BDD
Given deferred start在visible=false时返回并自行stop；When stop在途时visible恢复且调用resume（包括重复调用）；Then旧stop完成后恰好一次新client start，并可正常heartbeat。若最终hidden/disposed不得重启；stop拒绝必须传播且不得重试。既有pending pause恢复路径必须保持。

## 最小改动
生产：IntRuoyiFronted/src/views/dcc/controlled-file/view/reading-lifecycle.ts:66，将`if (starting) return starting`改为`if (starting) return starting.then(resume)`。等待原promise成功完成并清除starting后重新进入既有守卫；active避免重复start，disposed/visible守卫阻止后台重启；拒绝沿promise传播，不执行then恢复。不添加定时器、重试器或状态包装。15秒规则未改。

测试：IntRuoyiFronted/tests/unit/dcc-preview-reading-lifecycle.test.cjs；原setup支持deferred stop和show，新增visible/hidden/disposed三种终态及stop拒绝测试。旧8项保留，包括pending pause恢复、late start/dispose、失败和身份校验。

## 实际命令与结果
工作目录IntRuoyiFronted。

RED: `node --test tests/unit/dcc-preview-reading-lifecycle.test.cjs` → exit1，11 tests /10 pass /1 fail。visible候选实际[start,stop]，期望[start,stop,start,heartbeat]；隐藏和dispose通过。见repair-r4-lifecycle-red.log。

GREEN: 同上 → exit0，11/11 PASS；随后补充stop拒绝保护测试并纳入回归。见repair-r4-lifecycle-green.log。

REGRESSION: `node --test tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-approval-dialog-context.test.cjs` → exit0，61/61 PASS，含最终12项lifecycle。见repair-r4-lifecycle-regression.log。

第一次准备测试的命令因工作目录路径重复未写入目标文件，随后仅执行旧8项并通过；该次是命令准备错误/基线检查，不当作RED或GREEN。真正RED在新增候选后、生产修改前保存。

## Diff与范围
repair-r4-lifecycle.diff为本次编辑前/后对比：按明确编辑操作逆向重建编辑前内容，并非Git HEAD差异；两次只读git diff未返回内容，未据此宣称工作区干净。生产diff仅1行；单测fixture及4用例。这里只改两个授权代码文件及自身任务证据，没有改其他调用方/API/配置。

## 结论边界
独立候选恢复竞争已通过真实单测RED/GREEN确认并最小修复；仍不能证明它是R3 XLSX实际根因，因为原trace缺焦点/可见性事件证据。R3所有首错、DWG精确hash FAIL、SLDDRW停计和XLSX未执行三次切换均保持。未启动浏览器、未构建重启、未写DB/Git、未使用新线程或Agent。当前服务PID仅作为用户提供背景，未启动/停止或运行态探测。

## Review后验证建议
经Root放行后只做定向真实前台：记录focus/blur/visibility与自然start/stop/heartbeat元信息及可见累计值，验证恢复、失焦暂停、隐藏/关闭不重启；不得通过API/响应JSON代替业务断言。浏览器验证待另行安排，本线程停止等待review。
