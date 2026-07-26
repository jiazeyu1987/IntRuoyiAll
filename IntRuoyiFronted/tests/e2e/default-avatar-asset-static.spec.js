const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const defaultAvatarPath = path.join(frontendRoot, 'src/assets/imgs/default-avatar.png')
assert.ok(fs.existsSync(defaultAvatarPath), '默认头像必须使用新的 PNG 资源 default-avatar.png')

const defaultAvatar = fs.readFileSync(defaultAvatarPath)
assert.equal(defaultAvatar.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', '默认头像必须是 PNG 文件')
assert.equal(defaultAvatar.readUInt32BE(16), 472, '默认头像宽度必须匹配用户提供图片')
assert.equal(defaultAvatar.readUInt32BE(20), 472, '默认头像高度必须匹配用户提供图片')
assert.equal(
  crypto.createHash('sha256').update(defaultAvatar).digest('hex').toUpperCase(),
  'F7012CEEFC62703EE685C8D3AB419D2AB966063E9FBCFCB4E958C13D4A3A1102',
  '默认头像必须匹配用户提供的图片内容'
)

const importers = [
  'src/components/Cropper/src/CropperAvatar.vue',
  'src/layout/components/UserInfo/src/UserInfo.vue',
  'src/layout/components/UserInfo/src/components/LockDialog.vue',
  'src/layout/components/UserInfo/src/components/LockPage.vue',
  'src/views/ai/chat/index/components/message/MessageList.vue'
]

for (const importer of importers) {
  const source = read(importer)
  assert.ok(
    source.includes("@/assets/imgs/default-avatar.png"),
    `${importer} 必须引用新的默认头像 PNG`
  )
  assert.doesNotMatch(source, /@\/assets\/imgs\/avatar\.gif/, `${importer} 不得继续引用旧默认头像 GIF`)
}

console.log('PASS: default avatar asset static contract')
