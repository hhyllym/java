<template>
  <div class="pet-page">
    <!-- 头部：返回 + 标题 -->
    <div class="pet-head">
      <button class="pet-back" title="返回主页" @click="$router.push({ name: 'home' })">«</button>
      <div class="pet-title">
        <h3>🐾 我的宠物</h3>
        <span>喂喂它、陪它玩，看它一天天长大</span>
      </div>
    </div>

    <div v-if="petLoading" class="pet-tip">看看小窝里有没有小家伙…</div>

    <!-- 宠物本体：会动的 Q 版小家伙 + 小舞台 -->
    <div v-else-if="pet" class="pet-card">
      <div class="pet-stage" @click="pokePet" title="点一下摸摸它">
        <span class="stage-cloud cloud-a"></span>
        <span class="stage-cloud cloud-b"></span>
        <span class="stage-cloud cloud-c"></span>
        <span class="stage-sun">☀️</span>
        <span class="stage-grass"></span>
        <Pet3D
          ref="pet3d"
          class="stage-pet"
          :kind="pet.kind"
          :size="168"
          :state="spriteState"
          :cosmetic="pet.cosmetic && pet.cosmetic.icon"
          :cosmetic-name="pet.cosmetic && pet.cosmetic.name"
          hint
        />
        <transition name="bubble">
          <span v-if="pokeText" :key="pokeText" class="stage-bubble">{{ pokeText }}</span>
        </transition>
        <span class="stage-hint">👆 摸摸我 · 拖动能转</span>
      </div>

      <div class="pet-main">
        <p class="pet-name">
          <b>{{ pet.name }}</b>
          <span class="pet-lv">Lv.{{ pet.level }}</span>
          <span class="pet-kind-tag">{{ kindLabel }}</span>
        </p>

        <div class="pet-bars">
          <div class="bar-row">
            <span class="bar-label">🍖 饱食</span>
            <div class="bar"><i class="bar-hunger" :style="{ width: pet.hunger + '%' }"></i></div>
            <em>{{ pet.hunger }}</em>
          </div>
          <div class="bar-row">
            <span class="bar-label">💖 心情</span>
            <div class="bar"><i class="bar-mood" :style="{ width: pet.mood + '%' }"></i></div>
            <em>{{ pet.mood }}</em>
          </div>
          <div class="bar-row">
            <span class="bar-label">⭐ 经验</span>
            <div class="bar"><i class="bar-exp" :style="{ width: expPercent + '%' }"></i></div>
            <em>{{ pet.exp }}/{{ pet.level * 50 }}</em>
          </div>
        </div>

        <div class="pet-actions">
          <button @click="feedPet">🍖 喂食</button>
          <button @click="playPet">🎾 玩耍</button>
          <button @click="walkPet">🐾 散步</button>
          <button @click="renamePet">✏️ 改名</button>
          <button @click="openWardrobe">🎀 衣橱</button>
        </div>
        <p v-if="pet.cosmetic" class="pet-wearing">🎀 今天戴着「{{ pet.cosmetic.name }}」</p>
        <p v-if="pet.hunger < 30" class="pet-warn">肚子咕咕叫了，喂点东西吧～</p>
      </div>
    </div>

    <!-- 还没领养：三只小家伙排队等你 -->
    <div v-else class="pet-tip">
      小窝还空着 🥚<br />
      <span>领养一只会动的小家伙，或去商店兑换宠物蛋</span>
      <div class="pet-preview">
        <PetSprite v-for="k in PET_KINDS" :key="k.kind" :kind="k.kind" :size="46" state="happy" />
      </div>
      <div class="pet-adopt-actions">
        <button class="pet-goto-adopt" @click="openAdopt">🥚 去领养</button>
        <button class="pet-goto-shop" @click="$router.push({ name: 'shop' })">去商店看看</button>
      </div>
    </div>

    <!-- 领养中心：选种类 + 起名 -->
    <Teleport to="body">
      <div v-if="adoptOpen" class="sheet-mask" @click.self="adoptOpen = false">
        <div class="sheet adopt-sheet">
          <h3>🥚 领养中心</h3>
          <p class="sheet-tip">选一只喜欢的小家伙，给它起个名字吧</p>
          <div class="adopt-kinds">
            <button
              v-for="k in PET_KINDS"
              :key="k.kind"
              :class="{ on: adoptKind === k.kind }"
              @click="adoptKind = k.kind"
            >
              <span class="adopt-face">
                <PetSprite :kind="k.kind" :size="56" :state="adoptKind === k.kind ? 'happy' : 'idle'" />
              </span>
              <b>{{ k.name }}</b>
              <em>{{ k.desc }}</em>
            </button>
          </div>
          <input v-model.trim="adoptName" maxlength="20" placeholder="起个名字（特殊符号也行）" @keyup.enter="confirmAdopt" />
          <div class="sheet-btns">
            <button class="primary" :disabled="adopting" @click="confirmAdopt">
              {{ adopting ? '领养中…' : '领养！' }}
            </button>
            <button class="ghost" @click="adoptOpen = false">再想想</button>
          </div>
        </div>
      </div>

      <!-- 衣橱：给宠物换装扮（积分兑换来的） -->
      <div v-if="wardrobeOpen" class="sheet-mask" @click.self="wardrobeOpen = false">
        <div class="sheet wardrobe-sheet">
          <h3>🎀 宠物衣橱</h3>
          <p class="sheet-tip">点一下就换上，再点一次卸下（只能戴一件）</p>
          <div v-if="pet" class="wardrobe-preview">
            <PetSprite :kind="pet.kind" :size="96" state="happy" />
            <span>试衣间 ✨</span>
          </div>
          <div class="wardrobe-grid">
            <button
              v-for="c in cosmetics"
              :key="c.id"
              :class="{ on: c.equipped }"
              @click="toggleCosmetic(c)"
            >
              <span class="wardrobe-face">{{ c.icon }}</span>
              <b>{{ c.name }}</b>
              <em v-if="c.equipped">装备中</em>
              <em v-else-if="c.count > 1">x{{ c.count }}</em>
            </button>
            <div v-if="!cosmetics.length" class="sheet-tip">
              衣橱还空着，去商店「宠物装扮」区兑换几件吧
            </div>
          </div>
          <div class="sheet-btns">
            <button class="ghost" @click="wardrobeOpen = false">搞定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { bump } from '../quest'
