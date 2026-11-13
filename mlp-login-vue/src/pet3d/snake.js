/**
 * 彩虹苹果贪吃蛇 · Q 版 3D 建模
 *
 * 纯 Three.js 几何体程序化拼装，不加载任何外部模型 / 贴图。
 * - 棋盘模式：正交俯视相机，格子坐标与 12×12 网格严格对齐；蛇身 / 蛇头 / 苹果都是 3D 小球。
 * - 立绘模式（portrait）：3/4 视角，只展示一只放大的 Q 版蛇头，用于准备页。
 *
 * 对外：createSnakeScene(canvas, opts) → { setSegments, setFood, setDirection, setState, resize, dispose }
 */
import * as THREE from 'three'

const N = 12
const HALF = N / 2

const C = {
  head: 0xf59ad4, headLight: 0xffd9f0, face: 0xfff0fa,
  eye: 0x463a58, eyeRim: 0x7a5a90, blush: 0xf07ba8, horn: 0xf0c04a,
  body: [0x8fdcc0, 0x7fc4ef, 0xb79bef, 0xf7a8d8],
  apple: 0xe0555f, appleLight: 0xff9d9d, gold: 0xf5be37,
  leaf: 0x7fd4a8, stem: 0x8a5a3b
}

function mat(color, extra = {}) {
  return new THREE.MeshPhysicalMaterial({
    color, roughness: .5, metalness: 0,
    clearcoat: .4, clearcoatRoughness: .45,
    sheen: .55, sheenColor: new THREE.Color(0xffffff),
    ...extra
  })
}

function ball(parent, r, color, pos, scale, extra) {
  const m = new THREE.Mesh(new THREE.SphereGeometry(r, 20, 14), mat(color, extra))
  m.position.set(pos[0], pos[1], pos[2])
  if (scale) m.scale.set(scale[0], scale[1], scale[2])
  parent.add(m)
  return m
}

