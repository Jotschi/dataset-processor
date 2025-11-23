package de.jotschi.ai.converter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import de.jotschi.ai.processor.chat.llm.Models;
import de.jotschi.ai.processor.chat.llm.anfrage.AnfrageGenerator;
import de.jotschi.ai.processor.chat.llm.anfrage.AnfrageResult;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.metaloom.ai.genai.llm.ollama.OllamaLLMProvider;
import io.vertx.core.json.JsonObject;

public class LegacyEnhanceQAJsonLTest {

	private static LargeLanguageModel MODEL = Models.OLLAMA_PHI3_MINI;
	private static LLMProvider ollama = new OllamaLLMProvider();

	@Test
	public void testEnhance() throws IOException {

		AnfrageGenerator gen = new AnfrageGenerator(ollama, MODEL);

		File jsonl = new File("dataset", "kleiner_astronaut_qa.jsonl");
		File outputFile = new File("dataset", "kleiner_astronaut_qa_enhanced.jsonl");
		List<String> lines = FileUtils.readLines(jsonl, Charset.defaultCharset());
		for (String line : lines) {
			JsonObject json = new JsonObject(line);
//			if (json.getInteger("row") <= 478) {
//				System.out.println("skipping");
//				continue;
//			}

			String source = json.getString("source");
			AnfrageResult result = gen.generateTriggerQuestion(source, null, null);
			if (result != null) {
				json.put("anfrage", result.anfrage());
				// System.out.println(enhanced.encodePrettily());
				FileUtils.writeStringToFile(outputFile, json.encode() + "\n", Charset.defaultCharset(), true);
			}
		}

	}

}
