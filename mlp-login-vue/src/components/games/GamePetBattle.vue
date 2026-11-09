<template>
  <div class="game-pb">
    <!-- 选宠阶段 -->
    <div v-if="phase === 'pick'" class="pb-pick">
      <div class="pb-tip">🐾 选一只宠物出战，对手随机匹配：</div>
      <div class="pb-grid">
        <button v-for="p in roster" :key="p.name" class="pb-card" @click="pick(p)">
          <span class="pb-model"><PetSprite :kind="p.kind" :size="56" state="happy" /></span>
          <b>{{ p.name }}</b>
          <em>❤️ {{ p.hp }} · ⚔️ {{ p.atk }}</em>
        </button>
      </div>
      <div class="pb-note">打赢积分多，剩血越多分越高；战败也有安慰分</div>
    </div>

    <!-- 战斗阶段 -->
    <div v-else class="pb-fight">
      <div class="pb-arena">
        <div class="pb-side" :class="{ hurt: meHurt }">
          <span class="pb-model big"><PetSprite :kind="me.kind" :size="88" state="play" /></span>
          <b>{{ me.name }}（你）</b>
          <div class="pb-hp"><i :style="{ width: mePct + '%' }"></i></div>
          <em>{{ meHp }} / {{ me.hp }}</em>
        </div>
        <span class="pb-vs">VS</span>
        <div class="pb-side" :class="{ hurt: foeHurt }">
          <span class="pb-model big foe"><PetSprite :kind="foe.kind" :size="88" state="play" /></span>
          <b>{{ foe.name }}</b>
          <div class="pb-hp"><i :style="{ width: foePct + '%' }"></i></div>
          <em>{{ foeHp }} / {{ foe.hp }}</em>
        </div>
      </div>

      <div class="pb-log" :class="{ crit: lastCrit }">{{ log }}</div>

      <div v-if="!over" class="pb-actions">
        <button class="pb-btn atk" :disabled="busy" @click="attack">⚔️ 攻击</button>
        <button class="pb-btn skill" :disabled="busy || skills <= 0" @click="skill">✨ 技能 ×{{ skills }}</button>
        <button class="pb-btn heal" :disabled="busy || heals <= 0" @click="healUp">🍬 干粮 ×{{ heals }}</button>
        <button class="pb-btn flee" :disabled="busy" @click="finish('flee')">🏃 认输</button>
      </div>

      <div v-else class="pb-over">
        <b>{{ result.title }}</b>
        <span>本局得分 <em>{{ score }}</em></span>
        <button class="pb-btn again" @click="reset">🔄 再来一局</button>
      </div>
    </div>
  </div>
</template>

<script>
/** ⚔️ 宠物单挑：选宠出战打回合制，赢了按剩余血量折算积分（emit over 自动进结算换积分） */
const ROSTER = [
  { icon: '🐶', kind: 'dog', name: '萨摩耶', hp: 54, atk: 10 },
  { icon: '🐱', kind: 'cat', name: '狸花猫', hp: 44, atk: 12 },
  { icon: '🐰', kind: 'rabbit', name: '垂耳兔', hp: 40, atk: 9 },
  { icon: '🦜', kind: 'budgie', name: '虎皮鹦鹉', hp: 36, atk: 13 },
  { icon: '🐹', kind: 'hamster', name: '金丝熊', hp: 48, atk: 9 },
  { icon: '🦊', kind: 'fox', name: '小狐狸', hp: 42, atk: 12 }
]
const rand = (n) => Math.floor(Math.random() * n)
const pick1 = (arr) => arr[rand(arr.length)]
import PetSprite from '../PetSprite.vue'

