<template>
  <transition name="np-pop">
    <div v-if="visible" class="np-wrap" role="alert">
      <!-- 未读消息卡片：点一下直达消息中心 -->
      <div v-if="notify.hasMessage" class="np-card np-msg" @click="goMessage">
        <div class="np-icon">💬</div>
        <div class="np-body">
          <b>你有新的消息</b>
          <span>
            {{ notify.pmUnread > 0 ? `私信 ${notify.pmUnread} 条` : '' }}
            {{ notify.pmUnread > 0 && notify.notifyUnread > 0 ? ' · ' : '' }}
            {{ notify.notifyUnread > 0 ? `通知 ${notify.notifyUnread} 条` : '' }}
          </span>
        </div>
        <button class="np-close" title="稍后查看" @click.stop="dismiss">✕</button>
      </div>

      <!-- 好友申请卡片：点一下直达「新的朋友」 -->
      <div v-if="notify.hasApply" class="np-card np-friend" @click="goFriends">
        <div class="np-icon">👥</div>
        <div class="np-body">
          <b>{{ appliesText }}</b>
          <span>点击前往处理好友申请</span>
        </div>
        <button class="np-close" title="稍后查看" @click.stop="dismiss">✕</button>
      </div>
    </div>
  </transition>
</template>

<script>
import { notify } from '../notify'

export default {
  name: 'NotificationPopup',
  data() {
    return {
      /** 把 notify 挂进来，模板里直接读它的响应式字段 */
      notify,
      /** 已手动关闭的那一版「未读签名」，变了就重新弹 */
      dismissed: ''
    }
  },
  computed: {
    /** 当前未读状态的指纹：有新未读时为非空串，否则空串（不弹） */
    signature() {
      if (notify.hasMessage || notify.hasApply) {
        return `M${notify.messageTotal}|C${notify.chatUnread}|A${notify.friendApplies.length}`
      }
      return ''
    },
    /** 有未读且这版没被手动关掉 → 显示 */
    visible() {
      return this.signature !== '' && this.dismissed !== this.signature
    },
    appliesText() {
      const n = notify.friendApplies.length
      return `收到 ${n} 个好友申请`
    }
  },
  methods: {
    dismiss() {
      this.dismissed = this.signature
    },
    goMessage() {
      this.dismiss()
      this.$router.push({ name: 'message' })
    },
    goFriends() {
      this.dismiss()
      this.$router.push({ name: 'friends', query: { tab: 'apply' } })
    }
  }
}
</script>

<style scoped>
.np-wrap {
  position: fixed;
  top: 60px;
  right: 16px;
  z-index: 60;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 280px;
  pointer-events: none;
}
.np-card {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 12px;
  border-radius: 15px;
  background: var(--card, #fff);
  border: 1.5px solid var(--border, #eadcff);
  box-shadow: 0 10px 26px rgba(120, 70, 160, .22);
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}
.np-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(120, 70, 160, .28);
}
.np-icon {
  width: 38px; height: 38px; flex-shrink: 0;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 19px; color: #fff;
}
.np-msg .np-icon { background: linear-gradient(135deg, #ff9ec6, #b58bff); }
.np-friend .np-icon { background: linear-gradient(135deg, #8fc7f0, #a06cd5); }
.np-body { flex: 1; min-width: 0; }
.np-body b { display: block; font-size: 13.5px; color: var(--text, #3a2b4d); }
.np-body span { display: block; font-size: 11px; color: var(--muted, #8a7ca8); margin-top: 2px; }
.np-close {
  flex-shrink: 0;
  width: 22px; height: 22px;
  border: none; border-radius: 50%;
  background: var(--panel, #f6eefb); color: var(--muted, #8a7ca8);
  cursor: pointer; font-size: 11px;
  transition: all .15s;
}
.np-close:hover { color: var(--danger, #e06666); background: #ffeceb; }

/* 进场动画 */
.np-pop-enter-active { animation: npIn .32s cubic-bezier(.22,1,.36,1); }
.np-pop-leave-active { animation: npIn .2s reverse ease-in; }
@keyframes npIn {
  from { opacity: 0; transform: translateX(24px) scale(.96); }
  to   { opacity: 1; transform: translateX(0) scale(1); }
}

/* 触屏：靠右铺满一点，避免太窄 */
@media (max-width: 480px) {
  .np-wrap { left: 12px; right: 12px; max-width: none; }
}
</style>
