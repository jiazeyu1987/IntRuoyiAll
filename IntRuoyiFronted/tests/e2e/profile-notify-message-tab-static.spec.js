const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const messagePopover = read('src/layout/components/Message/src/Message.vue')
const notifyPage = read('src/views/system/notify/my/index.vue')

const notifyListPath = 'src/views/system/notify/my/components/MyNotifyMessageList.vue'
assert.ok(
  fs.existsSync(path.join(repoRoot, notifyListPath)),
  '我的站内信列表必须抽成可复用组件，供个人中心 Tab 和原路由页面共用。'
)

const notifyList = fs.existsSync(path.join(repoRoot, notifyListPath)) ? read(notifyListPath) : ''

assert.match(
  profileIndex,
  /<el-tab-pane\s+name="workbench">[\s\S]*个人工作台[\s\S]*?<\/el-tab-pane>\s*<el-tab-pane\s+name="notifyMessage">/,
  '个人中心必须在“个人工作台”后紧跟“我的站内信”Tab。'
)
assert.match(
  profileIndex,
  /<template\s+#label>[\s\S]*<el-badge[\s\S]*class="profile-notify-message-tab__badge"[\s\S]*:is-dot="hasUnreadNotifyMessage"[\s\S]*:hidden="!hasUnreadNotifyMessage"[\s\S]*我的站内信[\s\S]*<\/el-badge>[\s\S]*<\/template>/,
  '个人中心“我的站内信”Tab 必须用红点展示未读站内信状态。'
)
assert.match(
  profileIndex,
  /import \* as NotifyMessageApi from '@\/api\/system\/notify\/message'/,
  '个人中心必须复用真实站内信未读数量接口。'
)
assert.match(
  profileIndex,
  /const unreadNotifyMessageCount = ref\(0\)[\s\S]*const hasUnreadNotifyMessage = computed\(\(\) => unreadNotifyMessageCount\.value > 0\)/,
  '个人中心必须用未读数量派生红点状态。'
)
assert.match(
  profileIndex,
  /const refreshUnreadNotifyMessageCount = async \(\) => \{[\s\S]*NotifyMessageApi\.getUnreadNotifyMessageCount\(\)[\s\S]*\}/,
  '个人中心必须通过真实接口刷新未读数量。'
)
assert.match(
  profileIndex,
  /onMounted\(\(\) => \{[\s\S]*refreshUnreadNotifyMessageCount\(\)[\s\S]*\}\)/,
  '个人中心加载时必须刷新未读数量。'
)
assert.match(
  profileIndex,
  /<MyNotifyMessageList\s+class="profile-notify-message-tab"\s+embedded\s+@read-status-change="refreshUnreadNotifyMessageCount"\s*\/>/,
  '个人中心“我的站内信”Tab 必须以嵌入模式渲染列表，并在已读状态变化后刷新红点。'
)
assert.match(
  profileIndex,
  /import MyNotifyMessageList from '@\/views\/system\/notify\/my\/components\/MyNotifyMessageList\.vue'/,
  '个人中心必须复用我的站内信列表组件，不能复制列表逻辑。'
)
assert.match(
  profileIndex,
  /route\.query\.tab\s*===\s*'notifyMessage'/,
  '个人中心必须支持通过 ?tab=notifyMessage 打开“我的站内信”Tab。'
)
assert.match(
  profileIndex,
  /watch\(\s*\[\s*\(\)\s*=>\s*route\.fullPath,\s*isAdminUser\s*\][\s\S]*activeName\.value\s*=\s*resolveProfileActiveTab\(\)/,
  '个人中心必须监听路由变化，确保从站内信弹层跳转后能切换到“我的站内信”Tab。'
)

assert.match(
  messagePopover,
  /name:\s*'Profile'[\s\S]*query:\s*\{[\s\S]*tab:\s*'notifyMessage'[\s\S]*\}/,
  '站内信弹层“查看全部”必须跳转到个人中心并打开“我的站内信”Tab。'
)
assert.doesNotMatch(
  messagePopover,
  /name:\s*'MyNotifyMessage'/,
  '站内信弹层“查看全部”不能再跳到独立站内信路由。'
)

assert.match(
  notifyPage,
  /<MyNotifyMessageList\s*\/>/,
  '原“我的站内信”路由页面必须继续渲染同一个列表组件。'
)
assert.match(
  notifyList,
  /embedded\?:\s*boolean/,
  '我的站内信列表必须提供嵌入模式。'
)
assert.match(
  notifyList,
  /contentWrapper[\s\S]*props\.embedded\s*\?\s*'div'\s*:\s*'ContentWrap'/,
  '我的站内信列表嵌入个人中心时必须使用无卡片容器。'
)
assert.match(
  notifyList,
  /getMyNotifyMessagePage/,
  '我的站内信列表必须继续调用真实我的站内信分页接口。'
)
assert.match(
  notifyList,
  /defineEmits<\{[\s\S]*read-status-change[\s\S]*\}>/,
  '我的站内信列表必须向父组件暴露已读状态变化事件。'
)
for (const handlerName of ['handleReadOne', 'handleUpdateAll']) {
  const handlerIndex = notifyList.indexOf(`const ${handlerName}`)
  assert.notEqual(handlerIndex, -1, `我的站内信列表必须保留 ${handlerName}。`)
  const nextHandlerIndex = notifyList.indexOf('\n/**', handlerIndex + 1)
  const handlerSource =
    nextHandlerIndex === -1 ? notifyList.slice(handlerIndex) : notifyList.slice(handlerIndex, nextHandlerIndex)
  assert.match(
    handlerSource,
    /emit\('read-status-change'\)/,
    `${handlerName} 完成已读操作后必须通知父组件刷新未读红点。`
  )
}
assert.equal(
  notifyList.indexOf('const handleUpdateList'),
  -1,
  '我的站内信列表不应保留已移除的批量阅读处理函数。'
)
assert.equal(
  notifyList.indexOf('type="selection"'),
  -1,
  '我的站内信列表不应保留仅服务于批量阅读的多选列。'
)
assert.match(
  notifyList,
  /MyNotifyMessageDetail/,
  '我的站内信列表必须保留详情弹窗和已读联动。'
)

console.log('PASS: profile notify message tab static contract')