export default {
  name: 'GamePetBattle',
  components: { PetSprite },
  emits: ['score', 'over'],
  data() {
    return {
      roster: ROSTER,
      phase: 'pick',
      me: ROSTER[0],
      foe: ROSTER[1],
      meHp: 0,
      foeHp: 0,
      foeMax: 0,
      skills: 3,
      heals: 2,
      rounds: 0,
      busy: false,
      over: false,
      win: false,
      result: { title: '' },
      score: 0,
      log: '',
      lastCrit: false,
      meHurt: false,
      foeHurt: false,
      _timers: []
    }
  },
  computed: {
    mePct() { return Math.max(0, Math.round((this.meHp / this.me.hp) * 100)) },
    foePct() { return Math.max(0, Math.round((this.foeHp / this.foeMax) * 100)) }
  },
  beforeUnmount() {
    this._timers.forEach(clearTimeout)
  },
  methods: {
    later(fn, ms) {
      this._timers.push(setTimeout(fn, ms))
    },
    reset() {
      this._timers.forEach(clearTimeout)
      this._timers = []
      this.phase = 'pick'
      this.over = false
      this.busy = false
      this.score = 0
      this.$emit('score', 0)
    },
    pick(p) {
      this.me = p
      const rivals = ROSTER.filter((r) => r.name !== p.name)
      const base = pick1(rivals)
      // 对手血量攻击带一点浮动
      this.foe = { ...base, hp: base.hp + rand(7), atk: base.atk + rand(3) }
      this.foeMax = this.foe.hp
      this.meHp = p.hp
      this.foeHp = this.foe.hp
      this.skills = 3
      this.heals = 2
      this.rounds = 0
      this.over = false
      this.phase = 'fight'
      this.log = `「${this.foe.name}」跳了出来！战斗开始！`
      this.$emit('score', 0)
    },
    /** 我方打一下 */
    strike(mult) {
      const crit = Math.random() < 0.15
      let dmg = this.me.atk + rand(5)
      if (mult > 1) dmg = this.me.atk * 2 + rand(7)
      if (crit) dmg = Math.round(dmg * 1.8)
      this.foeHp -= dmg
      this.foeHurt = true
      this.lastCrit = crit
      this.later(() => { this.foeHurt = false }, 300)
      return { dmg, crit }
    },
    /** 对手反击 */
    counter() {
      const dmg = this.foe.atk + rand(5)
      this.meHp -= dmg
      this.meHurt = true
      this.later(() => { this.meHurt = false }, 300)
      return dmg
    },
    afterPlayerHit(mult) {
      if (this.busy) return
      this.busy = true
      this.rounds += 1
      const { dmg, crit } = this.strike(mult)
      this.log = crit
        ? `会心一击！你对「${this.foe.name}」造成 ${dmg} 点伤害！💥`
        : `你对「${this.foe.name}」造成 ${dmg} 点伤害`
      this.$emit('score', this.currentScore())
      if (this.foeHp <= 0) return this.later(() => this.finish('win'), 450)
      // 对手反击
      this.later(() => {
        const back = this.counter()
        this.log += `；「${this.foe.name}」反击 ${back} 点！`
        if (this.meHp <= 0) this.later(() => this.finish('lose'), 450)
        else this.busy = false
      }, 520)
    },
    attack() {
      this.afterPlayerHit(1)
    },
    skill() {
      if (this.skills <= 0) return
      this.skills -= 1
      this.afterPlayerHit(2)
    },
    healUp() {
      if (this.heals <= 0 || this.busy) return
      this.busy = true
      this.rounds += 1
      this.heals -= 1
      const add = Math.ceil(this.me.hp * 0.35)
      this.meHp = Math.min(this.me.hp, this.meHp + add)
      this.log = `你吃下干粮，回复 ${add} 点血量 🍬`
      this.later(() => {
        const back = this.counter()
        this.log += `；「${this.foe.name}」趁机咬了你 ${back} 点！`
        if (this.meHp <= 0) this.later(() => this.finish('lose'), 450)
        else this.busy = false
      }, 520)
    },
    /** 得分：赢 = 60 + 剩血比例(0~60)；输 = 撑过的回合数 × 4（封顶 36）；认输 = 0 */
    currentScore() {
      return Math.max(0, this.score)
    },
    finish(how) {
      if (this.over) return
      this.over = true
      this.busy = false
      if (how === 'win') {
        const bonus = Math.round((this.meHp / this.me.hp) * 60)
        this.score = 60 + bonus
        this.result = { title: '🏆 胜利！对手被赶跑了' }
      } else if (how === 'lose') {
        this.score = Math.min(36, this.rounds * 4)
        this.result = { title: '💦 战败…下次再战' }
      } else {
        this.score = 0
        this.result = { title: '🏃 你溜了…' }
      }
      this.log = this.result.title
      this.$emit('score', this.score)
      this.later(() => this.$emit('over', this.score), 650)
    }
  }
}
</script>

