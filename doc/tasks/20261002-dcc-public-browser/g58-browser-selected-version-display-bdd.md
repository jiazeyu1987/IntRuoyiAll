# G58 AC23 — 库列表所选版本与当前执行版本分开显示

Status: ready_for_closeout_for_Root_review。共享任务保持in_progress。Root实际r7/031检入得到A/1-1工作小版本、锁释放。库列表所选正文为该工作版，但文件名称/编号摘要固定标“当前执行受控版本”，版本摘要依据所选ID是否列表行ID二分，误标“历史版”。真实证据路径由Root保管，本Agent只读json事实。

Given正式所选版本status=WORKING/versionNo=A/1-1，同master的currentActiveVersionNo=A/1，When实际展示helper及两处摘要renderer执行，Then所选为“工作小版本 A/1-1”，当前执行受控版本独立显示正式master A/1，不将工作版称受控或历史。

Given所选为在途申请、受控待生效、ACTIVE或历史OBSOLETE/SUPERSEDED，When摘要显示，Then阶段来自所选actualstatus；只有ACTIVE且versionNo与正式master.currentActiveVersionNo相同才标“当前执行受控版本”。缺master执行事实显示未记录，不从ACTIVE/列表所选身份推断当前，也不改变数据、版本选择或操作ID。

有限源仅browser/index.vue两摘要caller/版本摘要caller与browser/presentation.ts展示helper；专属真实helper/AST getter/实际模板renderer测试。原getVersionOptions/getSelectedVersion/检出检入/查询/wrapper/动作guard不改。G58 category2源、G57 Parent/native队列、Root working-navigation保持既有封存。先实际旧源码RED→GREEN，有限checkin操作回归与2prodlint；Root最后统一types/build及真实库页面验。本Agent无UI/API/DB/服务/Git/Maven/fulltypes/build。

有效RED：原生产3项均失败（g58-browser-selected-version-display-effective-red.log，exit1），准确为WORKING误历史、原getter没有所选阶段、两处原模板固定执行受控标签。首轮renderer卸载v-for片段时，宿主nextSibling恒null造成挂起；核精确所属child21112后仅终止该测试，原session96720/log保留，不作为业务RED。测试宿主补真实parent/sibling/remove关系后有效RED419ms完成，生产尚未改。

实施：getBrowserVersionSummary直接使用所选actualstatus显示工作小版本/草稿/在途/受控待生效/历史作废或替换；未知属性明确未记录。当前执行标记必须ACTIVE且与caller显式传入的row.currentActiveVersionNo相等，不从所选自己的current字段默认补齐、不猜ACTIVE执行身份。两处metadata显示同helper所选阶段、所选版本号及Master当前执行版本单独事实；缺Master显示未记录。原file动作ID、查询、列表选择、检入生成工作版本逻辑均未改。

最终3文件20项PASS、0fail/skip、exit0（g58-browser-selected-version-display-final-frozen.log）；2生产ESLint --max-warnings 0实际exit0、无输出（lint-frozen.log）。其中新3项使用正式helper、实际index AST getter及两处模板Vue renderer，其他17项为既有检入/申请入口回归，执行不累加先前批次。保留首GREEN和lint日志，最后版本为final-frozen/lint-frozen。

Source/test冻结，g58-browser-selected-version-display-fingerprints.json只封本2源/1test；G58独立类别权限和G57r2全部原字节pin保持。Root统一全types/build及真实页面复验待执行，本Agent未实际页面/HTTP/DB/服务/Git，不用离线PASS代真实业务验收。
