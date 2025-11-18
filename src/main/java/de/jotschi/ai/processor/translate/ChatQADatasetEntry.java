package de.jotschi.ai.processor.translate;

import de.jotschi.ai.processor.DatasetEntry;

public class ChatQADatasetEntry implements DatasetEntry {

	private final String messages;
	private final String source;
	private final int rowNum;

	public ChatQADatasetEntry(int rowNum, String messages, String source) {
		this.rowNum = rowNum;
		this.messages = messages;
		this.source = source;
	}

	public String source() {
		return source;
	}

	public String messages() {
		return messages;
	}

	@Override
	public String toString() {
		return "[" + source() + " => title: " + messages() + "]";
	}

	@Override
	public int rowNum() {
		return rowNum;
	}
}