export function createSnakeScene(canvas, opts = {}) {
  const portrait = !!opts.portrait
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, premultipliedAlpha: false })
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1))
  renderer.setClearColor(0x000000, 0)

  const scene = new THREE.Scene()
  let camera
  if (portrait) {
    // 高位略倾斜的俯视：和棋盘里的正俯视 Q 版脸一致，五官不会被侧视角挡住
    camera = new THREE.PerspectiveCamera(32, 1, .1, 100)
    camera.position.set(.5, 4.8, 1.0)
    camera.lookAt(0, 0, 0)
  } else {
    // 正俯视正交相机：世界 x = 列，世界 z = 行，与 CSS 网格逐格对齐
    camera = new THREE.OrthographicCamera(-HALF, HALF, HALF, -HALF, .1, 100)
    camera.position.set(0, 14, 0)
    camera.up.set(0, 0, -1)
    camera.lookAt(0, 0, 0)
  }

  scene.add(new THREE.HemisphereLight(0xffffff, 0xe4d3f2, 1.35))
  const key = new THREE.DirectionalLight(0xffffff, 1.45); key.position.set(4, 9, 5); scene.add(key)
  const fill = new THREE.DirectionalLight(0xffd6ee, .55); fill.position.set(-5, 5, -3); scene.add(fill)
  const rim = new THREE.DirectionalLight(0xbfe6ff, .65); rim.position.set(0, 4, -6); scene.add(rim)

  const root = new THREE.Group()
  scene.add(root)
  if (portrait) root.scale.setScalar(1.9)

  // ---------- Q 版蛇头（脸固定朝向屏幕，俯视下也是标准左右眼） ----------
  const head = new THREE.Group()
  root.add(head)
  ball(head, .50, C.head, [0, 0, 0], [1.08, .82, 1])
  // 浅色脸盘
  ball(head, .37, C.face, [0, .32, .08], [1, .40, .95])
  // 两只大眼睛：沿 x 左右分开，深色眼眶 → 白眼 → 深瞳 → 高光
  for (const x of [-.20, .20]) {
    ball(head, .155, C.eyeRim, [x, .45, .16], [1, .55, 1])
    ball(head, .132, 0xffffff, [x, .50, .17], [1, .55, 1])
    ball(head, .082, C.eye, [x, .55, .18], [1, .55, 1])
    ball(head, .034, 0xffffff, [x - .03, .58, .15])
  }
  // 腮红
  for (const x of [-.34, .34]) ball(head, .095, C.blush, [x, .42, .04], [1.2, .38, 1])
  // 扁扁的微笑嘴
  ball(head, .062, C.eye, [0, .44, .36], [1.5, .38, 1])
  // 小独角 / 呆毛（头顶后方）
  const horn = new THREE.Mesh(new THREE.ConeGeometry(.095, .30, 14), mat(C.horn))
  horn.position.set(0, .62, -.20)
  horn.rotation.z = -.30
  head.add(horn)
  ball(head, .085, C.headLight, [0, .42, -.34], [1, .6, 1])

  // ---------- 蛇身（网格池，按需增减） ----------
  const bodies = []
  function ensureBodies(n) {
    while (bodies.length < n) {
      const g = new THREE.Group()
      root.add(g)
      ball(g, .36, C.body[0], [0, 0, 0], [1, .82, 1])
      ball(g, .075, 0xffffff, [.12, .24, -.11], [1, .5, 1], { roughness: .9, clearcoat: 0 })
      bodies.push(g)
    }
    bodies.forEach((g, i) => { g.visible = i < n })
  }

  // ---------- 苹果 ----------
  const food = new THREE.Group()
  root.add(food)
  const appleBall = ball(food, .33, C.apple, [0, 0, 0])
  ball(food, .095, C.appleLight, [.14, .23, -.11], [1, .6, 1])
  const stem = new THREE.Mesh(new THREE.CylinderGeometry(.028, .035, .20, 10), mat(C.stem))
  stem.position.set(0, .36, 0); stem.rotation.z = .25; food.add(stem)
  const leaf = ball(food, .11, C.leaf, [.15, .38, 0], [1.3, .32, .8])
  const halo = new THREE.Mesh(new THREE.TorusGeometry(.42, .035, 8, 28), mat(C.gold, { emissive: 0x6b4a00, emissiveIntensity: .35 }))
  halo.rotation.x = Math.PI / 2
  halo.position.y = -.18
  halo.visible = false
  food.add(halo)

  // ---------- 状态 ----------
  let state = 'idle'
  let dir = { x: 1, y: 0 }
  let targetRot = 0, curRot = 0
  let t = 0, raf = 0, disposed = false, last = 0
  let foodGold = false

  const wx = x => x + .5 - HALF
  const wz = y => y + .5 - HALF

  function setSegments(arr) {
    if (!arr || !arr.length) return
    const h = arr[0]
    head.position.set(wx(h.x), .34, wz(h.y))
    ensureBodies(arr.length - 1)
    for (let i = 1; i < arr.length; i++) {
      const g = bodies[i - 1]
      const s = arr[i]
      const k = (i - 1) / Math.max(1, arr.length - 2)
      g.position.set(wx(s.x), .30, wz(s.y))
      const shrink = 1 - Math.min(.28, k * .28)
      g.scale.setScalar(shrink)
      const c = new THREE.Color(C.body[Math.min(C.body.length - 1, Math.floor(k * C.body.length))])
      g.children[0].material.color.copy(c)
    }
  }

  function setFood(f) {
    if (!f) { food.visible = false; return }
    food.visible = true
    food.position.set(wx(f.x), .32, wz(f.y))
    foodGold = !!f.gold
    appleBall.material.color.setHex(foodGold ? C.gold : C.apple)
    leaf.material.color.setHex(foodGold ? 0xfff0a8 : C.leaf)
    halo.visible = foodGold
  }

  function setDirection(d) {
    if (!d) return
    dir = d
    // 脸默认朝 +x；atan2(-y, x) 得到绕 Y 的朝向
    targetRot = Math.atan2(-d.y, d.x)
  }

  function setState(s) { state = s }

  function resize(w, h) {
    if (!w || !h) return
    renderer.setSize(w, h, false)
    if (portrait) { camera.aspect = w / h; camera.updateProjectionMatrix() }
  }

  function frame(now) {
    if (disposed) return
    raf = requestAnimationFrame(frame)
    const dt = last ? Math.min(3, (now - last) / 16.67) : 1
    last = now
    t += .016 * dt

    // 朝向缓动
    let diff = targetRot - curRot
    while (diff > Math.PI) diff -= Math.PI * 2
    while (diff < -Math.PI) diff += Math.PI * 2
    curRot += diff * Math.min(1, .18 * dt)
    // 脸基本固定朝向观众，只随前进方向轻轻摆一点，保证左右眼始终看得清
    head.rotation.y = portrait ? Math.sin(t * .8) * .10 : curRot * .22

    const bob = Math.sin(t * 2.2) * .02
    const baseY = portrait ? 0 : .34
    head.position.y = baseY + bob + (state === 'over' && !portrait ? -.12 : 0)
    if (portrait) {
      head.rotation.z = Math.sin(t * 1.5) * .07
      root.position.y = Math.sin(t * 1.8) * .05
    }
    if (foodGold) {
      halo.rotation.z += .02 * dt
      const s = 1 + Math.sin(t * 5) * .12
      halo.scale.setScalar(s)
    }
    if (state === 'idle' && !portrait) {
      bodies.forEach((g, i) => { g.position.y = .30 + Math.sin(t * 3 + i * .6) * .015 })
    }
    renderer.render(scene, camera)
  }

  // 初始：一条 3 节小蛇，立绘模式只显示头
  setSegments([{ x: 4, y: 6 }, { x: 3, y: 6 }, { x: 2, y: 6 }])
  setFood({ x: 8, y: 6, gold: false })
  if (portrait) {
    bodies.forEach(g => { g.visible = false })
    food.visible = false
    head.position.set(0, 0, 0)
    head.scale.setScalar(1.15)
  }
  resize(opts.width || canvas.clientWidth || 320, opts.height || canvas.clientHeight || 320)
  raf = requestAnimationFrame(frame)

  return {
    setSegments, setFood, setDirection, setState, resize,
    dispose() {
      disposed = true
      cancelAnimationFrame(raf)
      scene.traverse(o => {
        if (o.isMesh) {
          o.geometry.dispose()
          if (o.material) o.material.dispose()
        }
      })
      renderer.dispose()
    }
  }
}
