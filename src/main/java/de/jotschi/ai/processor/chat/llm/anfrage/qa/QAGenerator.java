package de.jotschi.ai.processor.chat.llm.anfrage.qa;

import java.util.List;

import de.jotschi.ai.processor.chat.llm.AbstractGenerator;
import io.metaloom.ai.genai.llm.LLMContext;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.metaloom.ai.genai.llm.prompt.Prompt;
import io.metaloom.ai.genai.llm.prompt.impl.PromptImpl;
import io.metaloom.ai.genai.utils.TextUtils;
import io.vertx.core.json.JsonObject;

public class QAGenerator extends AbstractGenerator {

	public QAGenerator(LLMProvider llm, LargeLanguageModel model) {
		super(llm, model);
	}

	private static final List<String> W_FRAGEN = List.of("Wer", "Was", "Wie", "Wo", "Womit", "Wann", "Mit wem",
			"Weshalb", "Warum");

	private static final String ANFRAGE_PROMPT_TEMPLATE = """
			Du bist 8 Jahre alt und hast eine Frage zur Geschichte.
			Schreib eine einzelne kurze, einfache kindgerechte ${typ}-Frage und die kurze Antwort passend zu der Geschichte.
			Umschreibe komplizierte Wörter so das sie von Kindern verstanden werden können. Vermeide komplizierte Fragen.

            * Beginne mit ${typ}...
			* Verwende sehr einfache Sprache. 
			* Halte dich sehr kurz. 
			* Maximal ein Satz und maximal 100 Zeichen.

			Geschichte:
			${text}

			Gib JSON aus:
			{
				"frage": "${typ}..."
				"antwort": "..."
			}
			""";

	public QuestionAnswerResult generateQA(String story) {

		for (int i = 0; i < RETRY_MAX; i++) {
			String randomFrageTyp = W_FRAGEN.get(rand.nextInt(W_FRAGEN.size()));
			Prompt prompt = new PromptImpl(ANFRAGE_PROMPT_TEMPLATE);
			
			if(story.length()>1000) {
				story = story.substring(0,1000);
			}
			prompt.set("text", TextUtils.quote(story));
			prompt.set("typ", randomFrageTyp);
			LLMContext ctx = LLMContext.ctx(prompt, model);
			ctx.setTemperature(0.25);
			try {
				JsonObject json = llm.generateJson(ctx);
				String frage = json.getString("frage");
				String antwort = json.getString("antwort");
				// Retry on invalid case - Quality gate
				boolean invalidFrage = frage == null || frage.isEmpty() || TextUtils.count('?', frage) > 1
						|| frage.contains("...") || !frage.startsWith(randomFrageTyp)
						|| frage.trim().equalsIgnoreCase(randomFrageTyp);

				boolean invalidAnswer = antwort == null || antwort.isEmpty() || antwort.contains("...");
				if (invalidFrage || invalidAnswer) {
					System.out.println("Retry.. " + i + " " + frage + " / " + randomFrageTyp);
					continue;
				}

				return new QuestionAnswerResult(frage, antwort, randomFrageTyp);
			} catch (Exception e) {
				System.out.println("Retry.. " + i + " " + e.getMessage());
				e.printStackTrace();
				// NOOP
			}
		}
		return null;
	}

	
}
