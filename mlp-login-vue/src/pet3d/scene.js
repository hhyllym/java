/**
 * 小马乐园 · 真 3D 宠物渲染器
 *
 * 纯程序化建模（Three.js WebGL 实时渲染），不加载任何外部模型/图片：
 * 用高分段球体 + 锥体 + 管状曲线拼出 Q 版宠物，按部件分组，
 * 因此头、耳、尾、翅膀、眼睑都能单独动 —— 不是贴图平移，是真的 3D 形变。
 *
 * 光照：半球环境光 + 主光 + 补光 + 轮廓光，MeshPhysicalMaterial 的 sheen
 * 模拟绒毛，clearcoat 做眼睛高光；地面用径向渐变贴片做柔和接触影。
 *
 * 对外：createPetScene(canvas, kind, opts) → { setKind, setState, resize,
 *       setAutoRotate, poke, dispose }
 */
import * as THREE from 'three'
import { pet3dConfig } from './config.js'

const GRAY = new THREE.Color(0x9a94a4)

function makeMat(color, opts = {}) {
  const m = new THREE.MeshPhysicalMaterial({
    color,
    roughness: .84,
    metalness: 0,
    sheen: 1,
    sheenRoughness: .82,
    sheenColor: new THREE.Color(0xfff6ec),
    clearcoat: .06,
    clearcoatRoughness: .9,
    ...opts
  })
  m.userData.base = new THREE.Color(color)
  return m
}

/** 一个球（可缩放成椭球），自动记录原始色以便「饿了」变灰后还原 */
function ball(parent, r, color, pos, scale, opts) {
  const m = new THREE.Mesh(new THREE.SphereGeometry(r, 32, 24), makeMat(color, opts))
  m.position.set(pos[0], pos[1], pos[2])
  if (scale) m.scale.set(scale[0], scale[1], scale[2])
  parent.add(m)
  return m
}

/** 细长圆柱（胡须、呆毛、尾羽用） */
function rod(parent, len, thick, color, pos, rot) {
  const m = new THREE.Mesh(new THREE.CylinderGeometry(thick, thick * .7, len, 8), makeMat(color))
  m.position.set(pos[0], pos[1], pos[2])
  if (rot) m.rotation.set(rot[0], rot[1], rot[2])
  parent.add(m)
  return m
}

/** 圆锥（耳朵、喙、冠羽、松果） */
function cone(parent, r, h, color, pos, rot, seg = 12) {
  const m = new THREE.Mesh(new THREE.ConeGeometry(r, h, seg), makeMat(color))
  m.position.set(pos[0], pos[1], pos[2])
  if (rot) m.rotation.set(rot[0], rot[1], rot[2])
  parent.add(m)
  return m
}

/** 弯月牙（笑眼、笑嘴） */
function arc(parent, radius, thick, color, pos, rotZ = 0) {
  const m = new THREE.Mesh(
    new THREE.TorusGeometry(radius, thick, 8, 28, Math.PI),
    makeMat(color, { roughness: .55 })
  )
  m.position.set(pos[0], pos[1], pos[2])
  m.rotation.z = rotZ
  parent.add(m)
  return m
}

/** 包子脸：下巴略收、脸颊略鼓，比纯球更像活物 */
function faceGeometry(r) {
  const g = new THREE.SphereGeometry(r, 40, 32)
  const p = g.attributes.position
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i), z = p.getZ(i)
    const low = Math.max(0, -y / r)
    p.setX(i, x * (1 - .17 * low * low))
    p.setZ(i, z * (1 - .04 * low))
  }
  g.computeVertexNormals()
  return g
}

