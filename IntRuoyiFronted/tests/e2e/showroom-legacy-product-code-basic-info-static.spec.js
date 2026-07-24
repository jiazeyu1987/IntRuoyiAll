const fs = require('fs')
const path = require('path')

const file = path.join(process.cwd(), 'src/views/showroom-admin/index.vue')
const source = fs.readFileSync(file, 'utf8')

const required = [
  {
    name: 'ProductForm declares legacyProductCode',
    pattern: /interface ProductForm[\s\S]*legacyProductCode:\s*string/
  },
  {
    name: 'empty product form initializes legacyProductCode',
    pattern: /const createEmptyProductForm = \(\): ProductForm => \(\{[\s\S]*legacyProductCode:\s*''/
  },
  {
    name: 'basic info dialog renders old product code input',
    pattern:
      /<el-form-item label="旧产品编号">[\s\S]*v-model="productForm\.legacyProductCode"[\s\S]*placeholder="例如 product_012"/
  },
  {
    name: 'edit dialog hydrates legacyProductCode from detail',
    pattern:
      /assignProductForm\(\{[\s\S]*legacyProductCode:\s*resolveStringValue\(revision\.legacyProductCode\)/
  },
  {
    name: 'save payload includes trimmed legacyProductCode',
    pattern: /const payload = \{[\s\S]*legacyProductCode:\s*productForm\.legacyProductCode\.trim\(\)/
  }
]

const failures = required.filter((item) => !item.pattern.test(source)).map((item) => item.name)

if (failures.length > 0) {
  console.error('旧产品编号基础信息静态检查失败:')
  for (const failure of failures) {
    console.error(`- ${failure}`)
  }
  process.exit(1)
}

console.log('旧产品编号基础信息静态检查通过')
