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

	public static final File INPUT_DATASET = new File("dataset", "kleiner_astronaut_qa_v3.jsonl");
	public static final File OUTPUT_TRAIN = new File("dataset", "kleiner_astronaut_conversations_v3_train.jsonl");
	public static final File OUTPUT_VAL = new File("dataset", "kleiner_astronaut_conversations_v3_val.jsonl");

	@Test
	public void testConvert() throws IOException {
		if (OUTPUT_TRAIN.exists()) {
			OUTPUT_TRAIN.delete();
		}

		if (OUTPUT_VAL.exists()) {
			OUTPUT_VAL.delete();
		}

		List<String> lines = FileUtils.readLines(INPUT_DATASET, Charset.defaultCharset());
		int total = lines.size();
		int val_size = (int) (total * 0.05f);

		for (String line : lines) {
			File destFile = OUTPUT_TRAIN;
			if (val_size >= 0) {
				destFile = OUTPUT_VAL;
				val_size--;
			}
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
					FileUtils.writeStringToFile(destFile, conv.encode() + "\n", Charset.defaultCharset(), true);
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
		String request_word_1 = json.getString("request_word_1");
		String request_word_2 = json.getString("request_word_2");
		String answer_word = json.getString("answer_word");

		String story = json.getString("story");
		if (TextUtils.count('*', story) > 0) {
			return null;
		}
		if (story.contains("\":")) {
			return null;
		}
		String answer = json.getString("answer");
		String question = json.getString("question");

		JsonArray chat = new JsonArray();
		chat.add(message("user", request));
		chat.add(message("assistant", story).put("rl_key1", request_word_1).put("rl_key2", request_word_2));
		chat.add(message("user", question));
		chat.add(message("assistant", answer).put("rl_key1", answer_word));

		return chat;
	}

	private JsonObject message(String role, String msg) {
		return new JsonObject().put("role", role).put("content", msg);
	}
}
