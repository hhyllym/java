/**
 * 小马乐园 · 真 3D 宠物模型参数表（14 个真实品种）
 *
 * 这些参数驱动 pet3d/scene.js 里的程序化建模：同一套骨架，
 * 靠毛色 / 耳型 / 尾型 / 翅膀 / 喙 / 冠羽 / 腮帮 / 花纹 / 手持物
 * 组合出 14 只长得完全不同的小家伙。全部原创，无外部模型素材。
 *
 * ear   triangle 尖耳 | round 圆耳 | flop 垂耳 | long 长竖耳 | tuft 簇毛尖耳
 * tail  curl 卷尾 | bushy 大蓬尾 | ball 球尾 | thin 细长尾 | feather 尾羽 | none 无
 */
export const PET3D_KINDS = [
  'cat', 'dog', 'bird', 'rabbit', 'fox', 'bear', 'hamster', 'penguin',
  'mouse', 'squirrel', 'ragdoll', 'corgi', 'shiba', 'budgie'
]

export const PET3D_CONFIG = {
  cat: {
    name: '狸花猫', group: '猫', desc: '棕灰虎斑，卷卷尾巴，眼神最机灵',
    fur: 0xc2a07c, furLight: 0xf2e1c6, belly: 0xfff4e4, earInner: 0xf2a8bb,
    eye: 0x86ae67, nose: 0xe08fa6, blush: 0xf09bb0, accent: 0x8b6c4d,
    ear: 'triangle', tail: 'curl', stripe: true, fluff: .35,
    head: [1.02, 1, .94], body: [.94, .88, .86], tailSpeed: 1.7
  },
  dog: {
    name: '萨摩耶', group: '狗', desc: '一身白云，永远在笑',
    fur: 0xfffaf0, furLight: 0xffffff, belly: 0xfffdf8, earInner: 0xffd9cf,
    eye: 0x4a382c, nose: 0x5b4638, blush: 0xffb3bd, accent: 0xe8dcc8,
    ear: 'triangle', tail: 'curl', smile: true, fluff: .95,
    head: [1.04, 1, .95], body: [.98, .9, .9], tailSpeed: 3.6
  },
  bird: {
    name: '玄凤鹦鹉', group: '鸟', desc: '黄冠羽 + 橙腮红，会歪头看你',
    fur: 0xf7e3a8, furLight: 0xfff3cf, belly: 0xfff9e8, earInner: 0xf6a94e,
    eye: 0x3b3630, nose: 0xd9a552, blush: 0xf79a4e, accent: 0xc9cdd2,
    ear: 'none', tail: 'feather', wing: true, beak: true, crest: true,
    fluff: .3, head: [1, 1.02, .94], body: [.94, 1.02, .9], tailSpeed: 1.5
  },
  rabbit: {
    name: '垂耳兔', group: '兔', desc: '长耳一前一后，胆子小小',
    fur: 0xfaf0ec, furLight: 0xfffaf7, belly: 0xffffff, earInner: 0xf7b9c9,
    eye: 0x8a5c72, nose: 0xeba0b4, blush: 0xf5a9bc, accent: 0xe6cfc8,
    ear: 'flop', tail: 'ball', fluff: .5,
    head: [1, .97, .94], body: [.94, .92, .88], tailSpeed: 1.4
  },
  fox: {
    name: '赤狐', group: '狐', desc: '大尾巴扫来扫去，鬼点子最多',
    fur: 0xe98b47, furLight: 0xffc28a, belly: 0xfff3e5, earInner: 0x8a4a2c,
    eye: 0x9c6a2c, nose: 0x4a3122, blush: 0xf0a07a, accent: 0xb9602f,
    ear: 'triangle', tail: 'bushy', tailTip: 0xfff6e8, fluff: .6,
    head: [1.04, .96, .94], body: [.94, .88, .88], tailSpeed: 1.25
  },
  bear: {
    name: '棕熊', group: '熊', desc: '圆滚滚，走路慢悠悠',
    fur: 0xc79a6e, furLight: 0xe6c39c, belly: 0xf7e5cd, earInner: 0xa9784f,
    eye: 0x4a3122, nose: 0x4a3122, blush: 0xe0a98a, accent: 0xa87c55,
    ear: 'round', tail: 'ball', fluff: .55,
    head: [1.06, 1.02, .96], body: [1.02, .96, .94], tailSpeed: 1.0
  },
  hamster: {
    name: '金丝熊', group: '鼠', desc: '腮帮塞满瓜子，一鼓一鼓',
    fur: 0xeab566, furLight: 0xffe0a8, belly: 0xfff4de, earInner: 0xf3a8ad,
    eye: 0x3d2a1c, nose: 0xe38f9c, blush: 0xf2a6a6, accent: 0xc98c48,
    ear: 'round', tail: 'ball', cheek: .95, hold: 'seed', fluff: .45,
    head: [1.08, .98, .96], body: [1, .92, .94], tailSpeed: 1.6
  },
  penguin: {
    name: '小企鹅', group: '鸟', desc: '左右摇摆，翅膀拍个不停',
    fur: 0x6f86a8, furLight: 0x9db4d2, belly: 0xffffff, earInner: 0x6f86a8,
    eye: 0x25384f, nose: 0xf0a24a, blush: 0xf2a0b0, accent: 0x4d6486,
    ear: 'none', tail: 'none', wing: true, beak: true, feet: true,
    fluff: .25, head: [1, 1.08, .94], body: [.94, 1.08, .9], tailSpeed: 1.8
  },
  mouse: {
    name: '花枝鼠', group: '鼠', desc: '大圆耳、细长尾，跑得飞快',
    fur: 0xf2f4fa, furLight: 0xffffff, belly: 0xffffff, earInner: 0xf7b9c9,
    eye: 0x383851, nose: 0xef9db4, blush: 0xf3aec1, accent: 0xc3cadd,
    ear: 'round', earBig: true, tail: 'thin', whisker: true, fluff: .2,
    head: [1.02, .94, .94], body: [.9, .88, .86], tailSpeed: 2.6
  },
  squirrel: {
    name: '红松鼠', group: '鼠', desc: '蓬松大尾，松果永远抱着',
    fur: 0xd9874a, furLight: 0xf6bd85, belly: 0xffeed6, earInner: 0xc06a34,
    eye: 0x50301c, nose: 0xa9683e, blush: 0xeea07a, accent: 0xb26c36,
    ear: 'tuft', tail: 'bushy', tailTip: 0xffd9a8, hold: 'cone', fluff: .7,
    head: [1.02, .98, .94], body: [.92, .9, .88], tailSpeed: 1.5
  },
  ragdoll: {
    name: '布偶猫', group: '猫', desc: '蓝眼睛的重点色小仙女',
    fur: 0xf3e7d8, furLight: 0xfffaf3, belly: 0xfffaf3, earInner: 0xe0b6b0,
    eye: 0x4a86c8, nose: 0xe59bb0, blush: 0xf2aec0, accent: 0x9b7a67,
    ear: 'triangle', tail: 'bushy', patch: 0x9b7a67, fluff: .8,
    head: [1.04, 1, .95], body: [.98, .92, .92], tailSpeed: 1.3
  },
  corgi: {
    name: '柯基犬', group: '狗', desc: '大立耳、小短腿，屁股最圆',
    fur: 0xe9a45c, furLight: 0xffd9a8, belly: 0xfff6e8, earInner: 0xf5b9ae,
    eye: 0x503020, nose: 0x4a3226, blush: 0xf2a89a, accent: 0xc97f42,
    ear: 'triangle', earBig: true, tail: 'ball', shortLegs: true, smile: true,
    fluff: .45, head: [1.06, .96, .94], body: [1.08, .78, .96], tailSpeed: 3.2
  },
  shiba: {
    name: '柴犬', group: '狗', desc: '赤芝麻色，卷尾摇成小风扇',
    fur: 0xdb9048, furLight: 0xf7c68e, belly: 0xfff2df, earInner: 0xc97636,
    eye: 0x4a3122, nose: 0x3d2a20, blush: 0xf0a888, accent: 0xb96e32,
    ear: 'triangle', tail: 'curl', smile: true, fluff: .5,
    head: [1.04, .98, .95], body: [.96, .9, .9], tailSpeed: 3.0
  },
  budgie: {
    name: '虎皮鹦鹉', group: '鸟', desc: '天蓝小胖球，脸上有小斑点',
    fur: 0x8fcbe8, furLight: 0xc9ecfb, belly: 0xf4fbff, earInner: 0xffe79a,
    eye: 0x2f465c, nose: 0xd9b96a, blush: 0xf2b0c0, accent: 0x5fa8d2,
    ear: 'none', tail: 'feather', wing: true, beak: true, facePatch: 0xffe79a,
    fluff: .25, head: [1, 1, .94], body: [.94, 1, .9], tailSpeed: 1.9
  }
}

/** 六种状态，页面按钮与组件共用 */
export const PET3D_STATES = [
  { key: 'idle', label: '发呆', icon: '🫧' },
  { key: 'happy', label: '开心', icon: '💗' },
  { key: 'play', label: '玩耍', icon: '🎾' },
  { key: 'eat', label: '吃饭', icon: '🍖' },
  { key: 'walk', label: '散步', icon: '🐾' },
  { key: 'hungry', label: '饿了', icon: '💤' }
]

export function pet3dConfig(kind) {
  return PET3D_CONFIG[String(kind || '').toLowerCase()] || PET3D_CONFIG.cat
}
