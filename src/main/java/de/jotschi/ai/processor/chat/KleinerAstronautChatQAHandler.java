package de.jotschi.ai.processor.chat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import de.jotschi.ai.processor.DatasetEntryHandler;
import de.jotschi.ai.processor.chat.llm.anfrage.AnfrageGenerator;
import de.jotschi.ai.processor.chat.llm.anfrage.AnfrageResult;
import de.jotschi.ai.processor.chat.llm.anfrage.qa.QAGenerator;
import de.jotschi.ai.processor.chat.llm.anfrage.qa.QuestionAnswerResult;
import io.metaloom.ai.genai.llm.LLMProvider;
import io.metaloom.ai.genai.llm.LargeLanguageModel;
import io.vertx.core.json.JsonObject;

public class KleinerAstronautChatQAHandler implements DatasetEntryHandler<KleinerAstronautDatasetEntry> {

	private final File outputFile;

	private QAGenerator qaGenerator;

	private AnfrageGenerator anfrageGenerator;

	public KleinerAstronautChatQAHandler(File outputFile, LLMProvider ollama, LargeLanguageModel model) {
		this.outputFile = outputFile;
		this.anfrageGenerator = new AnfrageGenerator(ollama, model);
		this.qaGenerator = new QAGenerator(ollama, model);
	}

	@Override
	public void process(KleinerAstronautDatasetEntry entry) {
		if (entry.id() <= 20738) {
			return;
		}
		try {
			String text = entry.text();
			String word1 = entry.word1();
			String word2 = entry.word2();

			AnfrageResult result = anfrageGenerator.generateTriggerQuestion(text, word1);
			QuestionAnswerResult qa = qaGenerator.generateQA(text);
			if (qa != null) {
				JsonObject jsonOut = new JsonObject();
				jsonOut.put("id", entry.id());
				jsonOut.put("request", result.anfrage());
				jsonOut.put("story", text);
				jsonOut.put("question_typ", qa.typ());
				jsonOut.put("question", qa.question());
				jsonOut.put("answer", qa.answer());

				jsonOut.put("adj_1", entry.adj1());
				jsonOut.put("adj_2", entry.adj2());
				jsonOut.put("topic", entry.topic());
				jsonOut.put("verb", entry.verb());
				jsonOut.put("word_1", word1);
				jsonOut.put("word_2", word2);
				try {
					FileUtils.writeStringToFile(outputFile, jsonOut.encode() + "\n", Charset.defaultCharset(), true);
				} catch (IOException e) {
					System.err.println("Processing failed");
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
