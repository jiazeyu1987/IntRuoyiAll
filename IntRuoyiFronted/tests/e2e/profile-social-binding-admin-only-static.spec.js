const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const profileIndexPath = path.join(repoRoot, 'src/views/Profile/Index.vue')
const source = fs.readFileSync(profileIndexPath, 'utf8')

assert.match(
  source,
  /import\s+\{\s*useUserStore\s*\}\s+from\s+['"]@\/store\/modules\/user['"]/,
  'Profile page must read the current login roles from the user store'
)

assert.match(
  source,
  /const\s+isAdminUser\s*=\s*computed\(\s*\(\)\s*=>\s*userStore\.getRoles\.includes\(['"]super_admin['"]\)\s*\)/,
  'Profile page must derive admin visibility from the super_admin role'
)

assert.match(
  source,
  /<el-tab-pane[\s\S]*:label="t\('profile\.info\.userSocial'\)"[\s\S]*name="userSocial"[\s\S]*v-if="isAdminUser"/,
  'Social binding tab must be rendered only for admin users'
)

assert.match(
  source,
  /const\s+isSocialBindingCallback\s*=\s*\(\)\s*=>[\s\S]*route\.query\.code[\s\S]*route\.query\.type[\s\S]*route\.fullPath\.includes\(['"]type%3D['"]\)/,
  'Social OAuth callback detection must stay explicit'
)

assert.match(
  source,
  /if\s*\(\s*isAdminUser\.value\s*&&\s*isSocialBindingCallback\(\)\s*\)\s*\{[\s\S]*return\s+['"]userSocial['"]/,
  'Social OAuth callback must only open the social tab for admin users'
)

assert.match(
  source,
  /if\s*\(\s*!isAdminUser\.value\s*&&\s*activeName\.value\s*===\s*['"]userSocial['"]\s*\)[\s\S]*activeName\.value\s*=\s*['"]workbench['"]/,
  'Non-admin users must be pushed away if the active tab is userSocial'
)

console.log('PASS: profile social binding admin-only static contract')
