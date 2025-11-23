package de.jotschi.ai.converter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import io.metaloom.ai.genai.utils.TextUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Dataset2ChatTest {

	@Test
	public void testConvert() throws IOException {
		File chatJsonL = new File("dataset", "kleiner_astronaut_conversations_v2.jsonl");
		if (chatJsonL.exists()) {
			chatJsonL.delete();
		}
		File dataset = new File("dataset", "kleiner_astronaut_qa_v2.jsonl");
		List<String> lines = FileUtils.readLines(dataset, Charset.defaultCharset());
		for (String line : lines) {
			line = line.trim();
			line = TextUtils.trimNonAsciiFromEnd(line);
			if (line.isEmpty()) {
				continue;
			}
			try {
				JsonObject json = new JsonObject(line);
				JsonArray conv = toChat(json);
				if (conv != null) {
					// System.out.println(conv.encodePrettily());
					FileUtils.writeStringToFile(chatJsonL, conv.encode() + "\n", Charset.defaultCharset(), true);
				}
			} catch (Exception e) {
				System.out.println(line);
				e.printStackTrace();
			}
		}
	}

	private JsonArray toChat(JsonObject json) {
		String request = json.getString("request");
		if (request == null) {
			return null;
		}
		String story = json.getString("story");
		if (TextUtils.count('*', story) > 0) {
			return null;
		}
		if(story.contains("\":")) {
			return null;
		}
		String answer = json.getString("answer");
		String question = json.getString("question");

		JsonArray chat = new JsonArray();
		chat.add(message("user", request));
		chat.add(message("assistant", story));
		chat.add(message("user", question));
		chat.add(message("assistant", answer));

		return chat;
	}

	private Object message(String role, String msg) {
		return new JsonObject().put("role", role).put("content", msg);
	}
}