/** 尾巴：按品种生成不同形态，挂到可旋转的 tailG 上 */
function buildTail(tailG, c) {
  const kind = c.tail || 'ball'
  const main = c.fur, light = c.furLight, tip = c.tailTip || light
  if (kind === 'curl') {
    const curve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(0, 0, 0), new THREE.Vector3(.42, .12, -.16),
      new THREE.Vector3(.72, .46, -.3), new THREE.Vector3(.62, .82, -.22),
      new THREE.Vector3(.34, .9, -.1)
    ])
    const t = new THREE.Mesh(new THREE.TubeGeometry(curve, 30, .135, 10, false), makeMat(main))
    tailG.add(t)
    ball(tailG, .14, light, [.34, .9, -.1])
  } else if (kind === 'bushy') {
    const pts = [[.18, .1, -.1, .26], [.44, .34, -.2, .34], [.62, .66, -.24, .36], [.58, .96, -.18, .3]]
    for (const [x, y, z, r] of pts) ball(tailG, r, main, [x, y, z], [1, 1.05, 1])
    ball(tailG, .26, tip, [.5, 1.12, -.12], [1, 1.15, 1])
  } else if (kind === 'ball') {
    ball(tailG, .24, light, [.16, .08, -.12])
  } else if (kind === 'thin') {
    const curve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(0, 0, 0), new THREE.Vector3(.4, .06, -.2),
      new THREE.Vector3(.76, .34, -.3), new THREE.Vector3(.86, .66, -.24),
      new THREE.Vector3(.72, .82, -.14)
    ])
    tailG.add(new THREE.Mesh(new THREE.TubeGeometry(curve, 32, .045, 8, false), makeMat(main)))
  } else if (kind === 'feather') {
    for (let i = 0; i < 3; i++) {
      const f = ball(tailG, .3, i === 1 ? main : light,
        [.26 + i * .09, .06 + i * .1, -.28 - i * .1], [.28, .95, .5])
      f.rotation.z = -.5 - i * .12
    }
  }
}

/** 耳朵：每种形状单独分组，方便一前一后抖 */
function buildEars(headG, c) {
  const L = new THREE.Group(), R = new THREE.Group()
  headG.add(L, R)
  const e = c.ear || 'triangle'
  const size = c.earBig ? 1.2 : 1
  const dark = c.patch || c.accent

  if (e === 'triangle' || e === 'tuft') {
    const ex = .56 * size, ey = .66, h = .68 * size
    for (const [g, sign] of [[L, -1], [R, 1]]) {
      const outer = cone(g, .29 * size, h, dark || c.fur, [sign * ex, ey, .02], [0, 0, -sign * .2], 4)
      outer.rotation.y = sign * .2
      cone(g, .17 * size, h * .68, c.earInner, [sign * ex, ey - .02, .16], [0, 0, -sign * .2], 4)
      if (e === 'tuft') {
        for (let i = 0; i < 3; i++)
          cone(g, .045, .26, c.furLight, [sign * (ex - .1 + i * .1), ey + h * .48, .02], [0, 0, -sign * .3], 6)
      }
    }
    L.position.set(-.06, 0, 0); R.position.set(.06, 0, 0)
  } else if (e === 'round') {
    const ex = .62 * size, ey = .58, r = .3 * size
    for (const [g, sign] of [[L, -1], [R, 1]]) {
      ball(g, r, dark || c.fur, [sign * ex, ey, .02], [1, 1.05, .55])
      ball(g, r * .62, c.earInner, [sign * ex, ey, .22], [1, 1, .3])
    }
  } else if (e === 'flop') {
    for (const [g, sign] of [[L, -1], [R, 1]]) {
      const ear = ball(g, .23, c.fur, [sign * .62, .12, .04], [.72, 2.15, .62])
      ear.rotation.z = sign * .28
      const inner = ball(g, .13, c.earInner, [sign * .65, .12, .22], [.6, 1.9, .3])
      inner.rotation.z = sign * .28
    }
  } else if (e === 'long') {
    for (const [g, sign] of [[L, -1], [R, 1]]) {
      ball(g, .2, c.fur, [sign * .34, 1.15, -.04], [.72, 2.6, .6])
      ball(g, .11, c.earInner, [sign * .34, 1.2, .14], [.6, 2.3, .3])
    }
  }
  // 无耳（鸟类）也留空组，动画逻辑不用判空
  L.position.x -= .04; R.position.x += .04
  return { L, R }
}

