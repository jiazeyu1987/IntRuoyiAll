$ErrorActionPreference='Stop'
$run=$PSScriptRoot
$cases=[System.Collections.Generic.List[object]]::new()
function Add-Case($sheet,$node,$target,$operation,$expected,$actual,$limit,$path,$image){$cases.Add([pscustomobject]@{Sheet=$sheet;Node=$node;Target=$target;Operation=$operation;Expected=$expected;Actual=$actual;Limit=$limit;Path=$path;Image=$image})}
$base='本地地址 http://localhost:8081；测试1租户；潘鑫账号；eDHR批记录；检验表单；'
Add-Case '已通过' '环境及基线' 'ID与IDE' '核对租户、身份、目标和现有版本。' '在指定环境操作，并留下比较依据。' 'ID基线为G/6，IDE基线为B/2；页面身份为测试1、潘鑫。' '仅记录本次实际页面，不沿用上次结果。' ($base+'检验规程；分别选择ID与IDE；记录版本及关联。') '01-ID基线G6.png'
Add-Case '已通过' '分页' 'ID正式规程' '把每页数量改为十条，进入第二页。' '共十九项，第二页九项。' '共十九项，第二页九项；上一页可用，下一页不可用。' '不是用通用规程的小表代替跨页。' ($base+'检验规程；ID；检验项目；每页十条；第二页。') '05-真实跨页.png'
Add-Case '已通过' '筛选无结果' 'ID正式规程' '输入不存在的工序名称后清空筛选。' '无匹配时空表，清空后恢复。' '显示暂无检验项目；清空筛选后恢复十九项。' '另以检测筛选可见两项。' ($base+'检验规程；ID；检验项目；工序包含输入不存在的工序20261127；清空。') '06-筛选无结果.png'
Add-Case '已通过' '错误文件拦截' '检验编制' '导入无效的文档。' '用中文说明失败，不生成可发布的空规程。' '提示文件损坏或并非有效文档，并建议另存后导入；保存、导出及发布不可用。' '本次未出现上次英文错误提示。' ($base+'检验编制；导入Word；选择错误文件.docx。') '07-错误文件提示.png'
Add-Case '已通过' '有效文件及发布拦截' 'ID产品' '错误导入后改导有效文件，打开预览。' '能恢复解析；设备缺失时不能发布。' '生成九道工序十九项；八处设备未绑定时确认发布不可用。' '依指南不以原文判断解析内容对错。' ($base+'检验编制；选择ID；导入ID.docx；预览发布。') '08-缺设备拦截.png'
Add-Case '已通过' '设备配置与失败恢复' 'ID产品' '逐项绑定真实候选设备后重新预览。' '缺项清零后允许按正常流程发布。' '八处绑定完成，待处理问题清零，最终发布G/7。' '最后一处误选已在发布前改为十五牛砝码。' ($base+'检验编制；ID；依次补设备；预览；确认末检不适用；发布。') '11-编制发布G7.png'
Add-Case '已通过' '新增修改及排序' 'ID光固二工序' '增加临时项目，改名并上移。' '项目数和位置随操作变化。' '该工序由两项变三项，总数由十九变二十；临时项目改名并上移。' '临时项目未进入正式发布版本。' ($base+'检验编制；ID；光固二；添加项目；改名主链临时项目；项目上移。') '09-增改排序取消删除.png'
Add-Case '已通过' '删除取消与确认' '临时项目' '先取消删除，再确认删除。' '取消时保留，确认后移除且计数还原。' '取消后临时项仍在；确认后恢复两项和总数十九项。' '本项仅证明删除取消，不代替所有编辑撤销场景。' ($base+'检验编制；光固二；临时项目；删除后取消；再删除并确认。') '09-增改排序取消删除.png'
Add-Case '已通过' '重复保存及刷新恢复' 'ID编制草稿' '连续保存两次，刷新并恢复草稿。' '恢复相同内容，无重复项目。' '刷新后恢复为九道工序十九项，设备待处理问题仍为零。' '此处是浏览器内编制草稿，不是正式发布。' ($base+'检验编制；ID；保存草稿两次；刷新；恢复草稿。') '10-重复保存刷新恢复.png'
Add-Case '已通过' '编制发布并打开结果' 'ID产品G/7' '正常预览并发布，查看正式结果。' '正式版本真实生成。' '页面发布成功并生成G/7，查看结果能进入正式规程。' '末检不适用经页面确认，不绕过检查。' ($base+'检验编制；ID；预览发布；确认末检不适用；确认发布；查看结果。') '11-编制发布G7.png'
Add-Case '已通过' '正式草稿升版' 'ID产品G/8' '修改清洗外观名称及抽样接收限，保存草稿再发布。' '生成新版本，修改在刷新后保留。' '保存生成G/8草稿，发布后刷新显示G/8；后续G/10仍保留改名与接收限0.650。' '原接收限为0.400；设备历史问题另列。' ($base+'检验规程；ID的G/7；检验项目；清洗外观改名外观（主链复测）；接收限改0.650；保存；发布；刷新。') '30-G10修改保留.png'
Add-Case '已通过' '旧正式内容保留' 'ID产品G/7' '保存G/8草稿后回选G/7查看名称与抽样。' '旧版本这两项不被草稿覆盖。' '旧版仍为外观及接收限0.400；新草稿为改名及0.650。' '设备读取异常不在此项判为通过，另列存疑。' ($base+'检验规程；ID；保存G/8草稿后；回选G/7；核对清洗第一项。') '12-新草稿G8.png'
Add-Case '已通过' '通用导入及发布' '大中包装' '导入、保存、预览并发布。' '生成可查看的正式通用版本。' '一道工序三项，发布A/6成功。' '只判断真实页面与发布状态，不核对原文。' ($base+'通用编制；导入大中包装.docx；保存；预览；确认末检不适用；发布。') '16-大中包装A6.png'
Add-Case '已通过' '通用导入及发布' '初包装' '导入、保存、预览并发布。' '生成可查看的正式通用版本。' '一道工序四项，发布A/10成功。' '不使用美联专用规程替代普通初包装。' ($base+'通用编制；导入初包装.docx；保存；预览；确认末检不适用；发布。') '17-初包装A10.png'
Add-Case '已通过' '通用导入及结果查看' '美联初包装' '导入、预览发布并查看结果。' '生成专用通用版本且可打开。' '一道工序四项，B/4发布成功，结果窗口可查看。' '没有给ID改绑美联规程。' ($base+'通用编制；导入美联初包装.docx；预览；确认末检不适用；发布；查看结果。') '18-美联B4.png'
Add-Case '已通过' '关联指定版本' 'ID产品G/9' '选择普通初包装A/10和大中包装A/6，保存并发布。' '正式关联对应指定版本。' '刷新后G/9关联初包装A/10、大中包装A/6。' '刷新后才读取到新发布候选项。' ($base+'检验规程；ID；关联；选初包装A/10和大中包装A/6；保存；发布；刷新。') '19-G9绑定指定通用版.png'
Add-Case '已通过' '通用规程升级' '大中包装A/7' '在正式通用页改规程名称，保存并发布。' '产生新的正式版本。' '名称追加主链升版复测，发布A/7；刷新后仍为一道工序三项。' '本次成功升级改的是名称，没有把未提交的项目改名算作已发布。' ($base+'通用规程；PQC-CR-004；名称追加（主链升版复测）；保存；发布；刷新。') '21-通用A7刷新验证.png'
Add-Case '已通过' '通用升级不自动改绑' 'ID产品G/9' '通用A/7发布后刷新产品关联。' '原绑定版本仍为A/6。' '大中包装仍为A/6，初包装仍为A/10。' '版本号未变；名称标签变化单独列为存疑。' ($base+'通用A/7发布后；检验规程；ID；G/9；关联。') '22-通用升级仍绑定A6.png'
Add-Case '已通过' '主动改绑新版并发布' 'ID产品G/10' '主动选择大中包装A/7，保存并发布。' '新产品正式版关联新版。' '刷新后G/10关联初包装A/10和大中包装A/7。' '不是通用升级自动替换产品版本。' ($base+'检验规程；ID；关联；大中包装改A/7；保存；发布G/10；刷新。') '23-G10主动绑定A7.png'
Add-Case '已通过' '历史关联及抽样' 'ID产品G/9' '在G/10发布后查看历史G/9。' '历史保留其原关联和修改后抽样。' '历史G/9能打开；大中包装仍为A/6；第一项仍保留改名和接收限0.650。' '此项不证明所有历史设备均正常。' ($base+'检验规程；ID；版本选择G/9；检验项目及关联。') '31-G9历史关联.png'
foreach($product in @('IDE','IDI','IDML','IDPR')){$image=@{IDE='25-IDE解析.png';IDI='26-IDI解析.png';IDML='27-IDML解析.png';IDPR='28-IDPR解析.png'}[$product]; Add-Case '已通过' '其他产品解析' $product '在检验编制选择产品并导入对应文档。' '能显示可编辑的工序和检验项目。' '本次页面成功显示解析内容。' '仅测试导入，没有发布或补齐该产品设备。' ($base+"检验编制；选择$product；导入$product.docx；查看解析内容。") $image}
Add-Case '已通过' '导出并重新导入' 'IDPR编制内容' '实际导出Word后，将下载文件重新导入。' '下载文件可再次解析。' '下载文件已保留，重新导入后可编辑，规程名称、编号和日期显示正常。' '本项只证明回导可用；逐字段完全一致另列存疑。' ($base+'检验编制；IDPR；导出Word；导入下载文件IDPR导出回导.docx。') '29-IDPR导出回导.png'
Add-Case '已通过' '非目标版本及关联保护' 'IDE产品' 'ID重置尝试后回查IDE基线。' '非目标正式版本及绑定版本不变。' 'IDE仍为B/2，初包装A/7及大中包装A/1均保留。' '通用名称标签变为最新名称的问题另列存疑；目标未清空，不能据此宣称重置范围全部正确。' ($base+'ID重置确认并刷新后；检验规程；选择IDE；关联；与基线比较。') '34-非目标IDE保留.png'
Add-Case '已通过' '通用版本未被重置删除' '大中包装A/7' 'ID重置尝试后打开通用规程。' '非目标通用规程仍可查看。' '大中包装A/7仍已发布，一道工序三项。' '只对实际回查的大中包装作此结论。' ($base+'ID重置尝试后；通用规程；选择PQC-CR-004；查看A/7。') '35-重置后通用A7保留.png'
Add-Case '未通过' '历史正式版读取' 'ID产品G/7' 'G/10发布后从版本下拉选择G/7。' '完整加载对应历史内容和设备。' '页面显示规程版本加载失败，提示服务器错误；不能完整查看该历史版本。' '未重复保存失败后留在页面的旧内容；需要开发修复历史版本读取。' ($base+'检验规程；DCC项目ID；当前G/10；展开版本；选择G/7已作废；观察加载失败提示。') '24-历史G7读取失败.png'
Add-Case '未通过' '目标项目重置' 'ID产品' '点击测试重置，确认清理目标，再刷新并展开版本。' '清空提示范围内的目标草稿及正式版本；被引用时应明确拒绝。' '确认后刷新，G/0至G/10仍在，G/10仍为当前已发布；本次没有观察到明确拒绝原因。' '只确认提交一次，不反复重置。需要开发核对按钮提交及后端清理。' ($base+'检验规程；ID；测试重置；确认提示将清空草稿及已发布版本；确认重置；刷新；展开规程版本。') '33-重置后版本仍保留.png'
Add-Case '未通过' '页面编译错误遮挡' '本地检验页面' '执行通用升版过程中继续页面操作。' '页面可持续操作，不出现编译错误遮罩。' '出现前端编译错误遮罩，打断操作；随后刷新恢复并完成独立节点。' '不是永久不可访问，具体触发条件仍需开发结合本地构建记录定位。' ($base+'通用编制；导入大中包装并编辑；预览期间出现编译错误遮罩；截图见最后一列；刷新后恢复。') '20-本地编译遮挡.png'
Add-Case '存疑' '历史设备和抽样整体一致性' 'ID新旧版本' '发布前后查看旧版及新版内容。' '全部版本的设备、项目及抽样能逐项查看且一致保留。' '名称及接收限和G/9关联有回查；G/7历史读取报错，早期保存后还出现设备名称不匹配提示，未完成各历史版设备逐项比对。' '不能认定历史设备丢失，也不能判全部保留正常。' ($base+'检验规程；ID；保存G/8后回选G/7查看设备；G/10发布后再选G/7及G/9；分别核对设备和抽样。') '24-历史G7读取失败.png'
Add-Case '存疑' '旧版本名称显示' '历史大中包装关联' '升级通用名称后查看旧产品及历史关联。' '旧绑定版本信息表达清楚，不误导为新版内容。' 'IDE仍绑A/1，ID历史仍绑A/6，但显示名称都带本次A/7新增的主链升版复测。' '可确认标签变了；尚不能证明旧版本检验内容被覆盖，需确认名称是否按设计始终显示最新名称。' ($base+'通用规程；PQC-CR-004由A/6改名发布A/7；检验规程；ID选G/9或IDE的B/2；关联；查看旧版本名称标签。') '34-非目标IDE保留.png'
Add-Case '存疑' '导出回导逐项对照' 'IDPR全部字段' '回导下载文档后检查内容。' '所有工序、项目、设备和抽样字段逐一一致。' '已确认能够回导及基本抬头显示，但没有形成全部项目和隐藏结构字段逐项一致的充分证据。' '不能用回导成功代替完整数据一致性通过；需要补齐全字段对照。' ($base+'检验编制；IDPR；导入IDPR.docx；逐项记录设备和抽样；导出；回导IDPR导出回导.docx；逐项比较。') '29-IDPR导出回导.png'
Add-Case '存疑' '完整清理范围' '目标与全部非目标数据' '执行重置后对照全部基线。' '目标清空且全部非目标产品和通用数据不受影响。' '目标未清空；IDE版本和绑定及大中包装A/7仍保留，但不足以证明清理范围正确。' '目标重置修复后，应重测清理及所有相关非目标对象。' ($base+'检验规程；ID；测试重置并确认；刷新；对照ID、IDE和通用基线；目标仍在使完整范围验证无法通过。') '33-重置后版本仍保留.png'
Add-Case '存疑' '取消普通编辑' '非删除类修改' '区分删除取消与其他字段编辑撤销。' '取消普通编辑后恢复原值且不保存。' '本次有删除取消的独立证据，未对所有普通字段的取消或撤销操作单独形成证据。' '不把删除取消算作全部编辑取消均通过。' ($base+'检验编制；ID；选择一个普通字段；改值；撤销或取消；回看原值并刷新核对。') '09-增改排序取消删除.png'
$stamp=Get-Date -Format 'yyyyMMddHHmmss'
$workbook=Join-Path $run "report\$stamp.xlsx"
$commands=[System.Collections.Generic.List[object]]::new()
foreach($sheet in @('已通过','未通过','存疑')){$commands.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
$commands.Add(@{command='remove';path='/Sheet1'})
$headers=@('编号','测试环节','测试对象','操作方法','应有结果','实际结果','限制及后续处理','完整测试路径','证据截图')
$columns=@('A','B','C','D','E','F','G','H','I')
$widths=@(9,24,24,36,36,52,48,65,80)
foreach($sheet in @('已通过','未通过','存疑')){
 $rows=@($cases|Where-Object Sheet -eq $sheet)
 $commands.Add(@{command='set';path="/$sheet";props=@{freeze='D3';autoFilter="A2:I$($rows.Count+2)"}})
 $commands.Add(@{command='set';path="/$sheet/A1:H1";props=@{merge='true';value="2026年11月27日本地主链复测：$sheet $($rows.Count)项。测试1，潘鑫。全链未通过；历史读取和重置失败，证据不足项单列存疑。";fill='EAF0F6';bold='true';wrapText='true'}})
 $commands.Add(@{command='set';path="/$sheet/row[1]";props=@{height='42'}})
 for($columnIndex=0;$columnIndex -lt 9;$columnIndex++){
  $commands.Add(@{command='set';path="/$sheet/col[$($columns[$columnIndex])]";props=@{width=[string]$widths[$columnIndex]}})
  $commands.Add(@{command='set';path="/$sheet/$($columns[$columnIndex])2";props=@{value=$headers[$columnIndex];fill='264663';fontColor='FFFFFF';bold='true';wrapText='true'}})
 }
 $rowNumber=3
 foreach($case in $rows){
  $values=@([string]($rowNumber-2),$case.Node,$case.Target,$case.Operation,$case.Expected,$case.Actual,$case.Limit,$case.Path)
  for($columnIndex=0;$columnIndex -lt 8;$columnIndex++){$commands.Add(@{command='set';path="/$sheet/$($columns[$columnIndex])$rowNumber";props=@{value=$values[$columnIndex];font='微软雅黑';fontSize='11';wrapText='true';valign='top';'font.color'='202B38'}})}
  $commands.Add(@{command='set';path="/$sheet/row[$rowNumber]";props=@{height='242'}})
  $commands.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=(Join-Path $run "截图\$($case.Image)");anchor="I${rowNumber}:J$($rowNumber+1)";alt="本次实际页面证据：$($case.Node)"}})
  $rowNumber++
 }
}
$inputPath=Join-Path $run '报告生成指令.json'
$commands|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $inputPath -Encoding utf8
$cases|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $run '本次测试记录.json') -Encoding utf8
officecli create $workbook
if($LASTEXITCODE -ne 0){throw '创建工作簿失败'}
officecli batch $workbook --input $inputPath --stop-on-error|Set-Content -LiteralPath (Join-Path $run '报告生成日志.txt') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '生成工作簿失败'}
officecli close $workbook
if($LASTEXITCODE -ne 0){throw '保存工作簿失败'}
Write-Output $workbook
$cases|Group-Object Sheet|Select-Object Name,Count
