# 验证报告

## 验证范围

- 结果：当前源码静态复审已形成 14 项问题（3 P1、10 P2、1 P3）与 8 项条件风险/待确认项，逐项包含条件、调用链、影响、修复边界及未来验收要求。
- 已对历史 URB-001 至 URB-017 做原触发点源码复核；其中 URB-006 的 session 合同仍未闭环，映射 URB2-004。没有把历史测试结果当作本轮 PASS。
- 已修改 DCC/前端生产代码和定向测试；未运行 E2E、业务 API、数据库、服务启停或 Git 提交/推送。用户明确授权启用子 Agent。
- 文档验证 PASS：4 份 Markdown UTF-8/无替换字符/无行尾空白；14 个问题编号连续、严重度统计一致、8 个风险编号连续；全部确认问题包含条件、依据、影响、修复边界及 Given/When/Then；源码索引路径存在、引用行号未越界。方法锚点经人工逐项搜索复核。
- 前端静态合同 PASS：`pnpm exec node tests/e2e/dcc-upload-revision-browse-general-fixes-static.spec.js`；`pnpm run ts:check` PASS；后端 `mvn -pl yudao-module-dcc -am "-DskipTests" compile "-Dcheckstyle.skip=true"` PASS；后端定向 `DccControlledFileQueryServiceTest,DccControlledFileFinalizationServiceImplTest` 共 227 tests PASS；`git diff --check` PASS。
- 工作区基线：int_qms / a9bcb6d36d96145ddc1252f111347b644b328deb。既有脏改动未整理或改动。

## 收尾状态

修复和验证完成，已进入 ready_for_closeout；cleanup preview/apply 均 exit 0，keep=4、delete=0、warnings=none。随后按规则记录 blocked。Git 收尾仍受当轮未授权阻塞：AGENTS.md 要求当轮授权才能提交推送，而 docs/task-closeout-rules.md 要求提交推送才能标记 completed，未擅自执行 Git 写操作。

残余风险：最终化失败发生在审批/签名事务内时，审批签名可能随事务回滚；当前 retryStamp 已拒绝缺少完整冻结证据的重试，但尚未建立跨失败事务的审批事实持久化，因此“审批成功、派生失败后可恢复重试”仍需单独设计与验证。

清理命令：`python -X utf8 <个人 skills>/task-closeout-cleanup/scripts/task_closeout.py --task-id 20260929-dcc-upload-revision-browse-reaudit --mode preview`，随后同参数 `--mode apply`。两次均 exit 0；不存在待删除资产，因此无文件删除。

## 源码指纹

以下为本轮交付时工作区文件 UTF-8 解码后统一 CRLF/CR 为 LF、再 UTF-8 编码所得 SHA-256。路径索引与 review-report.md 对应；并非 HEAD blob hash。


| 索引 | SHA-256（UTF-8/LF） |
|---|---|
| U | `71c8e21b367613cd7c3d0a61532418ae8d5a126d8bffe15c891d07997b85cf35` |
| US | `fadf594a121e488f583f97d43d9dcdf7e61cbb0e275e41c017e9d05b4831f28f` |
| B | `7c37756edc1e61a575adeda5e12a1ca285776bf84f72923df7bb79814bfc99a9` |
| D | `40f5babaee2e9ebb25d6496fd0569e62ac3f903e2b932692faa97efb0d0cee82` |
| V | `8e7f4a72c10399c977b23b4cc0afd422132b416a13fbdb2d158cea422d07940e` |
| W | `aca93e11ccb83a6fbd7cbf93d673ffc655c8467f6ac2f34bff42266b6776e0e3` |
| Q | `f2c847ea7e9c44ab51d656873cbbbf8a6c48a1717e3002c290592a901cc20519` |
| F | `49297713ae528d23ae6a4982bbb9d81965122d6aa623dbd41d5209628ce8e284` |
| UP | `f7904be2ca3fa51737213fe6c5abdb7c837714cecbf7b131a803e1649f6d5875` |
| C | `e79dd28ea16499a3ada37dcd4289535525080751aadf60632ef30cd8c59a0ff9` |
| M | `de69f3797b52b97c9b73d9b820c6c3b0304c422dc2163a7e62884e6f01172886` |
| A | `ab4841c61b36178f500186fa227c910a3261762debfb40544898ec1db176cbd3` |
| H | `6986c75eced724a40e4dfe85fc7c9dab50890e2ae969dbae791450edfb13df30` |
| S | `c45f29a9df1eea19c4e172f9093996b956bbdffe0c11aa88bec19f9f4caf29c9` |
| CV | `8f4491cbfb1a26a96bfb29c91c95da2cc1d53b250e441174fa8b1b0ce4152658` |
| POL | `06ef90b74a6075d53184e8e7fa5854ee9f4741b990adcdbddf54c78bd1cdd6e3` |
| SB | `1931c00622a4e2bb82d772169b2313cca19a7bc2326ccf0268ab434ba6c06598` |
| FAIL | `01bef275de04db8685d217733a3706c6865d2ee025f848896b5207b366fc52cc` |
| AD | `de64225b40f81e6474d69fc9a50ae489acbc50001cc97b29bb927e26077e146a` |
| AUTH | `51fff7102f6230b4a4703e3c7528b93ddb477e205979008e0537268d1cb66b9a` |
| DIR | `d9992d00b6167644a9905df5c083c7b611b32b4e237145ad54fa02ef976baa00` |
| CAT | `6274e4e3a15e3dd9533ed5944046594e83023f1ae032f7f26fff145305cbf2c6` |
| BPM | `7cd6d07a2b0a9a28088abdc904cf871fc0f59f1eefe0cedc2b822658a4811de6` |
| BI | `7ee30b204f42af9c0de4e5dc7a64575c7ebf4053637ee27a85bf320839a81479` |
| BL | `63125e2eda5090346f4d8ffa222e81f2db7e7f7f0e90e18c959f6a3203d3ce40` |
| BE | `f2b2d2222c9709f9ef8b9e81742adcf2432f0318372763c43a065602929e7cc4` |
| SIG | `c80ea6581277333113574a55a7fd3c94b5d2eb06c5a0646cf4ae69c1acd0daa1` |
| API | `bfa0b11d1a2aaf8629da008edf8eb94638ca644cc2d8a61f3a52baf8aaa2435e` |
| PA | `af663a0cb52f7db822dcb64265a091ffbb8c100d2b9e08dcde5e31644b8116da` |
| LC | `918f6a6f42edcb6c4919affe1c16567e244a2e25b0777dac9eaae552d3e2609a` |
