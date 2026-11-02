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

    <!-- 宠物本体 -->
    <div v-if="petLoading" class="pet-tip">看看宠物窝里有没有小家伙…</div>
    <div v-else-if="pet" class="pet-card">
      <div class="pet-face" :class="{ hungry: pet.hunger < 30 }">
        {{ pet.kindInfo.emoji }}
        <span v-if="pet.cosmetic" class="pet-cosmetic" :title="pet.cosmetic.name">{{ pet.cosmetic.icon }}</span>
      </div>
      <div class="pet-main">
        <p><b>{{ pet.name }}</b> <span class="pet-lv">Lv.{{ pet.level }}</span></p>
        <div class="pet-stats">
          <span>🍖 饱食 {{ pet.hunger }}</span>
          <span>💖 心情 {{ pet.mood }}</span>
          <span>⭐ 经验 {{ pet.exp }}/{{ pet.level * 50 }}</span>
          <span v-if="pet.cosmetic">🎀 {{ pet.cosmetic.name }}</span>
        </div>
        <div class="pet-actions">
          <button @click="feedPet">🍖 喂食</button>
          <button @click="playPet">🎾 玩耍</button>
          <button @click="renamePet">✏️ 改名</button>
          <button @click="openWardrobe">🎀 衣橱</button>
        </div>
      </div>
    </div>
    <div v-else class="pet-tip">
      还没有宠物 🥚<br />
      <span>领养一只小家伙，或去商店兑换宠物蛋</span>
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
              <span class="adopt-face">{{ k.emoji }}</span>
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
import { apiPetMine, apiPetFeed, apiPetPlay, apiPetRename, apiPetAdopt, apiPetCosmetics, apiPetCosmeticEquip, apiPetCosmeticUnequip } from '../pet'
import sound from '../sound'

/** 领养中心的宠物种类（与后端 ShopDefs 一致） */
const PET_KINDS = [
  { kind: 'dragon', name: '小火龙', emoji: '🐲', desc: '喷小火苗的龙崽' },
  { kind: 'cat', name: '奶糖猫', emoji: '🐱', desc: '又甜又软，爱打呼噜' },
  { kind: 'bird', name: '云雀', emoji: '🐦', desc: '嗓门很小，唱歌跑调' }
]

export default {
  name: 'PetView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      pet: null,
      petLoading: true,

      // 领养中心
      adoptOpen: false,
      adoptKind: 'dragon',
      adoptName: '',
      adopting: false,

      // 衣橱
      wardrobeOpen: false,
      cosmetics: [],

      PET_KINDS
    }
  },
  created() {
    this.loadPet()
  },
  methods: {
    async loadPet() {
      this.petLoading = true
      const r = await apiPetMine(this.session.id)
      this.petLoading = false
      if (r.code === 200) this.pet = r.data
    },
    async feedPet() {
      const r = await apiPetFeed(this.session.id)
      this.afterPetAction(r)
    },
    async playPet() {
      const r = await apiPetPlay(this.session.id)
      this.afterPetAction(r)
    },
    afterPetAction(r) {
      if (r.code === 200) {
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
      this.adoptKind = 'dragon'
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

/* 宠物卡片 */
.pet-card {
  display: flex; gap: 12px; padding: 14px;
  border-radius: 14px; background: var(--panel);
  border: 1.5px solid var(--border); text-align: left;
}
.pet-face {
  position: relative;
  width: 68px; height: 68px; flex-shrink: 0;
  border-radius: 16px; background: var(--card);
  display: flex; align-items: center; justify-content: center;
  font-size: 38px;
  animation: petBounce 2.6s ease-in-out infinite;
}
.pet-face.hungry { filter: grayscale(.6); }
@keyframes petBounce {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-6px) rotate(2deg); }
}
.pet-main { flex: 1; min-width: 0; }
.pet-main p { margin: 0 0 6px; font-size: 15px; }
.pet-lv {
  font-size: 10.5px; color: #fff; margin-left: 6px;
  background: var(--primary); border-radius: 999px; padding: 1px 8px;
}
.pet-stats { display: flex; flex-wrap: wrap; gap: 8px; font-size: 11.5px; color: var(--muted); margin-bottom: 9px; }
.pet-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pet-actions button {
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--card); color: var(--text);
  border-radius: 999px; padding: 6px 13px; font-size: 12px; font-weight: 700;
  transition: all .15s;
}
.pet-actions button:hover { border-color: var(--primary); color: var(--primary); }

/* 领养 / 商店入口按钮 */
.pet-adopt-actions { display: flex; gap: 8px; justify-content: center; margin-top: 10px; }
.pet-goto-adopt, .pet-goto-shop {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 18px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}

/* 宠物头顶装扮角标 */
.pet-cosmetic {
  position: absolute; top: -10px; right: -8px;
  font-size: 20px;
  filter: drop-shadow(0 2px 3px rgba(160, 108, 213, .4));
  animation: petBounce 2s ease-in-out infinite;
}

/* 领养中心 */
.adopt-kinds { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.adopt-kinds button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 14px;
  background: var(--panel); color: var(--text);
  padding: 12px 6px; cursor: pointer; transition: all .15s;
}
.adopt-kinds button:hover { border-color: var(--primary); }
.adopt-kinds button.on {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.adopt-face { font-size: 32px; }
.adopt-kinds b { font-size: 12.5px; }
.adopt-kinds em {
  font-style: normal; font-size: 10px; color: var(--muted);
  text-align: center; line-height: 1.4;
}
.adopt-kinds button.on em { color: rgba(255, 255, 255, .85); }

/* 衣橱 */
.wardrobe-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.wardrobe-grid button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 14px;
  background: var(--panel); color: var(--text);
  padding: 10px 4px; cursor: pointer; transition: all .15s;
}
.wardrobe-grid button:hover { border-color: var(--primary); transform: translateY(-2px); }
.wardrobe-grid button.on {
  border-color: var(--primary);
  outline: 2px solid var(--primary);
  outline-offset: -1px;
}
.wardrobe-face { font-size: 26px; }
.wardrobe-grid b { font-size: 11px; }
.wardrobe-grid em { font-style: normal; font-size: 10px; color: var(--primary); font-weight: 700; }
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
