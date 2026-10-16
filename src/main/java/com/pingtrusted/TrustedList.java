package com.pingtrusted;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** The list of trusted player names, saved to config/pingtrusted.json. */
final class TrustedList {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pingtrusted.json");

	private static final class Data {
		List<String> trusted = new ArrayList<>();
	}

	private static Data data = new Data();

	private TrustedList() {
	}

	static synchronized void load() {
		if (!Files.exists(FILE)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(FILE)) {
			Data loaded = GSON.fromJson(reader, Data.class);

			if (loaded != null && loaded.trusted != null) {
				data = loaded;
			}
		} catch (Exception e) {
			System.err.println("[pingtrusted] Could not read " + FILE + ": " + e);
		}
	}

	private static void save() {
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			GSON.toJson(data, writer);
		} catch (IOException e) {
			System.err.println("[pingtrusted] Could not write " + FILE + ": " + e);
		}
	}

	static synchronized boolean add(String name) {
		if (contains(name)) return false;
		data.trusted.add(name);
		save();
		return true;
	}

	static synchronized boolean remove(String name) {
		boolean removed = data.trusted.removeIf(n -> n.equalsIgnoreCase(name));
		if (removed) save();
		return removed;
	}

	static synchronized boolean contains(String name) {
		return data.trusted.stream().anyMatch(n -> n.equalsIgnoreCase(name));
	}

	static synchronized List<String> names() {
		return new ArrayList<>(data.trusted);
	}
}
