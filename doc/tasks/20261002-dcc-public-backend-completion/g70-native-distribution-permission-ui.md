# G70 原生文控下发权限真实UI配置

## Current Status

ready_for_closeout — 真实UI新增菜单605071339、精确doc_control910233追加授权及正常重开确认已完成。Root在创建前更正parent为当前启用文控中心6800，原6808已退役不可用；未动旧6808/6819/角色/用户。已UI_FREE、owned浏览器session78118关闭exit0；凭据内存，无生产/SQL/API/mock/BPM操作。这里配置PASS，不替Root实际下发工作台/业务E2E。

## BDD and evidence

Given Root已读当前permission节点0且admin有doc_control、类别USER1DISTRIBUTE，When真实菜单页新增准确enabled按钮后真实role菜单页追加新节点，Then重开菜单核parent/type/name/permission、重开role保原selected列表加新节点。原缺项为配置前置，不假写产品RED或接口Success=全业务PASS。

先观察 `/system/menu` 与 `/system/role`，锚定准确row/弹框。自然请求只method/path/status，不解析业务JSON为oracle、不存token/header/storageState；最终截图/DOM证明保存回显。仅最终截图，不称逐步trace。

## 实际保存与回显

稳定本机8061/48061由Rootowned BE6952/attempt16提供。fresh Playwright实际登录芋道源码/admin。系统菜单页搜索唯一文控中心，实际V2 DOM rowkey=6800，同行新增；只在未提交form将总是显示设不是、缓存设不缓存，再选“按钮”，名称文控下发、permission精确distribute、sort99/开启，未填page/path/icon/component。自然POST `/admin-api/system/menu/create` 200（018.json），实际DOM新ID=`605071339`，正常“修改”重开显示parent文控中心、type按钮、name、permission、sort99、开启（023.json）。Root随后独立只读核exactflags/空字段，未由本Agent解析API响应冒证明。

角色页 `/system/role?roleId=910233`正常打开准确doc_control行，菜单权限弹框核name/code。关闭父子联动，不点全选；展开只是读树。原checked/halfChecked各DOM集合033.json保存，6800已有halfChecked祖先、不另授6819/categorymanage。只新增605071339。

首次原checkbox input操作后保存自然POST200，但重开039.json新节点仍未选、原授权合集没变，**该次不是PASS**，Rootgrant0也明确佐证，原截图/状态保留。随后点击实际可见该data-key节点的label.el-checkbox，inspect确认is-checked，再填G70原因、保存自然POST `/admin-api/system/permission/assign-role-menu` 200（042.json）。正常重开48，文控下发真实checkbox checked=true；033与048 DOM原checked或half集合对比唯一新增605071339，0删除/0额外新增。没有以toast或200单独称完成，也未强制checkbox/API注入。

最终只取消重开的只读dialog、关闭本人browser并发UI_FREE，Root可停止更新其ownedBE。未给已ordinary的910328恢复doc_control、未改任何其它role/菜单/旧退役parent。Root另做数据库差集和fresh登录工作台下发验收，本Agent不代做。

## 保护证据

`C:/IntRuoyiBackups/20261006-dcc-task-actor-config/g70-ui-r1`：49个可见DOM/input/自然method-status JSON、5张截图，54asset rawbytes/SHA封存于 `g70-native-distribution-ui-receipt.json` SHA `30e418acfc66b756486a9a3972ba32c2bd84c2325ae4a25841f21223d18dfd48`。

- 菜单最终：`023.json` / `native-button-final.png`；newID由actualDOM rowkey观察。
- role原集合：`033.json` / `doc-control-menu-before-add.png`。
- 首次不成功保留：`039.json` / `doc-control-distribute-final.png`，不得作为最终PASS截图。
- 最终重开：`048.json` / `doc-control-distribute-verified-final.png`，真实可见checkbox已选，final截图已view_image检查。

脚本 `g70-native-permission-ui.cjs` SHA `47981203d50ca490ba62b6eeb0091ee24e5e2398ee14e25fdf89aad12c3a1c21`，node --check0；复用既有AGENTS内存登录，每次只通过真实UI。秘密无文件/trace/storageState/headers；原输入隐藏radio控件timeout作为locator事实保留，不写成产品失败。资源已释放，不新增回归或业务操作。