import { apiPetMine, apiPetFeed, apiPetPlay, apiPetRename, apiPetAdopt, apiPetCosmetics, apiPetCosmeticEquip, apiPetCosmeticUnequip } from '../pet'
import PetSprite from '../components/PetSprite.vue'
import Pet3D from '../components/Pet3D.vue'
import sound from '../sound'

/** 领养中心的 14 种真实品种（与后端 ShopDefs 一致） */
const PET_KINDS = [
  { kind: 'cat', name: '狸花猫', desc: '中华田园猫，机灵又黏人' },
  { kind: 'dog', name: '萨摩耶', desc: '微笑天使，蓬松一团雪' },
  { kind: 'bird', name: '玄凤鹦鹉', desc: '腮红圆圆，头顶呆毛' },
  { kind: 'rabbit', name: '垂耳兔', desc: '耳朵软软垂下来' },
  { kind: 'fox', name: '赤狐', desc: '大尾巴，鬼点子多' },
  { kind: 'bear', name: '棕熊', desc: '圆滚滚，爱抱抱' },
  { kind: 'hamster', name: '金丝熊', desc: '腮帮塞满瓜子' },
  { kind: 'penguin', name: '小企鹅', desc: '走路一摇一摆' },
  { kind: 'mouse', name: '花枝鼠', desc: '大耳细尾，聪明得很' },
  { kind: 'squirrel', name: '红松鼠', desc: '松果永远抱着' },
  { kind: 'ragdoll', name: '布偶猫', desc: '蓝眼睛，软得像云' },
  { kind: 'corgi', name: '柯基犬', desc: '小短腿，大耳朵' },
  { kind: 'shiba', name: '柴犬', desc: '圆脸微笑，傲娇本娇' },
  { kind: 'budgie', name: '虎皮鹦鹉', desc: '蓝黄配色，叽叽喳喳' }
]