/** 一只眼睛：眼白 + 虹膜 + 瞳孔 + 双高光 + 可闭合的眼睑 + 笑眼月牙 */
function buildEye(headG, sign, c) {
  const g = new THREE.Group()
  g.position.set(sign * .33, .1, -.03)
  headG.add(g)
  const eyeball = new THREE.Group()
  g.add(eyeball)

  // 眼白嵌进脸里，只让虹膜和双高光微微浮出表面，避免"两个黑球凸在脸外"
  ball(eyeball, .205, 0xfffdfa, [0, 0, .65], [1, 1.14, .6])
  ball(eyeball, .135, c.eye, [sign * .012, -.012, .745], [1, 1.1, .42],
    { roughness: .18, clearcoat: 1, clearcoatRoughness: .05, sheen: 0 })
  ball(eyeball, .072, 0x2f2436, [sign * .012, -.018, .795], [1, 1.1, .38],
    { roughness: .12, clearcoat: 1, sheen: 0 })
  ball(eyeball, .048, 0xffffff, [sign * .012 - .048, .065, .83], null, { roughness: .05, sheen: 0 })
  ball(eyeball, .024, 0xffffff, [sign * .012 + .05, -.07, .83], null, { roughness: .05, sheen: 0 })
  // 眼睑：平时完全隐藏（否则眯成一条横线穿过眼睛），只在眨眼瞬间出现
  const lid = ball(g, .215, c.fur, [0, .01, .86], [1.05, .02, .62])
  lid.visible = false
  // 笑眼：弯月牙，开心/玩耍时替代眼球
  const smile = arc(g, .16, .032, 0x5b3f52, [0, .02, .87], Math.PI)
  smile.visible = false
  return { lid, smile, eyeball }
}

