package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.MonopolyRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 🎲 糖霜镇大富翁「四人联机」接口（内存房间，无需登录令牌，凭房间号 + 座位 token 操作）。
 *
 *   POST /api/monopoly/create  { nickname, userId, mode }        → { roomId, seatId, token, state }
 *   POST /api/monopoly/join    { roomId, nickname, userId }      → { roomId, seatId, token, state }
 *   POST /api/monopoly/start   { roomId, seatId, token, mode, fillAi }
 *   GET  /api/monopoly/state?roomId=&seatId=&token=               → 房间状态（前端每 ~1.2s 轮询）
 *   POST /api/monopoly/action  { roomId, seatId, token, type, value }
 *   POST /api/monopoly/leave   { roomId, seatId, token }
 *
 * action.type：roll（掷骰）、buy（value=true 买下 / false 跳过）
 */
@RestController
@RequestMapping("/api/monopoly")
public class MonopolyRoomController {

    @Autowired
    private MonopolyRoomService roomService;

    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body) {
        return guard(() -> roomService.create(
                str(body.get("nickname")), toLong(body.get("userId")), str(body.get("mode"))));
    }

    @PostMapping("/join")
    public Result<?> join(@RequestBody Map<String, Object> body) {
        return guard(() -> roomService.join(
                str(body.get("roomId")), str(body.get("nickname")), toLong(body.get("userId"))));
    }

    @PostMapping("/start")
    public Result<?> start(@RequestBody Map<String, Object> body) {
        return guard(() -> roomService.start(
                str(body.get("roomId")), toInt(body.get("seatId"), -1), str(body.get("token")),
                str(body.get("mode")), bool(body.get("fillAi"))));
    }

    /**
     * 主机在局域网里的 IPv4 地址。前端拿它拼「发给同学的邀请网址」，
     * 免得大家还得自己敲 ipconfig 找 IP。
     */
    @GetMapping("/lan")
    public Result<?> lan() {
        List<String> ips = lanIps();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ip", ips.isEmpty() ? "" : ips.get(0));
        m.put("ips", ips);
        m.put("port", 5180);
        return Result.ok(m);
    }

    @GetMapping("/state")
    public Result<?> state(@RequestParam String roomId,
                           @RequestParam int seatId,
                           @RequestParam String token) {
        return guard(() -> roomService.state(roomId, seatId, token));
    }

    @PostMapping("/action")
    public Result<?> action(@RequestBody Map<String, Object> body) {
        return guard(() -> roomService.action(
                str(body.get("roomId")), toInt(body.get("seatId"), -1), str(body.get("token")),
                str(body.get("type")), bool(body.get("value"))));
    }

    @PostMapping("/leave")
    public Result<?> leave(@RequestBody Map<String, Object> body) {
        return guard(() -> {
            roomService.leave(str(body.get("roomId")), toInt(body.get("seatId"), -1), str(body.get("token")));
            return null;
        });
    }

    /** 业务异常转成前端能识别的 { code:400, msg } */
    private Result<?> guard(Supplier<Object> call) {
        try {
            return Result.ok(call.get());
        } catch (MonopolyRoomService.RoomException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("房间服务异常：" + e.getMessage());
        }
    }

    /**
     * 列出本机局域网 IPv4。虚拟网卡（VMware / WSL / Docker / Hyper-V …）排在后面，
     * 真实 WiFi / 网卡优先，这样「发给同学的网址」才不会拼成连不上的虚拟地址。
     */
    private List<String> lanIps() {
        List<String> real = new ArrayList<>();
        List<String> virtual = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                String desc = ((ni.getDisplayName() == null ? "" : ni.getDisplayName()) + " " + ni.getName()).toLowerCase();
                boolean isVirtual = ni.isVirtual() || desc.contains("vmware") || desc.contains("virtual")
                        || desc.contains("wsl") || desc.contains("hyper-v") || desc.contains("vethernet")
                        || desc.contains("docker") || desc.contains("tap") || desc.contains("vpn")
                        || desc.contains("loopback") || desc.contains("virtualbox");
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress() && !a.isLoopbackAddress()) {
                        String ip = a.getHostAddress();
                        List<String> bucket = isVirtual ? virtual : real;
                        if (!bucket.contains(ip)) bucket.add(ip);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // 家用 / 校园局域网里 192.168.x 最常见，往前放
        real.sort((a, b) -> Integer.compare(rank(a), rank(b)));
        List<String> out = new ArrayList<>(real);
        for (String ip : virtual) if (!out.contains(ip)) out.add(ip);
        return out;
    }

    private int rank(String ip) {
        if (ip.startsWith("192.168.")) return 0;
        if (ip.startsWith("10.")) return 1;
        return 2;
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.valueOf(String.valueOf(o).trim()); } catch (Exception e) { return null; }
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return def; }
    }

    private boolean bool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        return "true".equalsIgnoreCase(String.valueOf(o));
    }
}