/** 摸一摸时冒出来的小台词 */
const POKE_WORDS = {
  cat: ['喵呜~', '咕噜咕噜…', '尾巴卷成小问号', '蹭蹭你 ✨', '再摸一下嘛'],
  dog: ['汪汪！', '尾巴摇成小风扇啦', '要一起散步吗 🐾', '扑叽~', '最喜欢你了！'],
  bird: ['啾啾~', '扑棱扑棱！', '给你唱首歌 ♪', '歪头看看你', '飞高高~'],
  rabbit: ['耳朵竖起来啦', '蹦跶一下~', '别吓我嘛…', '胡萝卜在哪 🥕'],
  fox: ['尾巴扫到你啦', '嘿嘿，又有好点子', '猜猜我在想什么', '蓬蓬尾~'],
  bear: ['抱抱 🤗', '嗯…慢半拍', '蜂蜜味的风~', '团成一颗球'],
  hamster: ['腮帮还塞着呢', '瓜子分你一半', '咕叽咕叽', '鼓鼓的~'],
  penguin: ['摇摇摆摆~', '一点都不冷！', '滑一下 ⛸️', '啾——'],
  mouse: ['吱吱！', '溜得最快就是我', '耳朵听到啦', '尾巴卷卷'],
  squirrel: ['松果抱紧了 🌰', '大尾巴扫扫', '藏起来藏起来', '蹦到树枝上~'],
  ragdoll: ['软乎乎~', '蓝眼睛看看你', '抱一下嘛', '呼噜呼噜…'],
  corgi: ['汪！', '小短腿跑起来啦', '屁股扭扭~', '耳朵竖高高'],
  shiba: ['汪呜~', '才、才没有想你', '尾巴卷成圈', '摸一下下就好'],
  budgie: ['啾啾啾！', '歪头看你', '羽毛蓬起来啦', '学你说话~']
}

