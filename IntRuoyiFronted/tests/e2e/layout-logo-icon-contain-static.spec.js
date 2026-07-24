const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const logoPath = 'src/layout/components/Logo/src/Logo.vue'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const logo = readUtf8(logoPath)

assertContains(logo, 'src="@/assets/imgs/sidebar-brand-logo.png"', 'sidebar brand logo asset')
assertContains(logo, 'alt="瑛泰管理系统品牌图标"', 'brand icon alt text')
assertContains(logo, 'logo-icon-frame', 'fixed icon frame class')
assertContains(logo, 'h-40px w-40px', '40px square icon frame')
assertContains(logo, 'items-center justify-center', 'centered icon frame')
assertContains(logo, 'h-full w-full object-contain object-center', 'full contained centered image')
assertContains(logo, 'ml-18px text-16px font-700', 'two-space visual title spacing')
assertContains(logo, '{{ title }}', 'system title remains')

assertNotContains(logo, 'object-left', 'left-aligned image rendering')
assertNotContains(logo, 'w-[var(--logo-height)]', 'wide image box that leaves the icon visually offset')

console.log('PASS: sidebar logo icon uses a fixed square frame, contained image, and title spacing')