function buildPet(root, kind) {
  const c = pet3dConfig(kind)
  const R = { cfg: c, mats: [] }

  // ---------- 身体 ----------
  const bodyG = new THREE.Group()
  root.add(bodyG)
  R.bodyG = bodyG
  ball(bodyG, .82, c.fur, [0, .18, 0], c.body || [.94, .88, .88])
  // 绒毛柔边壳
  const shell = new THREE.Mesh(
    new THREE.SphereGeometry(.82 * (1 + .055 * (c.fluff || 0)), 28, 20),
    new THREE.MeshBasicMaterial({
      color: c.furLight, transparent: true, opacity: .1 + .07 * (c.fluff || 0),
      side: THREE.BackSide, depthWrite: false
    })
  )
  shell.position.y = .18
  shell.scale.set(...(c.body || [.94, .88, .88]))
  bodyG.add(shell)
  // 肚皮
  ball(bodyG, .6, c.belly, [0, .12, .42], [1.02, .92, .52])
  // 爪子
  const pawY = c.shortLegs ? -.36 : -.48
  for (const sx of [-1, 1]) {
    for (const sz of [0, 1]) {
      ball(bodyG, .21, c.furLight, [sx * .42, pawY, .34 - sz * .34], [1, .78, 1.05])
    }
  }
  // 鸟/企鹅的橙脚
  if (c.feet) for (const sx of [-1, 1])
    ball(bodyG, .24, 0xf0a24a, [sx * .3, -.58, .38], [1.1, .4, 1.35])

  // 条纹（狸花猫 / 柴犬脸部）
  if (c.stripe) {
    for (let i = -1; i <= 1; i++)
      ball(bodyG, .12, c.accent, [i * .3, .56, .58], [1.5, .35, .3])
  }

  // 尾巴
  const tailG = new THREE.Group()
  tailG.position.set(.4, .02, -.55)
  bodyG.add(tailG)
  R.tailG = tailG
  buildTail(tailG, c)

  // 翅膀（鸟类）
  R.wingL = new THREE.Group(); R.wingR = new THREE.Group()
  for (const [g, sign] of [[R.wingL, -1], [R.wingR, 1]]) {
    g.position.set(sign * .74, .3, 0)
    bodyG.add(g)
    const w = ball(g, .36, c.accent || c.fur, [sign * .16, -.12, .02], [.5, 1.15, .62])
    w.rotation.z = sign * .2
    ball(g, .2, c.furLight, [sign * .22, -.18, .18], [.5, 1, .4])
  }
  if (!c.wing) { R.wingL.visible = false; R.wingR.visible = false }

  // ---------- 头 ----------
  const headG = new THREE.Group()
  headG.position.set(0, 1.16, .06)
  root.add(headG)
  R.headG = headG
  const hg = faceGeometry(.86)
  const head = new THREE.Mesh(hg, makeMat(c.fur))
  head.scale.set(...(c.head || [1, 1, .94]))
  headG.add(head)
  // 头部绒毛柔边
  const hshell = new THREE.Mesh(
    new THREE.SphereGeometry(.86 * (1 + .05 * (c.fluff || 0)), 28, 20),
    new THREE.MeshBasicMaterial({
      color: c.furLight, transparent: true, opacity: .12 + .1 * (c.fluff || 0),
      side: THREE.BackSide, depthWrite: false
    })
  )
  hshell.scale.set(...(c.head || [1, 1, .94]))
  headG.add(hshell)

  // 重点色面罩 / 脸部浅斑
  if (c.patch) ball(headG, .54, c.patch, [0, .3, .5], [1.12, .78, .5])
  if (c.facePatch) ball(headG, .52, c.facePatch, [0, -.06, .58], [1.16, .92, .42])
  // 额头条纹
  if (c.stripe) for (let i = -1; i <= 1; i++)
    rod(headG, .22, .035, c.accent, [i * .13, .68, .52], [0, 0, i * .2])

  // 耳朵
  const ears = buildEars(headG, c)
  R.earL = ears.L; R.earR = ears.R
  if (c.ear === 'none') { ears.L.visible = false; ears.R.visible = false }

  // 冠羽（玄凤）
  if (c.crest) for (let i = -1; i <= 1; i++)
    cone(headG, .075, .62, c.furLight, [i * .13, 1.06, -.06], [-.3, 0, -i * .28], 8)

  // 眼睛
  const eL = buildEye(headG, -1, c), eR = buildEye(headG, 1, c)
  R.lids = [eL.lid, eR.lid]
  R.smiles = [eL.smile, eR.smile]
  R.eyes = [eL.eyeball, eR.eyeball]

  // 腮红 / 鼓腮
  const cheekR = c.cheek ? .3 : .18
  for (const sign of [-1, 1]) {
    const ch = ball(headG, cheekR, c.cheek ? c.furLight : c.blush,
      [sign * .58, -.22, .48], [1, .62, .3],
      c.cheek ? {} : { transparent: true, opacity: .62, depthWrite: false })
    ch.rotation.z = sign * .1
  }
  // 鸟类橙色圆腮
  if (c.blush && (c.beak)) for (const sign of [-1, 1])
    ball(headG, .19, c.blush, [sign * .52, -.16, .56], [1, .85, .28],
      { transparent: true, opacity: .8, depthWrite: false })

  // 吻部 + 鼻子 + 嘴（吻部微微贴出脸面，不做成凸起的"口罩"）
  ball(headG, .26, c.belly, [0, -.32, .66], [1.08, .62, .42])
  if (c.beak) {
    cone(headG, .17, .3, 0xffc061, [0, -.26, .86], [Math.PI / 2, 0, 0], 10)
    cone(headG, .13, .22, 0xf0a24a, [0, -.38, .84], [Math.PI / 2, 0, 0], 10)
  } else {
    ball(headG, .095, c.nose, [0, -.22, .9], [1.25, .8, .6], { roughness: .35, clearcoat: .6 })
    arc(headG, .13, .022, 0x8a5f70, [0, -.44, .84], Math.PI)
    if (c.smile) {
      // 吐小舌头
      ball(headG, .1, 0xff8fae, [0, -.53, .84], [.9, 1.2, .35])
    }
  }

  // 胡须只给鼠类，放在吻部两侧（不横穿眼睛）
  if (c.whisker) for (const sign of [-1, 1]) for (let i = 0; i < 2; i++)
    rod(headG, .34, .007, 0xffffff, [sign * .68, -.34 - i * .1, .52],
      [0, 0, Math.PI / 2 + sign * (i ? -.2 : .2)])

  // 手持物
  if (c.hold === 'seed') {
    ball(root, .1, 0xb98a52, [0, .28, .86], [.7, 1.3, .6])
    for (const sign of [-1, 1]) ball(root, .15, c.furLight, [sign * .18, .22, .78])
  } else if (c.hold === 'cone') {
    cone(root, .16, .34, 0xb3793f, [0, .22, .88], [0, 0, 0], 8)
    for (const sign of [-1, 1]) ball(root, .15, c.furLight, [sign * .19, .2, .8])
  }

  // 收集材质，供「饿了」整体变灰
  root.traverse(o => { if (o.isMesh && o.material && o.material.color) R.mats.push(o.material) })
  return R
}

