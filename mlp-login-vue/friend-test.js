/**
 * 好友 + 聊天 端到端测试（两个账号：1=哇哇哇, 2=喂喂喂）
 * 流程：搜索 → 申请 → 查看申请 → 同意 → 好友列表 → 发消息 → 未读 → 历史 → 已读 → 会话列表 → 删好友
 */
const fs = require('fs');
const BASE = 'http://localhost:8081';
const out = [];

async function api(path, opts = {}) {
  const res = await fetch(BASE + path, {
    method: opts.method || 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: opts.body ? JSON.stringify(opts.body) : undefined
  });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch (e) { json = { code: -1, msg: 'NON-JSON: ' + text.slice(0, 200) }; }
  return json;
}

(async () => {
  try {
    // 0. 清理：删掉旧的好友关系和聊天记录，保证测试从零开始
    await api('/api/friend/delete', { body: { meId: 1, friendId: 2 } });
    await api('/api/friend/delete', { body: { meId: 2, friendId: 1 } });
    out.push('[0] 清理旧数据 ok');

    // 1. 哇哇哇 搜索 喂喂喂
    let r = await api('/api/friend/search', { body: { meId: 1, keyword: '喂喂喂' } });
    out.push('[1] 搜索喂喂喂 code=' + r.code + ' data=' + JSON.stringify(r.data));

    // 2. 哇哇哇 给 喂喂喂 发申请
    r = await api('/api/friend/apply', { body: { fromId: 1, toId: 2, msg: '一起玩呀！' } });
    out.push('[2] 发申请 code=' + r.code + ' msg=' + r.msg);

    // 3. 重复申请应该被拦住
    r = await api('/api/friend/apply', { body: { fromId: 1, toId: 2, msg: '再试一次' } });
    out.push('[3] 重复申请 code=' + r.code + ' msg=' + r.msg);

    // 4. 喂喂喂 查看收到的申请
    r = await api('/api/friend/applies', { body: { meId: 2 } });
    out.push('[4] 收到的申请 code=' + r.code + ' data=' + JSON.stringify(r.data));
    const applyId = r.data && r.data[0] && r.data[0].id;

    // 5. 同意申请
    r = await api('/api/friend/handle', { body: { applyId, meId: 2, accept: true } });
    out.push('[5] 同意申请 code=' + r.code + ' msg=' + r.msg);

    // 6. 双方好友列表
    r = await api('/api/friend/list', { body: { meId: 1 } });
    out.push('[6] 哇哇哇的好友 code=' + r.code + ' data=' + JSON.stringify(r.data));
    r = await api('/api/friend/list', { body: { meId: 2 } });
    out.push('[6b] 喂喂喂的好友 code=' + r.code + ' data=' + JSON.stringify(r.data));

    // 7. 哇哇哇 发消息给 喂喂喂
    r = await api('/api/chat/send', { body: { fromId: 1, toId: 2, content: '你好呀，这是我的第一条聊天消息！' } });
    out.push('[7] 发消息 code=' + r.code + ' data=' + JSON.stringify(r.data));

    // 8. 喂喂喂 的未读数应为 1
    r = await api('/api/chat/unread?meId=2', { method: 'GET' });
    out.push('[8] 喂喂喂未读 code=' + r.code + ' data=' + r.data);

    // 9. 喂喂喂 回复一条
    r = await api('/api/chat/send', { body: { fromId: 2, toId: 1, content: '收到收到， Friendship is Magic! ✨' } });
    out.push('[9] 回复 code=' + r.code + ' msg=' + r.msg);

    // 10. 聊天历史
    r = await api('/api/chat/history?meId=1&friendId=2', { method: 'GET' });
    out.push('[10] 历史(哇哇哇视角) code=' + r.code + ' 条数=' + (r.data || []).length + ' data=' + JSON.stringify(r.data));

    // 11. 会话列表
    r = await api('/api/chat/conversations?meId=1', { method: 'GET' });
    out.push('[11] 哇哇哇会话 code=' + r.code + ' data=' + JSON.stringify(r.data));

    // 11b. 标记已读
    r = await api('/api/chat/read', { body: { meId: 1, friendId: 2 } });
    r = await api('/api/chat/unread?meId=1', { method: 'GET' });
    out.push('[11b] 哇哇哇标记已读后未读 code=' + r.code + ' data=' + r.data);

    // 12. 非好友发消息应该被拒（1 和 1 自己不行；换个思路：先删好友再发）
    await api('/api/friend/delete', { body: { meId: 1, friendId: 2 } });
    r = await api('/api/chat/send', { body: { fromId: 1, toId: 2, content: '不是好友还发消息' } });
    out.push('[12] 删好友后发消息 code=' + r.code + ' msg=' + r.msg);

    // 13. 删好友后再加回来，验证历史消息还在
    r = await api('/api/friend/apply', { body: { fromId: 1, toId: 2, msg: '和好啦' } });
    r = await api('/api/friend/applies', { body: { meId: 2 } });
    if (r.data && r.data[0]) {
      await api('/api/friend/handle', { body: { applyId: r.data[0].id, meId: 2, accept: true } });
    }
    r = await api('/api/chat/history?meId=1&friendId=2', { method: 'GET' });
    out.push('[13] 和好后历史条数 code=' + r.code + ' 条数=' + (r.data || []).length);

    fs.writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/friend-test.txt', out.join('\n'), 'utf8');
    console.log('ALL DONE');
  } catch (e) {
    out.push('FATAL: ' + e.message);
    fs.writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/friend-test.txt', out.join('\n'), 'utf8');
    process.exit(1);
  }
})();
