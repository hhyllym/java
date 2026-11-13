/**
 * 小马跳跳跑 · 主角 3D 建模
 *
 * 纯 Three.js 几何体程序化拼装（球体 / 锥体 / 圆柱），不加载任何外部模型或贴图。
 * 透明背景、3/4 侧视角、自带渲染循环；腿部 / 鬃毛 / 尾巴独立部件，能跑能跳。
 * 两套造型可切换：cloud（云驰·彩虹鬃毛）、star（星澜·星翼）。
 *
 * 对外：createPonyScene(canvas, opts) → { setState, setVariant, resize, dispose }
 */
import * as THREE from 'three'

const VARIANTS = {
  cloud: {
    body: 0xe9a8dd, bodyLight: 0xfbdcf2, belly: 0xfff0fa, muzzle: 0xffd0e8,
    horn: 0xf0c04a, hoof: 0xa878c8, ear: 0xf08fb8,
    mane: [0xf07ba8, 0x9b6ee0, 0x5aa8e0, 0x6ecfa8],
    tail: [0xf07ba8, 0xf0a45c, 0x9b6ee0, 0x5aa8e0],
    wing: false
  },
  star: {
    body: 0xb08ce8, bodyLight: 0xdcc4f7, belly: 0xf3e9ff, muzzle: 0xe4ccff,
    horn: 0x6fc4ee, hoof: 0x7b5fd0, ear: 0xa886e8,
    mane: [0x7b5fd0, 0x4f7ad8, 0x9b6ee0, 0xe0a0ee],
    tail: [0x7b5fd0, 0x4f7ad8, 0x9b6ee0, 0xe0a0ee],
    wing: true
  }
}

const EYE = 0x4a3f5c
const BLUSH = 0xe878a0

function makeMat(color) {
  return new THREE.MeshPhysicalMaterial({
    color, roughness: .82, metalness: 0,
    sheen: .45, sheenRoughness: .8, sheenColor: new THREE.Color(0xfff2ee),
    clearcoat: .1, clearcoatRoughness: .85
  })
}