export default {
  name: 'PetView',
  components: { PetSprite, Pet3D },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      pet: null,
      petLoading: true,

      // 动作反馈：临时把 Q 版形象切到 eat / play / happy
      actionState: '',
      actionTimer: null,
      pokeText: '',
      pokeTimer: null,

      // 领养中心
      adoptOpen: false,
      adoptKind: 'cat',
      adoptName: '',
      adopting: false,

      // 衣橱
      wardrobeOpen: false,
      cosmetics: [],

      PET_KINDS
    }
  },
  computed: {
    /** 形象状态：动作反馈优先，其次看饱食度与心情 */
    spriteState() {
      if (this.actionState) return this.actionState
      if (!this.pet) return 'idle'
      if (this.pet.hunger < 30) return 'hungry'
      if (this.pet.mood >= 80) return 'happy'
      return 'idle'
    },
    kindLabel() {
      return this.pet && this.pet.kindInfo ? this.pet.kindInfo.name : '小家伙'
    },
    expPercent() {
      if (!this.pet) return 0
      const max = this.pet.level * 50 || 50
      return Math.min(100, Math.round(this.pet.exp / max * 100))
    }
  },
  created() {
    this.loadPet()
  },
  beforeUnmount() {
    clearTimeout(this.actionTimer)
    clearTimeout(this.pokeTimer)
  },
  methods: {
    async loadPet() {
      this.petLoading = true
      const r = await apiPetMine(this.session.id)
      this.petLoading = false
      if (r.code === 200) this.pet = r.data
    },
    /** 短暂切换动作状态（默认 1.4s 后自动恢复） */
    flashAction(state, duration = 1400) {
      clearTimeout(this.actionTimer)
      this.actionState = state
      this.actionTimer = setTimeout(() => { this.actionState = '' }, duration)
    },
    /** 摸一下：随机冒一句台词 + 蹦跶一下 */
    pokePet() {
      const k = this.pet && POKE_WORDS[this.pet.kind] ? this.pet.kind : 'cat'
      const words = POKE_WORDS[k]
      this.pokeText = words[Math.floor(Math.random() * words.length)]
      clearTimeout(this.pokeTimer)
      this.pokeTimer = setTimeout(() => { this.pokeText = '' }, 1500)
      this.flashAction('play')
      if (this.$refs.pet3d && this.$refs.pet3d.poke) this.$refs.pet3d.poke()
      sound.pop()
    },
    /** 带出去溜达：纯视觉动画，不消耗任何数据 */
    walkPet() {
      this.flashAction('walk', 2600)
      this.pokeText = '出去溜达一圈~ 🐾'
      clearTimeout(this.pokeTimer)
      this.pokeTimer = setTimeout(() => { this.pokeText = '' }, 1600)
      sound.pop()
    },
    async feedPet() {
      const r = await apiPetFeed(this.session.id)
      if (r.code === 200) this.flashAction('eat')
      this.afterPetAction(r)
    },
    async playPet() {
      const r = await apiPetPlay(this.session.id)
      if (r.code === 200) this.flashAction('play')
      this.afterPetAction(r)
    },
    afterPetAction(r) {
      if (r.code === 200) {
        bump('pet')
        sound.pop()
        this.toast(r.data, 'ok')
        this.loadPet()
      } else {
        this.toast(r.msg || '操作失败', 'err')
      }
    },
    async renamePet() {
      const name = prompt('给宠物起个新名字（特殊符号也行）：', this.pet ? this.pet.name : '')
      if (!name || !name.trim()) return
      const r = await apiPetRename(this.session.id, name.trim())
      if (r.code === 200) {
        this.toast(r.data, 'ok')
        this.loadPet()
      } else {
        this.toast(r.msg || '改名失败', 'err')
      }
    },
    /** 打开领养中心 */
    openAdopt() {
      this.adoptOpen = true
      this.adoptKind = 'cat'
      this.adoptName = ''
    },
    /** 确认领养（手动领养入口） */
    async confirmAdopt() {
      if (this.adopting) return
      this.adopting = true
      const r = await apiPetAdopt(this.session.id, this.adoptKind, this.adoptName)
      this.adopting = false
      if (r.code === 200) {
        this.adoptOpen = false
        sound.magic()
        this.toast(r.data, 'ok')
        this.loadPet()
      } else {
        sound.fail()
        this.toast(r.msg || '领养失败', 'err')
      }
    },
    /** 打开衣橱 */
    async openWardrobe() {
      this.wardrobeOpen = true
      const r = await apiPetCosmetics(this.session.id)
      if (r.code === 200) this.cosmetics = r.data || []
    },
    /** 点装扮：没装备就穿上，已装备就卸下 */
    async toggleCosmetic(c) {
      const r = c.equipped
        ? await apiPetCosmeticUnequip(this.session.id)
        : await apiPetCosmeticEquip(this.session.id, c.id)
      if (r.code === 200) {
        sound.pop()
        this.toast(r.data, 'ok')
        this.openWardrobe()
        this.loadPet()
      } else {
        sound.fail()
        this.toast(r.msg || '操作失败', 'err')
      }
    }
  }
}
</script>

<style scoped>
.pet-page { padding: 20px 24px; }
.pet-head {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 16px; text-align: left;
}
.pet-back {
  width: 34px; height: 34px; flex-shrink: 0;
  border: 1.5px solid var(--border); cursor: pointer;
  border-radius: 50%; background: var(--card); color: var(--text);
  font-size: 15px; font-weight: 800;
  transition: background .15s, transform .15s;
}
.pet-back:hover { background: var(--panel); transform: translateX(-2px); }
.pet-title h3 { margin: 0 0 2px; font-size: 16px; }
.pet-title span { font-size: 11.5px; color: var(--muted); }

.pet-tip {
  font-size: 12.5px; line-height: 1.9; color: var(--muted); text-align: center;
  padding: 20px 14px; border-radius: 12px; border: 1.5px dashed var(--border);
}
.pet-preview { display: flex; flex-wrap: wrap; justify-content: center; gap: 2px; margin: 12px 0 2px; }

