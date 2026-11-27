import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
const root=process.cwd()
const appRoot=existsSync(join(root,'src'))?'':'IntRuoyiFronted'
const read=path=>readFileSync(join(root,appRoot,'src/views/dcc/registration-certificate',path),'utf8')
const upload=read('upload/UploadDialog.vue'),change=read('change/ChangeDialog.vue'),style=read('shared/certificateForm.css')
for(const source of [upload,change]){
 assert.match(source,/label-position="top"/,'new and change forms keep labels above inputs')
 assert.match(source,/class="upload-material-grid"/,'upload material uses its dedicated grid')
 assert.match(source,/<UploadMaterialSlot[^>]+v-model="files"/,'primary material uses the same stateful card')
 assert.match(source,/<UploadMaterialSlot[^>]+v-model="attachments"/,'attachments use the same stateful card')
}
assert.match(style,/\.upload-material-grid\s*\{[^}]*repeat\(2, minmax\(0, 1fr\)\)/,'domestic and CE material columns share available width without filename overflow')
assert.match(style,/\.upload-material-grid\.is-fda\s*\{[^}]*repeat\(3, minmax\(0, 1fr\)\)/,'FDA has three columns')
assert.match(style,/@media\s*\(max-width:\s*600px\)[\s\S]*\.upload-material-grid[^}]+grid-template-columns:\s*1fr/,'narrow screens stack material cards')
assert.match(style,/\.certificate-section-heading\s*\{[^}]*margin-bottom:\s*16px/,'upload heading keeps breathing room above the cards')
