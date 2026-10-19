/**
 * 轻量音效模块：Web Audio API 实时合成，无外部音频文件。
 * 所有音效都是振荡器现场"演奏"出来的，体积为零、加载即用。
 *
 * 注意：浏览器要求用户有过一次交互后才允许播放声音，
 * 内部已做 AudioContext resume 处理；用户可在界面上一键静音（localStorage 记忆）。
 */

const MUTE_KEY = 'mlp_muted'

let ctx = null
let muted = false
try { muted = localStorage.getItem(MUTE_KEY) === '1' } catch (e) {}

/** 拿（并按需唤醒）AudioContext */
function ac() {
  if (muted) return null
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext
    if (!AC) return null
    try { ctx = new AC() } catch (e) { return null }
  }
  if (ctx.state === 'suspended') ctx.resume()
  return ctx
}

/**
 * 吹一个音：振荡器 + 包络，可带滑音（吹口哨似的感觉）
 * @param freq    起始频率 Hz
 * @param dur     时长 秒
 * @param type    波形 sine/triangle/square/sawtooth
 * @param gain    音量 0~1
 * @param delay   延迟 秒
 * @param slideTo 滑到的目标频率（可选）
 */
function tone(freq, dur, type = 'sine', gain = 0.1, delay = 0, slideTo = null) {
  const c = ac()
  if (!c) return
  const t0 = c.currentTime + delay
  const osc = c.createOscillator()
  const g = c.createGain()
  osc.type = type
  osc.frequency.setValueAtTime(freq, t0)
  if (slideTo) osc.frequency.exponentialRampToValueAtTime(slideTo, t0 + dur)
  g.gain.setValueAtTime(0, t0)
  g.gain.linearRampToValueAtTime(gain, t0 + 0.012)
  g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur)
  osc.connect(g).connect(c.destination)
  osc.start(t0)
  osc.stop(t0 + dur + 0.05)
}

const sound = {
  get muted() { return muted },

  /** 设置静音并记忆 */
  setMuted(v) {
    muted = !!v
    try { localStorage.setItem(MUTE_KEY, muted ? '1' : '0') } catch (e) {}
  },

  /** 切换静音，返回切换后的状态 */
  toggle() {
    this.setMuted(!muted)
    if (!muted) this.click()   // 解除静音时给个反馈音
    return muted
  },

  /** 按钮点击：清脆上扬 */
  click() { tone(660, 0.09, 'sine', 0.1, 0, 990) },

  /** 悬停轻触：极轻一点 */
  hover() { tone(520, 0.05, 'sine', 0.04) },

  /** 弹出/翻牌：Q 弹 */
  pop() { tone(380, 0.08, 'triangle', 0.09, 0, 720) },

  /** 成功：上行三音 do-mi-sol */
  success() {
    tone(523, 0.12, 'sine', 0.1)
    tone(659, 0.12, 'sine', 0.1, 0.1)
    tone(784, 0.2, 'sine', 0.11, 0.2)
  },

  /** 失败：下行闷音 */
  fail() { tone(320, 0.22, 'sine', 0.09, 0, 170) },

  /** 魔法闪光：高频叮 */
  magic() { tone(880, 0.14, 'triangle', 0.07, 0, 1320) },

  /** 星星闪烁：更细的叮 */
  twinkle() { tone(1174, 0.09, 'sine', 0.055, 0, 1568) }
}

export default sound