/* ============ 宠物卡片 ============ */
.pet-card {
  padding: 12px;
  border-radius: 16px; background: var(--panel);
  border: 1.5px solid var(--border); text-align: left;
}
.pet-stage {
  position: relative; height: 216px; border-radius: 13px; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(180deg, #e9f5ff 0%, #fdf0f8 58%, #f3e7fb 100%);
  cursor: pointer;
  border: 1.5px solid rgba(255, 255, 255, .75);
}
.stage-grass {
  position: absolute; left: -12%; right: -12%; bottom: -42px; height: 92px;
  border-radius: 50% 50% 0 0 / 60% 60% 0 0;
  background: linear-gradient(180deg, #d9f2dc, #bfe6cb);
}
.stage-grass::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(255,255,255,.5) 0 5px, transparent 6px),
              radial-gradient(circle at 72% 35%, rgba(255,255,255,.45) 0 4px, transparent 5px),
              radial-gradient(circle at 46% 62%, rgba(255,255,255,.4) 0 3px, transparent 4px);
}
.stage-cloud {
  position: absolute; height: 16px; border-radius: 999px;
  background: rgba(255, 255, 255, .78);
  filter: blur(.4px);
  animation: cloudDrift 14s linear infinite;
}
.cloud-a { width: 52px; top: 24px; left: -60px; }
.cloud-b { width: 38px; top: 52px; left: -60px; animation-duration: 19s; animation-delay: -6s; }
.cloud-c { width: 64px; top: 14px; left: -70px; animation-duration: 23s; animation-delay: -12s; }
@keyframes cloudDrift { from { transform: translateX(0); } to { transform: translateX(420px); } }
.stage-sun { position: absolute; top: 12px; right: 16px; font-size: 20px; opacity: .8; animation: sunSpin 12s linear infinite; }
@keyframes sunSpin { to { transform: rotate(360deg); } }

.stage-pet {
  position: relative; z-index: 2; margin-top: 10px;
  filter: drop-shadow(0 9px 15px rgba(123, 88, 160, .24));
}
.stage-hint {
  position: absolute; z-index: 3; bottom: 8px; right: 10px;
  font-size: 10.5px; color: #7b6b92;
  background: rgba(255, 255, 255, .72); border-radius: 999px; padding: 3px 9px;
}
.stage-bubble {
  position: absolute; z-index: 4; top: 12px; left: 50%;
  transform: translateX(-50%);
  background: #fff; color: #7b5a9c; font-size: 12px; font-weight: 700;
  border-radius: 999px; padding: 5px 12px; white-space: nowrap;
  box-shadow: 0 4px 12px rgba(160, 108, 213, .22);
}
.stage-bubble::after {
  content: ''; position: absolute; bottom: -5px; left: 50%; transform: translateX(-50%);
  border-left: 5px solid transparent; border-right: 5px solid transparent; border-top: 6px solid #fff;
}
.bubble-enter-active, .bubble-leave-active { transition: opacity .2s, transform .2s; }
.bubble-enter-from, .bubble-leave-to { opacity: 0; transform: translate(-50%, 6px) scale(.9); }

