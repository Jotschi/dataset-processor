package de.jotschi.ai.processor.chat.llm.anfrage;

import java.util.List;

import de.jotschi.ai.processor.chat.llm.AbstractGenerator;
import io.metaloom.ai.genai.llm.LLMContext;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.metaloom.ai.genai.llm.prompt.Prompt;
import io.metaloom.ai.genai.llm.prompt.impl.PromptImpl;
import io.metaloom.ai.genai.utils.TextUtils;
import io.vertx.core.json.JsonObject;

public class AnfrageGenerator extends AbstractGenerator {

	private static final int ANFRAGE_OUTPUT_MAX_LEN = 150;

	private static final int STORY_MAX_LEN = 150;

	private static final List<String> ANFRAGE_LIST = List.of("Schreibe mir", "Erfinde eine", "Schreib eine",
			"Schreib mir", "Schreib ein", "Erzähle mir", "Kannst du mir", "Bitte erzähle mir", "Eine Geschichte",
			"Ein Abendteuer", "Es war einmal", "Ich möchte gerne", "Lies mir", "Kannst du");

	final String GENERATE_ANFRAGE_PROMPT_TEMPLATE = """
			Du bist ein 8-jähriges Kind.
			Du möchtest eine kurze Kindergeschichte hören.

			Schreibe eine einzige Anfrage, die zu der folgenden Geschichte passt.

			Wichtig:

			Die Anfrage muss mit '${anfang}' beginnen.

			Maximal 1 Satz.

			Maximal 100 Zeichen.

			Benutze sehr einfache Sprache.

			Schreibe sehr kurz.

			Erlaubte Anfänge für Anfragen:
			"Schreibe mir", "Erfinde eine", "Schreib eine", "Schreib mir",
			"Schreib ein", "Erzähle mir", "Kannst du mir", "Bitte erzähle mir",
			"Eine Geschichte", "Ein Abenteuer", "Es war einmal",
			"Ich möchte gerne", "Lies mir", "Kannst du"

			Vervollständige die Anfang: '${anfang}'
			Gib auch ein Schlüsselwort für die Anfrage aus. Das Schlüsselwort muss in der Geschichte vorkommen.

			Gib JSON aus:
			{
				"anfrage": "${anfang}.."
				"schlüsselwort": "..."
			}

			Geschichte:
			${text}
			""";

	public AnfrageGenerator(LLMProvider llm, LargeLanguageModel model) {
		super(llm, model);
	}

	public AnfrageResult generateTriggerQuestion(String story, String word1) {
		for (int i = 0; i < RETRY_MAX; i++) {
			String randomAnfang = ANFRAGE_LIST.get(rand.nextInt(ANFRAGE_LIST.size()));

			Prompt prompt = new PromptImpl(GENERATE_ANFRAGE_PROMPT_TEMPLATE);
			story = TextUtils.softClamp(story, STORY_MAX_LEN, '?', '.', '!', '\n');
			prompt.set("text", TextUtils.quote(story));
			prompt.set("anfang", randomAnfang);

			LLMContext ctx = LLMContext.ctx(prompt, model);
			ctx.setTemperature(0.25);
			try {
				JsonObject anfrageJson = llm.generateJson(ctx);
				String anfrage = anfrageJson.getString("anfrage");
				String key = anfrageJson.getString("schlüsselwort");

				// Retry on invalid JSON
				if (anfrageJson == null || anfrage == null || key == null || anfrage.isEmpty() || key.isEmpty()
						|| anfrage.contains("..") || key.contains("..")) {
					String json = anfrageJson == null ? "null" : anfrageJson.encode();
					System.out.println("Retry.. " + i + " JSON invalid/incomplete: " + json);
					continue;
				}

				// Retry on invalid case - Quality gate - anfrage
				anfrage = anfrage.replace("\"", "").replace("'", "");
				if (!anfrage.startsWith(randomAnfang) || anfrage.contains(":")
						|| anfrage.toLowerCase().contains("anfrage")
						|| anfrage.toLowerCase().trim().equalsIgnoreCase(randomAnfang.toLowerCase().trim())
						|| anfrage.toLowerCase().contains("anweisung") || anfrage.length() > ANFRAGE_OUTPUT_MAX_LEN) {
					int len = anfrage.length();
					// System.out.println(prompt.input());
					System.out.println("Retry.. " + i + " " + anfrage + " / " + randomAnfang + ", len: " + len
							+ ", limit: " + ANFRAGE_OUTPUT_MAX_LEN);
					continue;
				}

				// Retry on invalid case - Quality gate - key
				key = key.replace("\"", "").replace("'", "");
				if (!story.toLowerCase().contains(key.toLowerCase())) {
					System.out.println("Retry.. " + i + " key not found in story: " + key);
					continue;
				}
				return new AnfrageResult(anfrage, key);
			} catch (Exception e) {
				System.out.println("Retry.. " + i + " " + e.getMessage());
				e.printStackTrace();
				// NOOP
			}

		}
		return null;
	}
}