export function createPonyScene(canvas, opts = {}) {
  const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, premultipliedAlpha: false })
  renderer.setPixelRatio(Math.min(2, (window.devicePixelRatio || 1)))
  renderer.setClearColor(0x000000, 0)

  const scene = new THREE.Scene()
  // 相机拉近 + 小视角：小画布里主角要够大、够清楚
  const camera = new THREE.PerspectiveCamera(26, 1, .1, 100)
  camera.position.set(1.35, 1.85, 6.0)
  camera.lookAt(0, 1.12, 0)

  scene.add(new THREE.HemisphereLight(0xffffff, 0xd9c8ea, 1.15))
  scene.add(new THREE.AmbientLight(0xffffff, .35))
  const key = new THREE.DirectionalLight(0xffffff, 1.35); key.position.set(3, 6, 6); scene.add(key)
  const fill = new THREE.DirectionalLight(0xffd9ef, .45); fill.position.set(-4, 2, 3); scene.add(fill)
  const rim = new THREE.DirectionalLight(0xc9b0f5, .7); rim.position.set(0, 3, -5); scene.add(rim)

  const root = new THREE.Group()
  scene.add(root)

  /** 记录需要随造型换色的网格 */
  const paints = []
  const paint = (mesh, role, idx = 0) => { paints.push({ mesh, role, idx }); return mesh }

  function ball(parent, r, color, pos, scale, role, idx) {
    const m = new THREE.Mesh(new THREE.SphereGeometry(r, 24, 16), makeMat(color))
    m.position.set(pos[0], pos[1], pos[2])
    if (scale) m.scale.set(scale[0], scale[1], scale[2])
    parent.add(m)
    if (role) paint(m, role, idx)
    return m
  }
  function cone(parent, r, h, color, pos, rotZ, role, idx) {
    const m = new THREE.Mesh(new THREE.ConeGeometry(r, h, 16), makeMat(color))
    m.position.set(pos[0], pos[1], pos[2])
    if (rotZ) m.rotation.z = rotZ
    parent.add(m)
    if (role) paint(m, role, idx)
    return m
  }

  const V0 = VARIANTS[opts.variant] || VARIANTS.cloud

  // —— 身体 / 肚子 ——
  ball(root, .72, V0.body, [0, 1.05, 0], [1, .84, .82], 'body')
  ball(root, .56, V0.belly, [.10, .86, .22], [1, .72, .58], 'belly')

  // —— 头 / 口鼻 ——
  ball(root, .46, V0.bodyLight, [.60, 1.62, .05], [1, .98, .94], 'bodyLight')
  ball(root, .27, V0.muzzle, [.94, 1.47, .10], [1.05, .74, .86], 'muzzle')
  ball(root, .045, BLUSH, [.74, 1.40, .40], [1.3, .8, .5])
  ball(root, .045, BLUSH, [.74, 1.40, -.30], [1.3, .8, .5])

  // 眼睛（朝向 +Z 的一侧能看见）
  ball(root, .092, EYE, [.77, 1.70, .38], [1, 1.15, .62])
  ball(root, .092, EYE, [.77, 1.70, -.28], [1, 1.15, .62])
  ball(root, .032, 0xffffff, [.81, 1.75, .43], [1, 1, 1])
  ball(root, .032, 0xffffff, [.81, 1.75, -.23], [1, 1, 1])

  // 耳朵
  cone(root, .13, .30, V0.ear, [.40, 2.00, .22], -.16, 'ear')
  cone(root, .13, .30, V0.ear, [.40, 2.00, -.18], -.16, 'ear')

  // 独角（微微前倾）
  cone(root, .095, .44, V0.horn, [.55, 2.06, .06], -.22, 'horn')
  // 角上的螺旋纹
  for (let i = 0; i < 3; i++) {
    const ring = new THREE.Mesh(new THREE.TorusGeometry(.078 - i * .014, .012, 8, 18), makeMat(0xfff2c9))
    ring.position.set(.57 + i * .035, 1.98 + i * .12, .06)
    ring.rotation.x = Math.PI / 2
    root.add(ring)
  }

  // —— 鬃毛（彩虹 / 星紫），沿颈后垂下 ——
  const maneSpots = [[.36, 1.94, .02], [.19, 1.84, .02], [.03, 1.70, .02], [-.10, 1.53, .02], [-.18, 1.36, .02]]
  const mane = maneSpots.map((s, i) =>
    ball(root, .26, V0.mane[i % V0.mane.length], s, [.82, .74, .95], 'mane', i))

  // —— 尾巴 ——
  const tailSpots = [[-.74, 1.30, 0], [-.94, 1.20, 0], [-1.10, 1.04, 0], [-1.20, .86, 0]]
  const tail = tailSpots.map((s, i) =>
    ball(root, .22, V0.tail[i % V0.tail.length], s, [.9, .8, 1], 'tail', i))

  // —— 星澜的翅膀（云驰没有） ——
  const wings = new THREE.Group()
  wings.position.set(-.10, 1.30, 0)
  root.add(wings)
  ball(wings, .30, 0xf4ecff, [-.08, .18, .34], [.42, 1.15, .28])
  ball(wings, .30, 0xf4ecff, [-.08, .18, -.34], [.42, 1.15, .28])
  wings.visible = !!V0.wing

  // —— 四条腿（挂到枢轴 Group 上才能摆动） ——
  function leg(x, z) {
    const g = new THREE.Group()
    g.position.set(x, .80, z)
    root.add(g)
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(.105, .085, .54, 14), makeMat(V0.body))
    shin.position.set(0, -.27, 0)
    g.add(shin)
    paints.push({ mesh: shin, role: 'body' })
    ball(g, .12, V0.hoof, [0, -.55, 0], [1, .72, 1.12], 'hoof')
    return g
  }
  const legs = [leg(.34, .26), leg(.34, -.26), leg(-.34, .26), leg(-.34, -.26)]

  // —— 造型切换 ——
  let variant = V0
  function setVariant(k) {
    const v = VARIANTS[k] || VARIANTS.cloud
    variant = v
    for (const p of paints) {
      let c
      if (p.role === 'mane') c = v.mane[p.idx % v.mane.length]
      else if (p.role === 'tail') c = v.tail[p.idx % v.tail.length]
      else c = v[p.role]
      if (c != null) p.mesh.material.color.setHex(c)
    }
    wings.visible = !!v.wing
  }

  // —— 动画 ——
  let state = opts.state || 'idle'
  let t = 0, raf = 0, disposed = false, last = 0
  function frame(now) {
    if (disposed) return
    raf = requestAnimationFrame(frame)
    const dt = last ? Math.min(3, (now - last) / 16.67) : 1
    last = now
    t += .016 * dt

    const running = state === 'run'
    const jumping = state === 'jump'
    const amp = running ? .52 : .06
    const spd = running ? 1 : .35
    const sw = Math.sin(t * 7.5 * spd)

    if (jumping) {
      legs[0].rotation.z = -.65; legs[2].rotation.z = -.45
      legs[1].rotation.z = .55; legs[3].rotation.z = .40
      root.rotation.z = -.10
      root.position.y = 0
    } else {
      legs[0].rotation.z = sw * amp
      legs[3].rotation.z = sw * amp
      legs[1].rotation.z = -sw * amp
      legs[2].rotation.z = -sw * amp
      root.rotation.z = 0
      root.position.y = running ? Math.abs(sw) * .055 : Math.sin(t * 1.6) * .015
    }
    mane.forEach((m, i) => { m.rotation.z = Math.sin(t * 5 + i * .7) * .12 })
    tail.forEach((m, i) => { m.rotation.z = Math.sin(t * 4.2 + i * .6) * .20 })
    if (wings.visible) wings.rotation.z = Math.sin(t * 6) * .10

    renderer.render(scene, camera)
  }

  function resize(w, h) {
    if (!w || !h) return
    renderer.setSize(w, h, false)
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }

  resize(opts.width || canvas.clientWidth || 90, opts.height || canvas.clientHeight || 84)
  raf = requestAnimationFrame(frame)

  return {
    setState(s) { state = s },
    setVariant,
    resize,
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
