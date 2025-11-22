package de.jotschi.ai.processor.chat.llm;

import java.util.List;

import io.metaloom.ai.genai.llm.LLMContext;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.metaloom.ai.genai.llm.prompt.Prompt;
import io.metaloom.ai.genai.llm.prompt.impl.PromptImpl;
import io.metaloom.ai.genai.utils.TextUtils;
import io.vertx.core.json.JsonObject;

public class AnfrageGenerator extends AbstractGenerator {

	private static final int MAX_LEN = 150;

	private static final List<String> ANFRAGE_LIST = List.of("Schreibe mir", "Erfinde eine", "Schreib eine",
			"Schreib ein", "Erzähle mir", "Kannst du mir", "Bitte erzähle mir", "Eine Geschichte", "Ein Abendteuer",
			"Es war einmal", "Ich möchte gerne", "Lies mir", "Kannst du");

	final String GENERATE_ANFRAGE_PROMPT_TEMPLATE = """
			Du bist 8 Jahre alt und möchtest eine Geschichte/Abenteuer.
			Schreib eine einzelne sehr kurze, einfache kindgerechte Anfrage (${anfang}..) für die Geschichte.
			
			* Beginne mit ${anfang}...
			* Verwende sehr einfache Sprache. 
			* Halte dich sehr kurz. 
			* Maximal ein Satz und maximal 100 Zeichen.

			Ideen:
			* eine Geschichte/Abendteuer..
			* von/über/mit/bei dem..

			Geschichte:
			${text}
			
			Gib gültiges JSON aus:
			{
				"anfrage": "..."
			}
			""";

	
	public AnfrageGenerator(LLMProvider llm, LargeLanguageModel model) {
		super(llm, model);
	}

	public String generateTriggerQuestion(String story) {
		for (int i = 0; i < RETRY_MAX; i++) {
			String randomAnfang = ANFRAGE_LIST.get(rand.nextInt(ANFRAGE_LIST.size()));

			Prompt prompt = new PromptImpl(GENERATE_ANFRAGE_PROMPT_TEMPLATE);
			if(story.length()>1000) {
				story = story.substring(0,1000);
			}
			prompt.set("text", TextUtils.quote(story));
			prompt.set("anfang", randomAnfang);

			LLMContext ctx = LLMContext.ctx(prompt, model);
			ctx.setTemperature(0.25);
			try {
				JsonObject out = llm.generateJson(ctx);
				String anfrage = out.getString("anfrage");

				// Retry on invalid case - Quality gate
				if (anfrage == null || anfrage.isEmpty() || anfrage.contains("...") || !anfrage.startsWith(randomAnfang)
						|| anfrage.contains("kindliche") || anfrage.toLowerCase().contains("anfrage") || anfrage.toLowerCase().contains("anweisung")
						|| anfrage.length() > MAX_LEN) {
					System.out.println("Retry.. " + i + " " + anfrage + " / " + randomAnfang + ", len: "
							+ anfrage.length() + ", limit: " + MAX_LEN);
					continue;
				}
				System.out.println("OK: " + anfrage);
				return anfrage;
			} catch (Exception e) {
				System.out.println("Retry.. " + i + " " + e.getMessage());
				e.printStackTrace();
				// NOOP
			}

		}
		return null;
	}
}
