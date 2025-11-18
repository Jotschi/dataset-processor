package de.jotschi.ai.processor.chat;

import java.io.File;
import java.util.List;

import org.apache.arrow.vector.table.Row;

import de.jotschi.ai.processor.AbstractProcessor;

public class KleinerAstronautChatProcessor extends AbstractProcessor<KleinerAstronautDatasetEntry> {

	private KleinerAstronautChatQAHandler handler;

	public KleinerAstronautChatProcessor(KleinerAstronautChatQAHandler handler) {
		this.handler = handler;
	}

	@Override
	public void process(File datasetFolder, String filter) {
		process(datasetFolder, filter, handler);
	}

	@Override
	protected List<String> fields() {
		return List.of("topic", "adjective_1", "adjective_2", "verb", "word_1", "word_2", "text");
	}

	@Override
	protected KleinerAstronautDatasetEntry toDatasetEntry(Row row) {
		int rowNum = row.getRowNumber();
		String topic = row.getVarCharObj("topic");
		String adj1 = row.getVarCharObj("adjective_1");
		String adj2 = row.getVarCharObj("adjective_2");
		String verb = row.getVarCharObj("verb");
		String word1 = row.getVarCharObj("word_1");
		String word2 = row.getVarCharObj("word_2");
		String text = row.getVarCharObj("text");

		return new KleinerAstronautDatasetEntry(rowNum, topic, adj1, adj2, verb, word1, word2, text);
	}
}