.pet-main { padding: 12px 4px 2px; }
.pet-name { margin: 0 0 10px; font-size: 15px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.pet-lv {
  font-size: 10.5px; color: #fff;
  background: var(--primary); border-radius: 999px; padding: 1px 8px;
}
.pet-kind-tag {
  font-size: 10.5px; color: var(--primary); font-weight: 700;
  background: var(--card); border: 1px solid var(--border);
  border-radius: 999px; padding: 1px 8px;
}
.pet-bars { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.bar-row { display: flex; align-items: center; gap: 8px; font-size: 11px; color: var(--muted); }
.bar-label { width: 52px; flex-shrink: 0; }
.bar { flex: 1; height: 7px; border-radius: 999px; background: var(--card); border: 1px solid var(--border); overflow: hidden; }
.bar i { display: block; height: 100%; border-radius: 999px; transition: width .4s ease; }
.bar-hunger { background: linear-gradient(90deg, #ffd28a, #ff9e7d); }
.bar-mood { background: linear-gradient(90deg, #ffb4d2, #f078ab); }
.bar-exp { background: linear-gradient(90deg, #c3a6f5, #8f74e0); }
.bar-row em { font-style: normal; font-size: 10px; width: 46px; text-align: right; flex-shrink: 0; }
.pet-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pet-actions button {
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--card); color: var(--text);
  border-radius: 999px; padding: 6px 13px; font-size: 12px; font-weight: 700;
  transition: all .15s;
}
.pet-actions button:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-1px); }
.pet-wearing, .pet-warn { margin: 9px 0 0; font-size: 11px; color: var(--muted); }
.pet-warn { color: #d97a68; font-weight: 700; }

/* 领养 / 商店入口按钮 */
.pet-adopt-actions { display: flex; gap: 8px; justify-content: center; margin-top: 10px; }
.pet-goto-adopt, .pet-goto-shop {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 18px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}

/* 领养中心 */
.adopt-kinds { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-bottom: 12px; }
.adopt-kinds button {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  border: 1.5px solid var(--border); border-radius: 12px;
  background: var(--panel); color: var(--text);
  padding: 8px 3px 10px; cursor: pointer; transition: all .15s;
}
.adopt-kinds button:hover { border-color: var(--primary); transform: translateY(-2px); }
.adopt-kinds button.on {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.adopt-face { height: 58px; display: flex; align-items: center; justify-content: center; }
.adopt-kinds b { font-size: 11.5px; }
.adopt-kinds em {
  font-style: normal; font-size: 9.5px; color: var(--muted);
  text-align: center; line-height: 1.35;
}
.adopt-kinds button.on em { color: rgba(255, 255, 255, .85); }

/* 衣橱 */
.wardrobe-preview {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  background: var(--panel); border-radius: 12px; margin-bottom: 12px; padding: 4px;
  font-size: 11px; color: var(--muted);
}
.wardrobe-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.wardrobe-grid button {
  display: flex; flex-direction: row; align-items: center; gap: 7px;
  border: 1.5px solid var(--border); border-radius: 14px;
  background: var(--panel); color: var(--text);
  padding: 9px 10px; cursor: pointer; transition: all .15s;
  text-align: left;
}
.wardrobe-grid button:hover { border-color: var(--primary); transform: translateY(-2px); }
.wardrobe-grid button.on {
  border-color: var(--primary);
  outline: 2px solid var(--primary);
  outline-offset: -1px;
}
.wardrobe-face { font-size: 24px; flex-shrink: 0; }
.wardrobe-grid b { font-size: 12px; }
.wardrobe-grid em { font-style: normal; font-size: 10px; color: var(--primary); font-weight: 700; white-space: nowrap; }
.wardrobe-grid button.on b { color: var(--primary); }

/* 通用弹层（领养/衣橱复用） */
.sheet-mask {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(60, 30, 90, .35);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.sheet {
  width: 100%; max-width: var(--app-w); max-height: var(--app-h); overflow-y: auto;
  background: var(--card); border-radius: 20px;
  padding: 20px; border: 1.5px solid var(--border);
}
.sheet h3 { margin: 0 0 4px; font-size: 17px; }
.sheet-tip { margin: 0 0 12px; font-size: 12px; color: var(--muted); }
.sheet input {
  width: 100%; box-sizing: border-box;
  border: 1.5px solid var(--border); border-radius: 10px;
  padding: 9px 12px; font-size: 13px;
  background: var(--panel); color: var(--text); outline: none;
}
.sheet input:focus { border-color: var(--primary); }
.sheet-btns { display: flex; gap: 8px; margin-top: 14px; }
.sheet-btns button {
  flex: 1; border: none; cursor: pointer; border-radius: 10px;
  padding: 9px 0; font-size: 13px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.sheet-btns button.ghost { background: var(--panel); color: var(--muted); }
.sheet-btns button:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; }
</style>
