const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const routePath = 'src/router/modules/remaining.ts'
const logoComponentPath = 'src/layout/components/Logo/src/Logo.vue'
const pngPath = 'src/assets/imgs/sidebar-brand-logo.png'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

const route = readUtf8(routePath)
const logoComponent = readUtf8(logoComponentPath)
const logoPng = fs.readFileSync(path.join(repoRoot, pngPath))

assertContains(route, "icon: 'ep:home-filled'", 'home menu icon contract')
assertContains(logoComponent, 'src="@/assets/imgs/sidebar-brand-logo.png"', 'Logo uses sidebar brand asset')
assertContains(logoComponent, 'h-full w-full object-contain object-center', 'Logo keeps contained centered scaling')
assertContains(logoComponent, 'ml-18px text-16px font-700', 'Logo title keeps two-space visual gap')

if (logoPng.readUInt32BE(0) !== 0x89504e47) {
  throw new Error('sidebar brand asset must be PNG')
}
if (logoPng.readUInt32BE(16) !== 80 || logoPng.readUInt32BE(20) !== 80) {
  throw new Error('sidebar brand PNG must be 80x80')
}
if (logoPng.readUInt8(25) !== 6) {
  throw new Error('sidebar brand PNG must be RGBA to preserve transparent background')
}

console.log('PASS: sidebar brand logo uses INT MEDICAL PNG and keeps menu home contract')
