const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertMatch(source, pattern, label) {
  if (!pattern.test(source)) {
    throw new Error(`missing ${label}: ${pattern}`)
  }
}

function assertNotMatch(source, pattern, label) {
  if (pattern.test(source)) {
    throw new Error(`forbidden ${label}: ${pattern}`)
  }
}

function extractHomeRoute(source) {
  const start = source.indexOf("name: 'Home'")
  const end = source.indexOf("name: 'UserInfo'", start)
  if (start === -1 || end === -1) {
    throw new Error('Home route block not found')
  }
  return source.slice(start, end)
}

const routeSource = readUtf8('src/router/modules/remaining.ts')
const homeRoute = extractHomeRoute(routeSource)
const homePageSource = readUtf8('src/views/Home/Index.vue')

assertMatch(routeSource, /path:\s*'\/'[\s\S]*redirect:\s*'\/index'/, 'root route still redirects to /index')
assertMatch(homeRoute, /meta:\s*\{\s*hidden:\s*true\s*\}/, 'Home parent route is hidden from default menu')
assertMatch(homeRoute, /path:\s*'index'/, 'Index child route remains available')
assertMatch(homeRoute, /component:\s*\(\)\s*=>\s*import\('@\/views\/Home\/Index\.vue'\)/, 'Index child keeps Home page component')
assertMatch(homeRoute, /hidden:\s*true/, 'Index child is hidden from default menu')
assertMatch(homeRoute, /noTagsView:\s*true/, 'Index child is hidden from default tags')
assertNotMatch(homeRoute, /affix:\s*true/, 'Index child must not be an affix tag')

assertMatch(homePageSource, /欢迎使用 IntRuoyi 管理后台/, 'Home page content remains accessible')
assertMatch(homePageSource, /进入展厅后台/, 'Home page showroom shortcut remains accessible')

console.log('PASS: homepage route is available but hidden from default menu and tags')
