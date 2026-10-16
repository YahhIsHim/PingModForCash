package com.pingtrusted;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class Pinger {
	/**
	 * Effectively unlimited: the ray keeps going until it hits a block. The client can only know about blocks in
	 * chunks it has loaded, so in practice the limit is your render distance.
	 */
	private static final double RANGE = 100_000.0;
	private static final long COOLDOWN_MS = 1000;
	private static final long ECHO_WINDOW_MS = 3000;

	private static final String END = " to everyone trusted";

	// "<user> has pinged <block> at <x>, <y>, <z> in <dimension> to everyone trusted"
	private static final Pattern PING = Pattern.compile("(\\S+) has pinged (.+?) at (-?\\d+), (-?\\d+), (-?\\d+) in (.+?)" + END);

	private static long lastPingTime = 0;
	private static String lastPingText = null;

	private Pinger() {
	}

	static void ping(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientPacketListener connection = mc.getConnection();

		if (player == null || mc.level == null || connection == null) return;

		long now = System.currentTimeMillis();
		if (now - lastPingTime < COOLDOWN_MS) return;

		HitResult hit = player.pick(RANGE, 1.0F, false);

		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendSystemMessage(Component.literal("No block in sight to ping.").withStyle(ChatFormatting.RED));
			return;
		}

		List<String> trusted = TrustedList.names();

		if (trusted.isEmpty()) {
			player.sendSystemMessage(Component.literal("Your trusted list is empty. Use /pingtrusted add <player>").withStyle(ChatFormatting.RED));
			return;
		}

		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		String blockName = mc.level.getBlockState(pos).getBlock().getName().getString();
		String dimension = PingMarkers.dimensionName(mc.level);
		String user = player.getGameProfile().name();

		String text = user + " has pinged " + blockName + " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
				+ " in " + dimension + END;

		List<String> sentTo = new ArrayList<>();

		for (String name : trusted) {
			// Skip ourselves and anyone who isn't online right now.
			if (name.equalsIgnoreCase(user)) continue;
			if (connection.getPlayerInfo(name) == null) continue;

			connection.sendCommand("msg " + name + " " + text);
			sentTo.add(name);
		}

		lastPingTime = now;
		lastPingText = text;

		PingMarkers.add(user, blockName, pos.getX(), pos.getY(), pos.getZ(), dimension);

		player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal(sentTo.isEmpty()
				? "(nobody on your trusted list is online)"
				: "(sent to " + String.join(", ", sentTo) + ")").withStyle(ChatFormatting.GRAY));

		playPingSound(mc);
	}

	/** True for the server's "You whisper to X: <our ping text>" echo lines. */
	static boolean isOwnEcho(String message) {
		return lastPingText != null
				&& System.currentTimeMillis() - lastPingTime < ECHO_WINDOW_MS
				&& message.contains(lastPingText);
	}

	/**
	 * Called for every incoming chat/system message. The message itself always shows in chat (it includes the
	 * dimension). The on-screen marker and sound only happen if the ping is in the dimension we're currently in.
	 */
	static void onIncoming(String message) {
		Matcher m = PING.matcher(message);
		if (!m.find()) return;

		String user = m.group(1);
		String block = m.group(2);
		int x;
		int y;
		int z;

		try {
			x = Integer.parseInt(m.group(3));
			y = Integer.parseInt(m.group(4));
			z = Integer.parseInt(m.group(5));
		} catch (NumberFormatException e) {
			return;
		}

		String dimension = m.group(6);
		Minecraft mc = Minecraft.getInstance();

		mc.execute(() -> {
			if (mc.level == null) return;
			if (!dimension.equals(PingMarkers.dimensionName(mc.level))) return; // different dimension: chat only

			PingMarkers.add(user, block, x, y, z, dimension);
			playPingSound(mc);
		});
	}

	private static void playPingSound(Minecraft mc) {
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5F, 1.0F));
	}
}
