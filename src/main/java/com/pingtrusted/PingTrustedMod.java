package com.pingtrusted;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/**
 * Ping Trusted - client-side mod for Minecraft 1.21.11.
 *
 * Look at a block, press the keybind, and everyone on your trusted list gets a private message:
 * "{user} has pinged {block} at {x, y, z} in {dimension} to everyone trusted".
 * Players in the same dimension also see an on-screen marker; players in another dimension only get the chat message.
 */
public class PingTrustedMod implements ClientModInitializer {
	public static final String MOD_ID = "pingtrusted";

	private static KeyMapping pingKey;

	@Override
	public void onInitializeClient() {
		TrustedList.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		// Default key: V (rebindable in Controls).
		pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pingtrusted.ping", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "ping_markers"), PingHud::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (pingKey.consumeClick()) {
				Pinger.ping(client);
			}
		});

		// Hide the "You whisper to X: ..." echo lines for our own pings (we show one summary line instead).
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !Pinger.isOwnEcho(message.getString()));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, type, time) -> !Pinger.isOwnEcho(message.getString()));

		// Play a sound when somebody else's ping arrives.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> Pinger.onIncoming(message.getString()));
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, type, time) -> Pinger.onIncoming(message.getString()));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommandManager.literal("pingtrusted")
						.then(ClientCommandManager.literal("add")
								.then(ClientCommandManager.argument("player", StringArgumentType.word())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "player");
											boolean added = TrustedList.add(name);
											ctx.getSource().sendFeedback(Component.literal(added
													? name + " is now trusted."
													: name + " was already trusted.").withStyle(ChatFormatting.GREEN));
											return 1;
										})))
						.then(ClientCommandManager.literal("remove")
								.then(ClientCommandManager.argument("player", StringArgumentType.word())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "player");
											boolean removed = TrustedList.remove(name);
											ctx.getSource().sendFeedback(Component.literal(removed
													? name + " is no longer trusted."
													: name + " was not on the trusted list.").withStyle(removed ? ChatFormatting.YELLOW : ChatFormatting.RED));
											return 1;
										})))
						.then(ClientCommandManager.literal("list")
								.executes(ctx -> {
									var names = TrustedList.names();
									ctx.getSource().sendFeedback(Component.literal(names.isEmpty()
											? "Trusted list is empty. Use /pingtrusted add <player>"
											: "Trusted (" + names.size() + "): " + String.join(", ", names)).withStyle(ChatFormatting.AQUA));
									return 1;
								}))));
	}
}
