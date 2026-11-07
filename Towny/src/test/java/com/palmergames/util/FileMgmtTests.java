package com.palmergames.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileMgmtTests {

	@TempDir
	Path folder;

	@Test
	void mapToFileReplacesContentAndLeavesNoTempFile() throws IOException {
		Path target = folder.resolve("town.txt");
		Files.writeString(target, "name=old\n");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", "Vanilya");
		data.put("mayor", "NPC1");
		FileMgmt.mapToFile(data, target);

		assertEquals(List.of("name=Vanilya", "mayor=NPC1"), Files.readAllLines(target));
		assertFalse(Files.exists(folder.resolve("town.txt.tmp")));
	}

	@Test
	void failedWriteKeepsTheOldFile() throws IOException {
		Path target = folder.resolve("nation.txt");
		Files.writeString(target, "name=kept\n");

		assertThrows(IOException.class, () -> FileMgmt.writeAtomically(target, temp -> {
			Files.writeString(temp, "name=half");
			throw new IOException("simulated crash while writing");
		}));

		assertEquals("name=kept\n", Files.readString(target));
		assertFalse(Files.exists(folder.resolve("nation.txt.tmp")));
	}

	@Test
	void listToFileWritesEveryLine() throws IOException {
		Path target = folder.resolve("regen.txt");
		FileMgmt.listToFile(List.of("a", "b"), target.toString());
		assertEquals(List.of("a", "b"), Files.readAllLines(target));
	}
}
