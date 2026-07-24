const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const list = read('src/views/system/notify/my/components/MyNotifyMessageList.vue')
const popup = read('src/layout/components/Message/src/Message.vue')
const mapper = read('../ruoyi-vue-pro/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/dal/mysql/notify/NotifyMessageMapper.java')

assert.doesNotMatch(
  list,
  /<Icon icon="ep:reading" class="mr-5px" \/>\s*批量阅读/,
  '我的站内信顶部不应再显示“批量阅读”按钮。'
)
assert.doesNotMatch(
  list,
  /const handleUpdateList = async/,
  '删除“批量阅读”入口后不应保留批量阅读处理函数。'
)
assert.doesNotMatch(
  list,
  /type="selection"/,
  '删除“批量阅读”入口后不应保留仅服务于批量阅读的表格多选列。'
)
assert.doesNotMatch(
  list,
  /selectedIds/,
  '删除“批量阅读”入口后不应保留批量阅读选中 ID 状态。'
)
assert.match(
  list,
  /<Icon icon="ep:reading" class="mr-5px" \/>\s*全部阅读/,
  '我的站内信顶部按钮必须显示“全部阅读”。'
)
assert.doesNotMatch(
  list,
  /{{\s*scope\.row\.readStatus\s*\?\s*'详情'\s*:\s*'已读'\s*}}/,
  '我的站内信操作列未读动作不能显示“已读”，已读只能作为状态展示。'
)
assert.match(
  list,
  /{{\s*scope\.row\.readStatus\s*\?\s*'详情'\s*:\s*'阅读'\s*}}/,
  '我的站内信操作列未读动作必须显示“阅读”。'
)
assert.match(
  list,
  /:type="scope\.row\.readStatus \? 'primary' : 'success'"/,
  '我的站内信操作列未读“阅读”按钮必须使用绿色 success 类型。'
)
assert.match(
  list,
  /const buildReadDetailData = \(\s*source: NotifyMessageApi\.NotifyMessageVO,\s*refreshed: NotifyMessageApi\.NotifyMessageVO \| undefined,\s*readAt: Date\s*\): NotifyMessageApi\.NotifyMessageVO => \{/,
  '未读消息阅读后必须用本次阅读时间构造可打开详情的数据，不能依赖刷新后的当前列表仍包含该行。'
)
assert.match(
  list,
  /readTime: refreshed\?\.readTime \|\| source\.readTime \|\| readAt/,
  '阅读成功后详情数据必须在刷新行仍缺阅读时间时使用本次阅读时间，不能继续显示空阅读时间。'
)
assert.match(
  list,
  /const readTimeByMessageId = ref<Record<number, Date>>\(\{\}\)/,
  '阅读接口成功后必须记录本地阅读时间覆盖，供列表刷新后合并展示。'
)
assert.match(
  list,
  /const mergeReadTimeOverrides = \(\s*items: NotifyMessageApi\.NotifyMessageVO\[\]\s*\): NotifyMessageApi\.NotifyMessageVO\[\] =>/,
  '我的站内信列表刷新必须统一合并本地阅读时间覆盖，避免阅读时间列继续为空。'
)
assert.match(
  list,
  /list\.value = mergeReadTimeOverrides\(data\.list\)/,
  '我的站内信列表不能直接使用刷新数据覆盖本地阅读时间。'
)
assert.match(
  list,
  /readTimeByMessageId\.value = \{\s*\.\.\.readTimeByMessageId\.value,\s*\[id\]: readAt\s*\}/,
  '单条阅读接口成功后必须先记录本地阅读时间，再刷新列表。'
)
assert.match(
  list,
  /detailRef\.value\.open\(buildReadDetailData\(data, updatedData, readAt\)\)/,
  '未读消息点击“阅读”成功后必须打开详情，即使当前筛选刷新后该行已从列表消失。'
)
assert.doesNotMatch(
  list,
  /if\s*\(\s*refreshed\s*\)\s*\{\s*return refreshed\s*\}/,
  '刷新后的行数据仍可能缺阅读时间，详情数据不能直接原样返回 refreshed。'
)
assert.doesNotMatch(
  list,
  /if\s*\(\s*updatedData\s*\)\s*\{\s*detailRef\.value\.open\(updatedData\)/,
  '未读消息阅读成功后不能因刷新列表未返回该行而跳过详情弹窗。'
)
assert.match(
  list,
  /message\.success\('全部阅读成功！'\)/,
  '全部阅读成功提示必须使用阅读动作语义。'
)
assert.match(
  mapper,
  /setReadStatus\(true\)\.setReadTime\(LocalDateTime\.now\(\)\)/,
  '后端阅读接口必须继续在首次阅读时写入阅读时间。'
)
assert.doesNotMatch(
  popup,
  /unreadCount\.value\s*=\s*0\s*\/\/\s*强制设置 unreadCount 为 0/,
  '顶部站内信弹层不能在仅打开未读列表时强制清零红点。'
)
assert.match(
  popup,
  /unreadCount\.value\s*=\s*list\.value\.length/,
  '顶部站内信弹层打开后红点应由真实未读列表决定，只要还有未读就保留。'
)

console.log('PASS: notify message read action static contract')
