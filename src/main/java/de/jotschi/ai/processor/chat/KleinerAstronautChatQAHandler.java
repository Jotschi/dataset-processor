package de.jotschi.ai.processor.chat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import de.jotschi.ai.processor.DatasetEntryHandler;
import de.jotschi.ai.processor.chat.llm.AnfrageGenerator;
import de.jotschi.ai.processor.chat.llm.QAGenerator;
import de.jotschi.ai.processor.chat.llm.QuestionAnswer;
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
		String text = entry.text();
		if (entry.id() <= 12429) {
			return;
		}
		try {
			String anfrage = anfrageGenerator.generateTriggerQuestion(text);
			QuestionAnswer qa = qaGenerator.generateQA(text);
			if (qa != null) {
				JsonObject jsonOut = new JsonObject();
				jsonOut.put("id", entry.id());
				jsonOut.put("request", anfrage);
				jsonOut.put("story", text);
				jsonOut.put("question_typ", qa.typ());
				jsonOut.put("question", qa.question());
				jsonOut.put("answer", qa.answer());

				jsonOut.put("adj_1", entry.adj1());
				jsonOut.put("adj_2", entry.adj2());
				jsonOut.put("topic", entry.topic());
				jsonOut.put("verb", entry.verb());
				jsonOut.put("word_1", entry.word1());
				jsonOut.put("word_2", entry.word2());
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
