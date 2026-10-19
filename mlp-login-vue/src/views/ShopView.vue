<template>
  <div class="shop">
    <!-- 头部：返回 + 标题 + 积分余额 -->
    <div class="shop-head">
      <button class="shop-back" title="返回我的" @click="$router.push({ name: 'dashboard' })">«</button>
      <div class="shop-title">
        <h3>🧧 积分商店</h3>
        <span>做任务攒积分，好东西换回家</span>
      </div>
      <div class="shop-balance">💎 <b>{{ points }}</b></div>
    </div>

    <!-- Tab：商店 / 背包 -->
    <div class="shop-tabs">
      <button :class="{ on: tab === 'shop' }" @click="tab = 'shop'">🛍 商店</button>
      <button :class="{ on: tab === 'bag' }" @click="switchBag">
        🎒 背包<span v-if="bagCount" class="bag-badge">{{ bagCount }}</span>
      </button>
    </div>

    <!-- 商品网格（按类型分组） -->
    <template v-if="tab === 'shop'">
      <div v-for="g in itemGroups" :key="g.kind" class="shop-group">
        <div class="shop-group-title">{{ g.icon }} {{ g.label }}</div>
        <div class="shop-grid">
          <div v-for="it in g.items" :key="it.id" class="shop-item">
            <div class="shop-item-face">{{ it.icon }}</div>
            <b>{{ it.name }}</b>
            <p>{{ it.desc }}</p>
            <div class="shop-item-foot">
              <span class="price">💎 {{ it.price }}</span>
              <button :disabled="points < it.price || buying === it.id" @click="buy(it)">
                {{ buying === it.id ? '兑换中…' : '兑换' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!items.length" class="empty">商品加载中…</div>
    </template>

    <!-- 背包列表 -->
    <div v-else class="bag-list">
      <div v-for="it in bag" :key="it.id" class="bag-row">
        <span class="bag-face">{{ it.icon }}</span>
        <div class="bag-main">
          <b>{{ it.name }} <em v-if="it.count > 1">x{{ it.count }}</em></b>
          <span>{{ it.desc }}</span>
        </div>
        <button :disabled="using === it.id" @click="useItem(it)">
          {{ using === it.id ? '使用中…' : '使用' }}
        </button>
      </div>
      <div v-if="!bag.length && !bagLoading" class="empty">
        背包空空如也 🕸<br />
        <span>去商店兑换点东西吧</span>
      </div>
      <div v-if="bagLoading" class="empty">翻背包中…</div>
    </div>

    <!-- 弹层：给宠物起名 -->
    <Teleport to="body">
      <div v-if="petDraft" class="sheet-mask" @click.self="petDraft = null">
        <div class="sheet">
          <h3>🥚 孵化宠物</h3>
          <p class="sheet-tip">给小家伙起个名字吧（特殊符号也行）</p>
          <input v-model.trim="petDraftName" maxlength="20" placeholder="宠物名字" @keyup.enter="confirmPet" />
          <div class="sheet-btns">
            <button class="primary" @click="confirmPet">孵化！</button>
            <button class="ghost" @click="petDraft = null">再想想</button>
          </div>
        </div>
      </div>

      <!-- 弹层：积分红包选好友 -->
      <div v-if="redpacketDraft" class="sheet-mask" @click.self="redpacketDraft = null">
        <div class="sheet">
          <h3>🧧 积分红包</h3>
          <p class="sheet-tip">选一位好友，送 TA 50 积分</p>
          <div class="friend-pick">
            <button
              v-for="f in friends"
              :key="f.id"
              :class="{ on: pickedFriend === f.id }"
              @click="pickedFriend = f.id"
            >{{ f.name }}</button>
            <div v-if="!friends.length" class="sheet-tip">还没有好友，去加一个再来送吧</div>
          </div>
          <div class="sheet-btns">
            <button class="primary" :disabled="!pickedFriend" @click="confirmRedpacket">送出 ❤</button>
            <button class="ghost" @click="redpacketDraft = null">取消</button>
          </div>
        </div>
      </div>

      <!-- 弹层：改名卡改名 -->
      <div v-if="renameDraft" class="sheet-mask" @click.self="renameDraft = null">
        <div class="sheet">
          <h3>✏️ 改名卡</h3>
          <p class="sheet-tip">输入新名字，保存后自动消耗一张改名卡</p>
          <input v-model.trim="renameValue" maxlength="20" placeholder="新名字" @keyup.enter="confirmRenameCard" />
          <div class="sheet-btns">
            <button class="primary" @click="confirmRenameCard">保存</button>
            <button class="ghost" @click="renameDraft = null">取消</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiShopItems, apiShopBuy, apiShopBag, apiShopUse } from '../shop'
import { apiAccountProfile, apiAccountUpdate } from '../account'
import { apiFriendList } from '../friend'
import sound from '../sound'

export default {
  name: 'ShopView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      points: 0,
      tab: 'shop',
      items: [],
      bag: [],
      bagLoading: false,
      buying: '',
      using: '',
      // 宠物孵化
      petDraft: null,
      petDraftName: '',
      // 积分红包
      redpacketDraft: false,
      friends: [],
      pickedFriend: null,
      // 改名卡
      renameDraft: false,
      renameValue: ''
    }
  },
  computed: {
    bagCount() {
      return this.bag.reduce((s, it) => s + (it.count || 0), 0)
    },
    /** 商品按类型分组：宠物蛋 / 宠物食物 / 宠物装扮 / 道具 */
    itemGroups() {
      const meta = {
        pet: { icon: '🥚', label: '宠物蛋' },
        food: { icon: '🍰', label: '宠物食物' },
        cosmetic: { icon: '🎀', label: '宠物装扮' },
        other: { icon: '🎁', label: '道具' }
      }
      const defs = [
        { kind: 'pet', filter: (i) => i.kind === 'pet' },
        { kind: 'food', filter: (i) => i.kind === 'food' },
        { kind: 'cosmetic', filter: (i) => i.kind === 'cosmetic' },
        { kind: 'other', filter: (i) => ['pet', 'food', 'cosmetic'].indexOf(i.kind) < 0 }
      ]
      const groups = []
      for (const d of defs) {
        const items = this.items.filter(d.filter)
        if (items.length) {
          groups.push(Object.assign({ kind: d.kind, items }, meta[d.kind]))
        }
      }
      return groups
    }
  },
  created() {
    this.loadPoints()
    this.loadItems()
    this.loadBag()
  },
  methods: {
    async loadPoints() {
      const r = await apiAccountProfile(this.session.id)
      if (r.code === 200 && r.data) this.points = r.data.points || 0
    },
    async loadItems() {
      const r = await apiShopItems()
      if (r.code === 200) this.items = r.data || []
    },
    async loadBag() {
      this.bagLoading = true
      const r = await apiShopBag(this.session.id)
      this.bagLoading = false
      if (r.code === 200) this.bag = r.data || []
    },
    async buy(it) {
      this.buying = it.id
      const r = await apiShopBuy(this.session.id, it.id)
      this.buying = ''
      if (r.code === 200) {
        sound.success()
        this.toast(r.data, 'ok')
        this.loadPoints()
        this.loadBag()
      } else {
        sound.fail()
        this.toast(r.msg || '兑换失败', 'err')
      }
    },
    switchBag() {
      this.tab = 'bag'
      this.loadBag()
    },
    /** 背包物品使用入口：按 kind 分流 */
    async useItem(it) {
      if (it.kind === 'pet') {
        this.petDraft = it
        this.petDraftName = ''
        return
      }
      if (it.kind === 'redpacket') {
        this.redpacketDraft = true
        this.pickedFriend = null
        const r = await apiFriendList(this.session.id)
        if (r.code === 200) this.friends = r.data || []
        return
      }
      if (it.kind === 'rename') {
        const p = await apiAccountProfile(this.session.id)
        this.renameValue = (p.code === 200 && p.data && p.data.name) || ''
        this.renameDraft = true
        return
      }
      // food / lucky / title 直接用
      this.using = it.id
      const r = await apiShopUse(this.session.id, it.id)
      this.using = ''
      if (r.code === 200) {
        sound.magic()
        this.toast(r.data, 'ok')
        this.loadPoints()
        this.loadBag()
      } else {
        sound.fail()
        this.toast(r.msg || '使用失败', 'err')
      }
    },
    async confirmPet() {
      const it = this.petDraft
      if (!it) return
      this.using = it.id
      const r = await apiShopUse(this.session.id, it.id, { petName: this.petDraftName })
      this.using = ''
      if (r.code === 200) {
        this.petDraft = null
        sound.magic()
        this.toast(r.data, 'ok')
        this.loadPoints()
        this.loadBag()
      } else {
        sound.fail()
        this.toast(r.msg || '孵化失败', 'err')
      }
    },
    async confirmRedpacket() {
      if (!this.pickedFriend) return
      this.using = 'point-redpacket'
      const r = await apiShopUse(this.session.id, 'point-redpacket', { toId: this.pickedFriend })
      this.using = ''
      if (r.code === 200) {
        this.redpacketDraft = false
        sound.success()
        this.toast(r.data, 'ok')
        this.loadPoints()
        this.loadBag()
      } else {
        sound.fail()
        this.toast(r.msg || '红包发送失败', 'err')
      }
    },
    async confirmRenameCard() {
      const name = this.renameValue
      if (!name) {
        this.toast('名字不能是空白的', 'err')
        return
      }
      const r = await apiShopUse(this.session.id, 'rename-card', { newName: name })
      if (r.code === 200) {
        this.renameDraft = false
        sound.magic()
        this.toast('改名成功，以后就叫「' + name + '」啦', 'ok')
        this.loadPoints()
        this.loadBag()
      } else {
        sound.fail()
        this.toast(r.msg || '改名失败', 'err')
      }
    }
  }
}
</script>

