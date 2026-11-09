/**
 * 单文件 3D 预览页的入口（由 esbuild 打包成 IIFE 后内联进 HTML）。
 * 与项目里的 Pet3D.vue 共用同一套 src/pet3d/scene.js 真 3D 渲染器。
 */
import { createPetScene } from '../src/pet3d/scene.js'
import { PET3D_KINDS, PET3D_CONFIG, PET3D_STATES } from '../src/pet3d/config.js'

const $ = id => document.getElementById(id)
const cv = $('cv')
let inst = null
let current = 'dog'
let state = 'idle'
let autoRotate = true

function log(msg) { $('status').textContent = msg }

try {
  inst = createPetScene(cv, current, {
    state, interactive: true, width: 440, height: 440
  })
} catch (e) {
  $('errmsg').style.display = 'block'
  $('errmsg').textContent = 'WebGL 初始化失败：' + (e && e.message ? e.message : e)
}

// 品种按钮
const kindBox = $('kinds')
PET3D_KINDS.forEach(k => {
  const c = PET3D_CONFIG[k]
  const b = document.createElement('button')
  b.className = 'chip' + (k === current ? ' on' : '')
  b.dataset.kind = k
  b.innerHTML = '<b>' + c.name + '</b><small>' + c.group + '</small>'
  b.onclick = () => {
    current = k
    document.querySelectorAll('.chip').forEach(x => x.classList.toggle('on', x.dataset.kind === k))
    $('pname').textContent = c.name
    $('pdesc').textContent = c.desc
    if (inst) inst.setKind(k)
    log('已切换：' + c.name + ' · 拖动模型可以看到侧面和背面')
  }
  kindBox.appendChild(b)
})

// 状态按钮
const stateBox = $('states')
PET3D_STATES.forEach(s => {
  const b = document.createElement('button')
  b.className = 'stab' + (s.key === state ? ' on' : '')
  b.dataset.state = s.key
  b.textContent = s.icon + ' ' + s.label
  b.onclick = () => {
    state = s.key
    document.querySelectorAll('.stab').forEach(x => x.classList.toggle('on', x.dataset.state === s.key))
    if (inst) inst.setState(s.key)
    log('状态：' + s.label + '（' + stateTip(s.key) + '）')
  }
  stateBox.appendChild(b)
})

function stateTip(k) {
  return {
    idle: '呼吸起伏、偶尔歪头、每 4 秒眨一次眼',
    happy: '笑眼 + 连续蹦跳 + 尾巴摇得飞快',
    play: '笑眼 + 蹦跳 + 摆身',
    eat: '点头咀嚼，身体一张一缩',
    walk: '左右踱步、身体倾斜、爪印',
    hungry: '变灰、动作放慢、垂头打盹'
  }[k] || ''
}

$('toggleRotate').onclick = () => {
  autoRotate = !autoRotate
  if (inst) inst.setAutoRotate(autoRotate)
  $('toggleRotate').textContent = autoRotate ? '🔄 自动转身：开' : '🔄 自动转身：关'
  $('toggleRotate').classList.toggle('off', !autoRotate)
  log(autoRotate ? '模型会自己慢慢转身' : '已固定角度，可手动拖动')
}
$('stage').onclick = () => {
  if (inst) inst.poke()
  log('摸了一下，蹦跶！')
}

if (typeof ResizeObserver !== 'undefined') {
  new ResizeObserver(entries => {
    const r = entries[0].contentRect
    if (inst && r.width && r.height) inst.resize(r.width, r.height)
  }).observe($('stage'))
}

const c0 = PET3D_CONFIG[current]
$('pname').textContent = c0.name
$('pdesc').textContent = c0.desc
log('真 3D · WebGL 实时渲染 · 拖动旋转 / 滚轮缩放')
