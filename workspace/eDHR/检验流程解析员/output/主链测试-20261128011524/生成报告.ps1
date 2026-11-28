$ErrorActionPreference='Stop'
$run=$PSScriptRoot
$base='本地 http://localhost:8081，测试1租户，panxin账号，eDHR批记录，检验表单，'
$data=@'
已通过|环境和基线|ID与IDE|核对账号、现有版本和关联|身份及目标与授权一致|ID起始G/10，九道工序十九项，关联初包装A/10和大中包装A/7；IDE为B/2，关联A/7和A/1|检验规程，分别选择ID及IDE，查看版本和关联|1795800052362
已通过|真实分页|ID正式规程|每页十条，进入第二页|第二页九项，翻页状态正确|总数十九项，第二页九项，上一页可用、下一页不可用|检验规程，ID，检验项目，每页十条，第二页|1795800195516
已通过|无结果筛选|ID正式规程|输入不存在的工序，再清空|空表后可恢复全部记录|无匹配时空表，清空后恢复十九项|检验规程，ID，工序包含输入不存在工序20261128，再清空|1795800445838
已通过|错误文件拦截|检验编制|导入错误文件.docx|说明文件无效，不产生可发布空规程|中文提示文件损坏或无效，保存、导出和预览不可用|检验编制，选择ID，导入输入文件夹内错误文件.docx|1795800723943
已通过|有效文件恢复与发布拦截|ID|改导有效文件，预览发布|恢复解析，设备缺失时拦截|恢复九道工序十九项，八处缺少设备时不能确认发布|检验编制，ID，导入ID.docx，预览发布|1795800928622
已通过|设备补齐和失败恢复|ID|逐项选择页面真实设备|缺项清零后可正常发布|八处绑定完成，问题清零，随后真实发布G/11|检验编制，ID，八处设备依次选C01017、FM-E-24、FM-E-24、AL150158、FM-E-24、气源、G01143、FM-E-24，预览并发布|1795806102064
已通过|新增修改和排序|ID清洗工序|新增临时项、改名、上移|数量和位置正确变化|清洗由一项变两项，总数十九变二十，临时项改名并从第二项移至第一项|检验编制，ID，清洗，添加项目，改名主链临时项目，项目上移|1795805617831
已通过|删除取消和确认|临时项目|先取消删除，再确认删除|取消保留，确认删除|取消后仍在，确认后总数恢复十九，清洗恢复一项|检验编制，ID，清洗，临时项目，删除后取消，再删除并确认|1795805617831
已通过|普通编辑撤销|IDE|在外观后输入临时文字，再逐字撤销|回到原名称|输入临时撤销验证，连续撤销六次恢复外观；未保存或发布IDE|检验编制，IDE，导入IDE.docx，首个外观项追加临时撤销验证，连续撤销六次|1795811945360
已通过|重复保存和刷新恢复|ID编制草稿|连续保存两次，刷新后恢复|内容相同，无重复项|恢复九道工序十九项，首项外观本轮验证，接收限0.650；此时尚未完成最终设备补齐|检验编制，ID，改名及接收限，保存两次，刷新，恢复草稿|1795804232755
已通过|编制发布|ID G/11|补设备，预览并确认发布|生成正式版本|提示发布成功，正式规程打开G/11，保留本轮改名和接收限|检验编制，ID，补齐设备，保存，预览，确认无需末检，确认发布，查看结果|1795806367434
已通过|正式草稿不覆盖旧版|ID G/11及G/12|修改后保存草稿，再回看旧版|新草稿变化，旧内容不变|生成G/12草稿；G/11仍为外观本轮验证、接收限0.650|检验规程，ID G/11，首项改外观本轮正式升版，接收限1.000，保存草稿，回选G/11|1795806761373
已通过|正式新版本发布|ID G/12|发布草稿并刷新|新版本真实生效|刷新后当前已发布为G/12，改名和接收限1.000保留|检验规程，ID，选择G/12草稿，发布并确认，刷新|1795806985526
已通过|通用导入保存预览发布|大中包装 A/8|导入大中包装.docx，保存并发布|生成可选正式通用版本|一道工序三项，发布成功为A/8|通用编制，导入大中包装.docx，保存，预览，确认无需末检，确认发布|1795807379364
已通过|通用导入保存预览发布|初包装 A/11|导入初包装.docx，保存并发布|生成可选正式通用版本|一道工序四项，发布成功为A/11|通用编制，导入初包装.docx，保存，预览，确认无需末检，确认发布|1795807713274
已通过|通用导入保存预览发布|美联初包装 B/5|导入美联初包装.docx，保存并发布|生成可选正式通用版本|一道工序四项，发布成功为B/5|通用编制，导入美联初包装.docx，保存，预览，确认无需末检，确认发布|1795808029653
已通过|关联指定通用版本|ID G/13|选初包装A/11、大中包装A/8并发布|保存指定版本，刷新不丢失|G/13刷新后仍关联这两个指定版本|检验规程，ID，通用关联，选初包装A/11及大中包装A/8，保存并发布，刷新|1795808765501
已通过|通用规程升版|大中包装 A/9|修改名称，保存预览发布|生成新正式通用版本|名称增加本轮1128升版，真实发布A/9|通用编制，导入大中包装，名称增加本轮1128升版，保存，预览并发布|1795809138322
已通过|旧关联不自动升级|ID G/13|通用升版后重开产品关联|仍锁定原通用版本|仍为大中包装A/8，没有自动替换为A/9；名称变化另列存疑|检验规程，ID，选择G/13，通用关联，查看大中包装版本|1795809251365
已通过|主动改绑新通用版本|ID G/14|主动改选大中包装A/9并发布|新产品版使用新版通用规程|G/14刷新后关联初包装A/11及大中包装A/9|检验规程，ID G/13，通用关联，改选大中包装A/9，保存并发布，刷新|1795809668574
已通过|历史关联保留|ID G/13|G/14发布后回看G/13|旧关联版本不变|历史G/13仍为初包装A/11、大中包装A/8|检验规程，ID，选择G/13，通用关联|1795809887170
已通过|历史内容和抽样保留|ID G/11与G/14|分别查看首项名称和抽样|旧版保留旧内容，新版保留新内容|G/11为外观本轮验证、0.650；新版为外观本轮正式升版、1.000；设备异常另列未通过|检验规程，ID，分别选择G/11及G/14，检验项目，清洗首项|1795806767434
已通过|改绑前后十九项不变|ID G/13与G/14|逐项比较页面字段、选项和设备|只改关联，不改项目内容|十九项的页面内容及八个设备编号一致，比较摘要保存在本次记录中|检验规程，ID，分别选G/13和G/14，每页一百条，对照十九行内容|1795810407545
已通过|通用导出回导逐项对照|大中包装三项|真实导出，再导回，逐项比较|字段、选项和开关不丢失|三项各十六个输入字段及选项开关均一致|通用编制，导入大中包装，导出Word，再导入本轮导出文件，逐项查看三项|1795811390456
已通过|产品导出回导完整对照|ID十九项|恢复已保存G/11，导出回导后再次导出比较|除自动新版本号外内容全部保留|九道工序十九项、八处设备及其他保存字段一致；仅预备版本从G/11变G/15，未发布G/15|检验编制，ID，恢复草稿，导出Word，导回该文件，再导出，比较两份本轮文件保存内容|1795816321782
已通过|产品导入解析|IDE|选择对应产品并导入|展示工序、项目和缺项|七道工序二十八项，十二处待修正；仅验证解析展示，未保存发布|检验编制，选择IDE，导入IDE.docx，查看工序树和待修正问题|1795811656405
已通过|产品导入解析|IDI|选择对应产品并导入|展示工序、项目和缺项|六道工序二十二项，八处待修正；仅验证解析展示，未保存发布|检验编制，选择IDI，导入IDI.docx，查看工序树和待修正问题|1795812184327
已通过|产品导入解析|IDML|选择对应产品并导入|展示工序、项目和缺项|十道工序二十七项，九处待修正；仅验证解析展示，未保存发布|检验编制，选择IDML，导入IDML.docx，查看工序树和待修正问题|1795812454471
已通过|产品导入解析|IDPR|选择对应产品并导入|展示工序、项目和缺项|十道工序二十七项，九处待修正；仅验证解析展示，未保存发布|检验编制，选择IDPR，导入IDPR.docx，查看工序树和待修正问题|1795812699117
已通过|重置引用保护|ID G/14|确认执行测试重置|存在生产引用时明确拒绝且不删除|提示未执行：生产订单引用二十三条、PQC任务引用三百五十四条，本次未删除任何规程或版本；刷新仍为G/14|检验规程，选择ID当前G/14，测试重置，确认，查看结果，再刷新|1795812978771
已通过|非目标数据保持|IDE和三份通用规程|重置被拒绝后逐项复查|原版本及关联保持|IDE仍B/2，关联A/7和A/1；三份通用当前仍A/11、A/9及B/5；实际删除范围未获验证|检验规程，IDE，通用关联；通用规程，依次选择初包装、大中包装、美联初包装|1795813509517
已通过|详情解析目标正确|IDE|从IDE正式详情打开解析|默认仍为IDE，不串为ID|弹窗目标为IDE；未上传和提交，随后取消|检验规程，选择IDE B/2，解析，核对所属项目，取消|1795813632515
未通过|历史设备显示缺失|ID历史 G/11|发布后续版本，再回看历史设备|历史应显示发布时的八处设备绑定|G/11八处均提示待绑定，撤压项不显示C01017；当前G/14同项正常。只能确认历史页面显示异常，尚不能认定底层记录丢失|检验规程，ID，先打开G/14检验项目查看撤压设备C01017，再切G/11，表格向下及向右滚动至设备列，对照八处设备，多次重新读取仍未显示|1795810678135
存疑|无引用目标成功清空分支|ID G/14|测试重置受到引用保护|无引用时才能验证成功清空和实际删除范围|因二十三条订单及三百五十四条PQC任务引用，重置未执行，不能据此判定成功清空分支通过或失败|检验规程，ID G/14，测试重置，确认，查看引用拦截；后续需获授权且无引用的明确目标，再测试成功清空及非目标对照，不擅自删除引用|1795812978771
存疑|旧关联名称随新版变化|ID G/13及IDE B/2|通用名称升版后查看旧关联|旧版名称是否锁定需要明确业务口径|版本仍锁定A/8或A/1，但大中包装名称显示本轮1128升版。尚不确定是最新名称展示规则还是历史展示缺陷|先在通用编制修改大中包装名称并发布A/9，再到检验规程选择ID G/13及IDE B/2的通用关联，对照旧版本号和最新名称|1795809251365
'@
$cases=@($data -split '\r?\n' | Where-Object {$_} | ForEach-Object {
 $fields=$_ -split '\|'
 if($fields.Count -ne 8){throw "记录字段数不正确：$_"}
 $image="$($fields[7]).png"
 if($fields[7] -eq '1795806767434'){$image='1795806761373.png'}
 [pscustomobject]@{Sheet=$fields[0];Node=$fields[1];Target=$fields[2];Operation=$fields[3];Expected=$fields[4];Actual=$fields[5];Limit='按本次实际页面判断，不与原文逐句核对。';Path=$base+$fields[6];Image=$image}
})
$cases|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $run '本次测试记录.json') -Encoding utf8
$commands=[System.Collections.Generic.List[object]]::new()
foreach($sheet in @('已通过','未通过','存疑')){$commands.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
$commands.Add(@{command='remove';path='/Sheet1'})
$headers=@('编号','测试环节','测试对象','操作方法','应有结果','实际结果','核验范围','完整测试路径','证据截图')
$columns=@('A','B','C','D','E','F','G','H','I')
$widths=@(9,24,24,36,36,54,32,68,80)
foreach($sheet in @('已通过','未通过','存疑')){
 $rows=@($cases|Where-Object Sheet -eq $sheet)
 $commands.Add(@{command='set';path="/$sheet";props=@{freeze='D3';autoFilter="A2:I$($rows.Count+2)"}})
 $commands.Add(@{command='set';path="/$sheet/A1:H1";props=@{merge='true';value="2026年11月28日本地主链复测，测试1，潘鑫。$sheet $($rows.Count)项。全链未通过：历史设备显示异常；无引用目标的成功清空分支受阻。";fill='EAF0F6';bold='true';wrapText='true'}})
 $commands.Add(@{command='set';path="/$sheet/row[1]";props=@{height='42'}})
 for($index=0;$index -lt 9;$index++){
  $commands.Add(@{command='set';path="/$sheet/col[$($columns[$index])]";props=@{width=[string]$widths[$index]}})
  $commands.Add(@{command='set';path="/$sheet/$($columns[$index])2";props=@{value=$headers[$index];fill='264663';'font.color'='FFFFFF';bold='true';wrapText='true'}})
 }
 $rowNumber=3
 foreach($case in $rows){
  $imagePath=Join-Path $run "截图\$($case.Image)"
  if(-not (Test-Path -LiteralPath $imagePath)){throw "缺少本次截图：$imagePath"}
  $values=@([string]($rowNumber-2),$case.Node,$case.Target,$case.Operation,$case.Expected,$case.Actual,$case.Limit,$case.Path)
  for($index=0;$index -lt 8;$index++){$commands.Add(@{command='set';path="/$sheet/$($columns[$index])$rowNumber";props=@{value=$values[$index];font='微软雅黑';fontSize='11';wrapText='true';valign='top';'font.color'='202B38'}})}
  $commands.Add(@{command='set';path="/$sheet/row[$rowNumber]";props=@{height='242'}})
  $commands.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=$imagePath;anchor="I${rowNumber}:J$($rowNumber+1)";alt="本次实际页面证据：$($case.Node)"}})
  $rowNumber++
 }
}
$inputPath=Join-Path $run '报告生成指令.json'
$commands|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $inputPath -Encoding utf8
$workbook=Join-Path $run ('report\'+(Get-Date -Format 'yyyyMMddHHmmss')+'.xlsx')
officecli create $workbook
if($LASTEXITCODE -ne 0){throw '创建工作簿失败'}
officecli batch $workbook --input $inputPath --stop-on-error|Set-Content -LiteralPath (Join-Path $run '报告生成日志.txt') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '生成工作簿失败'}
officecli close $workbook
if($LASTEXITCODE -ne 0){throw '保存工作簿失败'}
$workbook|Set-Content -LiteralPath (Join-Path $run '报告路径.txt') -Encoding utf8
Write-Output $workbook
$cases|Group-Object Sheet|Select-Object Name,Count
