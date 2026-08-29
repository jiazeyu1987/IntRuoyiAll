import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const exists = (relativePath) => existsSync(join(root, relativePath))
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const appRoot = exists('src') ? '' : 'IntRuoyiFronted'
const uploadDialogPath = `${appRoot ? `${appRoot}/` : ''}src/views/dcc/registration-certificate/upload/UploadDialog.vue`

assert.equal(exists(uploadDialogPath), true, `${uploadDialogPath} must exist`)

const source = read(uploadDialogPath)
const styleStart = source.indexOf('<style')
const styleEnd = source.indexOf('</style>')

assert.notEqual(styleStart, -1, 'upload dialog must keep scoped layout styles')
assert.notEqual(styleEnd, -1, 'upload dialog must close the style block')

const style = source.slice(styleStart, styleEnd)
const labelRule = style.match(/\.el-form-item__label\s*\{[\s\S]*?\}/)?.[0] || ''
const mobileRule = style.match(/@media\s*\(max-width:\s*720px\)\s*\{[\s\S]*$/)?.[0] || ''

assert.match(source, /label-width="12[0-9]px"/, 'form label width must leave room for long labels')
assert.match(source, /<el-row\s+:gutter="2[4-9]"/, 'form columns must have a wider horizontal gutter')
assert.match(labelRule, /padding-right:\s*12px\s*;/, 'labels must keep a fixed gap before controls')
assert.doesNotMatch(labelRule, /padding:\s*0\s*;/, 'labels must not reset all padding to zero')
assert.match(style, /\.el-row\s*\{[\s\S]*?row-gap:\s*4px\s*;/, 'form rows must have vertical breathing room')
assert.match(style, /\.el-form-item\s*\{[\s\S]*?margin-bottom:\s*20px\s*;/, 'form items must keep a comfortable bottom gap')
assert.match(mobileRule, /\.el-form-item\s*\{[\s\S]*?display:\s*block\s*;/, 'narrow screens must switch form items to stacked layout')
assert.match(mobileRule, /\.el-form-item__label\s*\{[\s\S]*?width:\s*100%\s*!important\s*;/, 'narrow screens must place labels above inputs')