<style scoped>
.shop-head {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 0 10px;
}
.shop-back {
  width: 34px; height: 34px; flex-shrink: 0;
  border: 1.5px solid var(--border); border-radius: 12px;
  background: var(--card); color: var(--text);
  font-size: 16px; cursor: pointer; transition: all .15s;
}
.shop-back:hover { border-color: var(--primary); color: var(--primary); }
.shop-title { flex: 1; min-width: 0; }
.shop-title h3 { margin: 0; font-size: 18px; }
.shop-title span { font-size: 11.5px; color: var(--muted); }
.shop-balance {
  flex-shrink: 0; font-size: 12.5px; color: var(--muted);
  background: var(--panel); border-radius: 999px; padding: 7px 14px;
}
.shop-balance b { color: var(--primary); font-size: 15px; }

.shop-tabs { display: flex; gap: 8px; margin-bottom: 12px; }
.shop-tabs button {
  position: relative;
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--card); color: var(--muted);
  border-radius: 999px; padding: 7px 16px; font-size: 12.5px; font-weight: 700;
  transition: all .15s;
}
.shop-tabs button.on {
  border-color: transparent; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.bag-badge {
  position: absolute; top: -5px; right: -5px;
  min-width: 16px; height: 16px; border-radius: 999px;
  background: var(--danger); color: #fff;
  font-size: 10px; line-height: 16px; padding: 0 4px;
}

/* 商品分组 */
.shop-group { margin-bottom: 16px; }
.shop-group-title {
  font-size: 12.5px; font-weight: 800; color: var(--muted);
  margin-bottom: 8px;
}
.shop-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px;
}
.shop-item {
  display: flex; flex-direction: column; gap: 4px;
  padding: 14px 12px 10px; border-radius: 16px;
  background: var(--card); border: 1.5px solid var(--border);
  transition: all .15s;
}
.shop-item:hover { border-color: var(--primary); transform: translateY(-2px); }
.shop-item-face { font-size: 36px; text-align: center; }
.shop-item b { font-size: 13.5px; }
.shop-item p {
  flex: 1; margin: 0; font-size: 11px; color: var(--muted);
  line-height: 1.5;
}
.shop-item-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 6px;
}
.price { font-size: 12.5px; font-weight: 800; color: var(--primary); }
.shop-item-foot button {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 5px 14px; font-size: 11.5px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.shop-item-foot button:disabled {
  background: var(--track); color: var(--muted); cursor: not-allowed;
}

/* 背包 */
.bag-list { display: flex; flex-direction: column; gap: 8px; }
.bag-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 14px;
  background: var(--panel);
}
.bag-face {
  width: 44px; height: 44px; flex-shrink: 0;
  border-radius: 12px; background: var(--card);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.bag-main { flex: 1; min-width: 0; }
.bag-main b { font-size: 13.5px; }
.bag-main b em { font-style: normal; font-size: 11.5px; color: var(--primary); margin-left: 4px; }
.bag-main span { display: block; font-size: 11px; color: var(--muted); margin-top: 1px; }
.bag-row button {
  border: none; cursor: pointer; flex-shrink: 0;
  border-radius: 999px; padding: 6px 16px;
  font-size: 12px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.bag-row button:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; }

.empty {
  grid-column: 1 / -1;
  text-align: center; color: var(--muted);
  font-size: 12.5px; padding: 36px 0; line-height: 1.8;
}

/* 弹层（Teleport 到 body，和首页详情一个路子） */
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
.friend-pick { display: flex; flex-wrap: wrap; gap: 6px; }
.friend-pick button {
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--panel); color: var(--text);
  border-radius: 999px; padding: 6px 14px; font-size: 12.5px; font-weight: 700;
}
.friend-pick button.on {
  border-color: transparent; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
</style>