function shadowTexture() {
  const cv = document.createElement('canvas')
  cv.width = cv.height = 128
  const x = cv.getContext('2d')
  const g = x.createRadialGradient(64, 64, 0, 64, 64, 64)
  g.addColorStop(0, 'rgba(108,76,150,0.40)')
  g.addColorStop(.5, 'rgba(108,76,150,0.15)')
  g.addColorStop(1, 'rgba(108,76,150,0)')
  x.fillStyle = g
  x.fillRect(0, 0, 128, 128)
  const t = new THREE.CanvasTexture(cv)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

export function createPetScene(canvas, kind, opts = {}) {
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, premultipliedAlpha: false })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
  renderer.outputColorSpace = THREE.SRGBColorSpace
  // Neutral 保色更好，白毛不会被压成灰（老版本 Three 回退到 ACES）
  renderer.toneMapping = THREE.NeutralToneMapping !== undefined
    ? THREE.NeutralToneMapping : THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.22

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(33, 1, .1, 100)
  camera.position.set(0, 1.02, 6.1)
  camera.lookAt(0, .72, 0)

  scene.add(new THREE.HemisphereLight(0xfffaf2, 0xe8daf4, 1.45))
  const key = new THREE.DirectionalLight(0xfff8ec, 1.6); key.position.set(3, 5, 5); scene.add(key)
  const fill = new THREE.DirectionalLight(0xffd8e8, .55); fill.position.set(-4, 2, 3); scene.add(fill)
  const rim = new THREE.DirectionalLight(0xcbb8ff, .65); rim.position.set(0, 3, -5); scene.add(rim)

  const shadowMat = new THREE.MeshBasicMaterial({
    map: shadowTexture(), transparent: true, depthWrite: false, opacity: .95
  })
  const shadow = new THREE.Mesh(new THREE.PlaneGeometry(3.4, 3.4), shadowMat)
  shadow.rotation.x = -Math.PI / 2
  shadow.position.y = -.72
  scene.add(shadow)

  const petRoot = new THREE.Group()
  scene.add(petRoot)

  let R = null
  let state = opts.state || 'idle'
  let autoRotate = opts.autoRotate !== false
  let interactive = opts.interactive !== false
  let userYaw = 0, userPitch = 0, dragging = false, lastX = 0, lastY = 0
  let pokeUntil = 0
  let raf = 0, disposed = false
  const t0 = performance.now()

  function rebuild(k) {
    if (R) {
      petRoot.traverse(o => {
        if (o.isMesh) { o.geometry.dispose(); if (o.material.dispose) o.material.dispose() }
      })
      petRoot.clear()
    }
    R = buildPet(petRoot, k)
    applyGray(state === 'hungry')
  }

  function applyGray(on) {
    if (!R) return
    for (const m of R.mats) {
      if (!m.userData.base) m.userData.base = m.color.clone()
      m.color.copy(m.userData.base)
      if (on) m.color.lerp(GRAY, .58)
    }
  }

  function setState(s) {
    state = s || 'idle'
    applyGray(state === 'hungry')
    if (R) for (const sm of R.smiles) sm.visible = false
  }

  function resize(w, h) {
    if (!w || !h) return
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }

  function onDown(e) {
    if (!interactive) return
    dragging = true; lastX = e.clientX; lastY = e.clientY
    canvas.setPointerCapture && canvas.setPointerCapture(e.pointerId)
    canvas.style.cursor = 'grabbing'
  }
  function onMove(e) {
    if (!dragging) return
    userYaw += (e.clientX - lastX) * .008
    userPitch = Math.max(-.45, Math.min(.4, userPitch + (e.clientY - lastY) * .005))
    lastX = e.clientX; lastY = e.clientY
  }
  function onUp() { dragging = false; canvas.style.cursor = 'grab' }
  function onWheel(e) {
    if (!interactive) return
    e.preventDefault()
    camera.position.z = Math.max(4.2, Math.min(8.5, camera.position.z + e.deltaY * .004))
  }
  function onLeave() { dragging = false; canvas.style.cursor = 'grab' }

  if (interactive) {
    canvas.style.cursor = 'grab'
    canvas.addEventListener('pointerdown', onDown)
    canvas.addEventListener('pointermove', onMove)
    canvas.addEventListener('pointerup', onUp)
    canvas.addEventListener('pointerleave', onLeave)
    canvas.addEventListener('wheel', onWheel, { passive: false })
  }

  function frame(now) {
    if (disposed) return
    const t = (now - t0) / 1000
    if (!R) { raf = requestAnimationFrame(frame); return }
    const c = R.cfg
    const slow = state === 'hungry' ? .42 : 1
    const ts = t * slow

    // 呼吸：身体纵向起伏 + 横向反相 + 整体轻浮
    const breath = Math.sin(ts * 2.35)
    R.bodyG.scale.y = 1 + breath * .03
    R.bodyG.scale.x = 1 - breath * .018
    petRoot.position.y = breath * .022

    // 缓慢自转，把立体感转出来；拖动时以用户角度为准
    const idleYaw = autoRotate && !dragging ? Math.sin(ts * .38) * .48 : 0
    petRoot.rotation.y = userYaw + idleYaw
    petRoot.rotation.x = userPitch
    petRoot.rotation.z = 0
    petRoot.position.x = 0

    // 歪头 + 左右张望
    R.headG.rotation.z = Math.sin(ts * .66) * .055 + (Math.sin(ts * .31) > .988 ? -.15 : 0)
    R.headG.rotation.y = Math.sin(ts * .48) * .09
    R.headG.rotation.x = 0

    // 眨眼：每 4.2 秒一次，快速闭合再睁开
    const ph = (ts % 4.2) / 4.2
    const blink = Math.max(0, 1 - Math.abs(ph - .905) / .028)
    const isSmile = state === 'happy' || state === 'play'
    for (const e of R.eyes) e.visible = !isSmile
    for (const lid of R.lids) {
      lid.visible = !isSmile && blink > .01
      lid.scale.y = .02 + blink * 1.16
    }
    for (const sm of R.smiles) sm.visible = isSmile

    // 尾巴 / 翅膀 / 耳朵
    const speed = c.tailSpeed || 1.6
    R.tailG.rotation.z = Math.sin(ts * speed) * .34
    R.tailG.rotation.y = Math.sin(ts * speed * .7) * .12
    if (c.wing) {
      R.wingL.rotation.z = -Math.sin(ts * 3.8) * .34
      R.wingR.rotation.z = Math.sin(ts * 3.8) * .34
    }
    const twitch = Math.sin(ts * .93) > .97 ? -.22 : 0
    R.earL.rotation.z = Math.sin(ts * .85) * .05 + twitch
    R.earR.rotation.z = -Math.sin(ts * .85 + .4) * .05 - twitch

    // 状态动作
    if (state === 'happy' || state === 'play') {
      const hop = Math.abs(Math.sin(ts * 5.1))
      petRoot.position.y += hop * .3
      petRoot.rotation.z = Math.sin(ts * 5.1) * .09
      R.tailG.rotation.z *= 2
    } else if (state === 'eat') {
      R.headG.rotation.x = Math.sin(ts * 9.5) * .11
      petRoot.position.y += Math.abs(Math.sin(ts * 9.5)) * .04
      R.bodyG.scale.y += Math.sin(ts * 9.5) * .015
    } else if (state === 'walk') {
      petRoot.position.x = Math.sin(ts * 2.5) * .3
      petRoot.position.y += Math.abs(Math.sin(ts * 5)) * .1
      petRoot.rotation.z = Math.sin(ts * 2.5) * -.09
      R.tailG.rotation.z *= 1.6
    } else if (state === 'hungry') {
      R.headG.rotation.z += .1
    }
    if (now < pokeUntil) {
      petRoot.position.y += .22 * Math.sin((pokeUntil - now) / 300 * Math.PI)
      petRoot.rotation.z += .12 * Math.sin((pokeUntil - now) / 300 * Math.PI)
    }

    shadow.scale.setScalar(1 - breath * .05)
    renderer.render(scene, camera)
    raf = requestAnimationFrame(frame)
  }

  rebuild(kind)
  resize(opts.width || canvas.clientWidth || 300, opts.height || canvas.clientHeight || 300)
  raf = requestAnimationFrame(frame)

  return {
    setKind: rebuild,
    setState,
    resize,
    setAutoRotate(v) { autoRotate = !!v },
    poke() { pokeUntil = performance.now() + 320 },
    dispose() {
      disposed = true
      cancelAnimationFrame(raf)
      canvas.removeEventListener('pointerdown', onDown)
      canvas.removeEventListener('pointermove', onMove)
      canvas.removeEventListener('pointerup', onUp)
      canvas.removeEventListener('pointerleave', onLeave)
      canvas.removeEventListener('wheel', onWheel)
      petRoot.traverse(o => {
        if (o.isMesh) { o.geometry.dispose(); if (o.material.dispose) o.material.dispose() }
      })
      shadow.geometry.dispose(); shadowMat.map.dispose(); shadowMat.dispose()
      renderer.dispose()
    }
  }
}
