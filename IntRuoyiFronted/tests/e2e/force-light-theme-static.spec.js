const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const appSource = readSource('src/App.vue')
const appStoreSource = readSource('src/store/modules/app.ts')
const settingSource = readSource('src/layout/components/Setting/src/Setting.vue')
const socialLoginSource = readSource('src/views/Login/SocialLogin.vue')
const styleEntrySource = readSource('src/styles/index.scss')

assert.match(
  appSource,
  /appStore\.setLightTheme\(\)/,
  'App startup must explicitly apply the light theme'
)
assert.doesNotMatch(
  appSource,
  /CACHE_KEY\.IS_DARK|prefers-color-scheme|isDark\(\)/,
  'App startup must not read a cached or operating-system dark preference'
)

assert.match(
  appStoreSource,
  /isDark:\s*false/,
  'App store dark state must always initialize as false'
)
assert.match(
  appStoreSource,
  /setLightTheme\(\)\s*\{[\s\S]*classList\.add\('light'\)[\s\S]*classList\.remove\('dark'\)/,
  'App store must expose a light-only theme initializer'
)
assert.doesNotMatch(
  appStoreSource,
  /setIsDark\(/,
  'App store must not expose an action that enables dark mode'
)

for (const [sourcePath, source] of [
  ['src/layout/components/Setting/src/Setting.vue', settingSource],
  ['src/views/Login/SocialLogin.vue', socialLoginSource]
]) {
  assert.doesNotMatch(
    source,
    /ThemeSwitch/,
    `${sourcePath} must not provide a dark theme switch`
  )
}

assert.doesNotMatch(
  styleEntrySource,
  /element-plus\/theme-chalk\/dark\/css-vars\.css/,
  'Global styles must not load the unused dark theme variables'
)

for (const removedThemeSwitchFile of [
  'src/layout/components/ThemeSwitch/index.ts',
  'src/layout/components/ThemeSwitch/src/ThemeSwitch.vue'
]) {
  assert.equal(
    fs.existsSync(path.join(repoRoot, removedThemeSwitchFile)),
    false,
    `${removedThemeSwitchFile} must not remain in the source tree`
  )
}

console.log('PASS: force light theme static contract')
