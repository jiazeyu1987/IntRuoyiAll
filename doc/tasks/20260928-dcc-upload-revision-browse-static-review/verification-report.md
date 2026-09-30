# Verification Report

## Result

静态审查文档验证 PASS；17 项确认代码问题（P1=5、P2=12），5 项风险/待确认项。审查对象是当前工作区，不是运行态。所有缺陷均为 open，未修复、未运行复现。

## Checks

- PowerShell 只读结构验证 exit 0：问题编号 URB-001 至 URB-017 连续，风险 URB-R01 至 URB-R05；确认问题均含触发条件、代码依据、影响、修复边界和验收。
- 源码索引 18 个路径全部存在；正文 F/B 标识引用存在，行号未超出文件范围。
- 主审查复读方法调用链、实际自定义 SQL、前端提交与异步回写；补查 AppView 的 route.path key/noCache，排除初稿中不同详情路径必然复用组件的推断。
- URB-011 收窄为同页签名并发分页；URB-012替换为已核对的纸质分发记录读接口缺文件级授权，不保留已排除的初稿问题。
- 没有运行 Maven、TypeScript 检查、业务单测、浏览器或 E2E；无 API/数据库写入、服务操作、生产代码修改。
- 本轮审查完成后停止于 ready_for_closeout；不将文档交付等同于原开发/E2E总任务完成。没有执行 Git 提交/推送或清理 apply。工作区包含大量既有改动，本轮未将其打包提交。

## Source Fingerprints

基线 HEAD：`a9bcb6d36d96145ddc1252f111347b644b328deb`，分支 `int_qms`。下表按 UTF-8 去 BOM、CRLF/CR 规范化 LF 后计算 SHA-256。标识与 review-report.md 源码索引一一对应；指纹不同须复核，不能用旧结论关闭问题。

| 标识 | SHA-256 |
|---|---|
| F1 | `cd76b1d3cbf4f3068bf43b7c7ee87ef9dc5b2fc348ad37136c8c067c878fdfd3` |
| F2 | `fadf594a121e488f583f97d43d9dcdf7e61cbb0e275e41c017e9d05b4831f28f` |
| F3 | `fefe3ff02cb93db6605b2355403ae90c07727421a1defd8c9971a8ce03b5c6bc` |
| F4 | `018eed532a4a24ffcdfa10112eb9e87fd881dce7340ca42b4918ba561c372bc8` |
| F5 | `86bdd91951ce95c307e74b274deba893e26291a284b0ed4ba0d4c6ba4edf1ca2` |
| F6 | `60f3d7c9e7ea4189fb457492f0426b825c306e2baf881bfba4e72d1756a84b7b` |
| F7 | `89e1d6e85a72cc5d315a191015ff9e9a0496000371807c2dcf1deaacfa5c1288` |
| B1 | `269e2ac10e585e00614d0777f10bee1e2573282ff8fe1a07e68e2fec0d00a945` |
| B2 | `d85cf4afcf69869f8a58ad9c11012ca0f4c3f4ec5520a8ae8df41ad4f7dd44f2` |
| B3 | `3349907875c3a7f573143b9a055d96622178680fd23ab3c4ac5e7ca4443e391f` |
| B4 | `4841736414a9a94e0e09661bafa65eaf02483693a21cf12c71cdcd0ef7ed7592` |
| B5 | `d046f1b6118b1caad2af7f8e5b51c897dbe042eaca58ef248c408bb2dceb3b20` |
| B6 | `ab4841c61b36178f500186fa227c910a3261762debfb40544898ec1db176cbd3` |
| B7 | `e79dd28ea16499a3ada37dcd4289535525080751aadf60632ef30cd8c59a0ff9` |
| B8 | `214a5ac837292c13fe1b35ae05b2c6cdbb132b94ec88aefbee2e86839f5409b3` |
| B9 | `01bef275de04db8685d217733a3706c6865d2ee025f848896b5207b366fc52cc` |
| B10 | `4cd3572edd1d763bb1d3da38327e6a56e9711f6db2eaf12e76aa668de3460ebe` |
| B11 | `b25eb06915c3ff9612ac147644a6bc53edb1ea095084fa76e9ff7143d7531973` |