<style scoped>
.pb-tip { text-align: center; font-size: 13px; font-weight: 700; color: var(--text); margin-bottom: 10px; }
.pb-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px;
  background: var(--panel); border-radius: 16px; padding: 14px;
}
.pb-card {
  border: 2px dashed var(--border); background: var(--card);
  border-radius: 14px; cursor: pointer; padding: 10px 6px 8px;
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  transition: transform .12s, border-color .12s;
}
.pb-card:hover { transform: translateY(-2px); border-color: var(--primary); }
.pb-emoji { font-size: 30px; line-height: 1.2; }
.pb-model { display: inline-flex; align-items: center; justify-content: center; filter: drop-shadow(0 4px 8px rgba(160, 108, 213, .25)); }
.pb-model.big { margin-bottom: 2px; }
.pb-model.foe { transform: scaleX(-1); }
.pb-card b { font-size: 12.5px; color: var(--text); }
.pb-card em { font-style: normal; font-size: 10.5px; color: var(--muted); }
.pb-note { text-align: center; font-size: 11px; color: var(--muted); margin-top: 10px; }

.pb-arena { display: flex; align-items: center; gap: 8px; }
.pb-side {
  flex: 1; background: var(--panel); border-radius: 14px;
  padding: 10px; text-align: center; transition: transform .15s;
}
.pb-side.hurt { animation: pbShake .3s ease; }
@keyframes pbShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
.pb-side .pb-emoji { font-size: 34px; display: block; }
.pb-side b { display: block; font-size: 12px; color: var(--text); margin: 3px 0 6px; }
.pb-side em { font-style: normal; font-size: 10.5px; color: var(--muted); font-variant-numeric: tabular-nums; }
.pb-vs { font-weight: 800; font-size: 15px; color: var(--primary); flex-shrink: 0; }
.pb-hp {
  height: 8px; border-radius: 5px; background: var(--track);
  overflow: hidden; margin-bottom: 4px;
}
.pb-hp i {
  display: block; height: 100%; border-radius: 5px;
  background: linear-gradient(90deg, #7be36a, #4caf7d);
  transition: width .3s ease;
}
.pb-log {
  text-align: center; font-size: 12.5px; font-weight: 700;
  color: var(--text); margin: 12px 0; min-height: 20px;
}
.pb-log.crit { color: #e07b28; }
.pb-actions { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.pb-btn {
  border: none; border-radius: 999px; cursor: pointer;
  padding: 9px 16px; font-size: 13px; font-weight: 700;
  transition: filter .12s, transform .05s;
}
.pb-btn:disabled { opacity: .45; cursor: not-allowed; }
.pb-btn:not(:disabled):active { transform: translateY(1px); }
.pb-btn.atk { background: linear-gradient(135deg, #ff8fa8, #e8556b); color: #fff; }
.pb-btn.skill { background: linear-gradient(135deg, #b79cf0, #8f7ae8); color: #fff; }
.pb-btn.heal { background: linear-gradient(135deg, #a9d6a8, #4caf7d); color: #fff; }
.pb-btn.flee { background: var(--card); color: var(--muted); border: 1.5px solid var(--border); }
.pb-over { text-align: center; padding: 6px 0 2px; }
.pb-over b { display: block; font-size: 15px; color: var(--text); margin-bottom: 4px; }
.pb-over span { font-size: 12.5px; color: var(--muted); }
.pb-over em { font-style: normal; font-weight: 800; color: var(--primary); font-size: 15px; }
.pb-over .again { margin-top: 10px; background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff; }
</style>
