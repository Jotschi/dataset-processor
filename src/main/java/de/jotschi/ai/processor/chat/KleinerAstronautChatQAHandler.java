package de.jotschi.ai.processor.chat;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import de.jotschi.ai.processor.DatasetEntryHandler;
import io.metaloom.ai.genai.llm.LLMContext;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.metaloom.ai.genai.llm.prompt.Prompt;
import io.metaloom.ai.genai.llm.prompt.impl.PromptImpl;
import io.vertx.core.json.JsonObject;

public class KleinerAstronautChatQAHandler implements DatasetEntryHandler<KleinerAstronautDatasetEntry> {

	private static final int RETRY_MAX = 10;

	private final Random rand = new Random();
	private final LLMProvider ollama;
	private final LargeLanguageModel model;

	private final File outputFile;

	public KleinerAstronautChatQAHandler(File outputFile, LLMProvider ollama, LargeLanguageModel model) {
		this.outputFile = outputFile;
		this.ollama = ollama;
		this.model = model;
	}

	@Override
	public void process(KleinerAstronautDatasetEntry entry) {
		String text = entry.text();
		List<String> wFragen = List.of("Wer", "Was", "Wie", "Wo", "Womit", "Wann", "Mit wem", "Weshalb", "Warum");

		String tmpl = """
				Schreib eine einzelne kurze, einfache kindliche ${typ}-Frage und die kurze Antwort zu dieser Geschichte.
				Umschreibe komplizierte Wörter so das sie von Kindern verstanden werden können.

				Gib die Frage Antwort als JSON aus:

				{
					"frage": "${typ}..."
					"antwort": "..."
				}

				Geschichte:
				${text}
				""";

		for (int i = 0; i < RETRY_MAX; i++) {
			String randomFrageTyp = wFragen.get(rand.nextInt(wFragen.size()));
			Prompt prompt = new PromptImpl(tmpl);
			prompt.set("text", text);
			prompt.set("typ", randomFrageTyp);
			LLMContext ctx = LLMContext.ctx(prompt, model);
			ctx.setTemperature(0.25);
			try {
				JsonObject json = ollama.generateJson(ctx);
				String frage = json.getString("frage");
				String antwort = json.getString("antwort");
				// Retry on invalid case - Quality gate
				if (frage == null || antwort == null || antwort.contains("...") || frage.contains("...")
						|| !frage.startsWith(randomFrageTyp) || frage.contains(" und ")
						|| frage.trim().equalsIgnoreCase(randomFrageTyp)) {
					System.out.println("Retry.. " + i + " " + frage + " / " + randomFrageTyp);
					continue;
				}
				JsonObject jsonOut = new JsonObject();
				jsonOut.put("source", text);
				jsonOut.put("row", entry.rowNum());
				jsonOut.put("typ", randomFrageTyp);
				jsonOut.put("frage", frage);
				jsonOut.put("antwort", antwort);
				FileUtils.writeStringToFile(outputFile, jsonOut.encode() + "\n", Charset.defaultCharset(), true);
				return;
			} catch (Exception e) {
				System.out.println("Retry.. " + i + " " + e.getMessage());
				// NOOP
			}
		}
	}

}
