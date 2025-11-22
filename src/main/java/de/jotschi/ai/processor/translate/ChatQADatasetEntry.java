package de.jotschi.ai.processor.translate;

import de.jotschi.ai.processor.DatasetEntry;

public class ChatQADatasetEntry implements DatasetEntry {

	private final String messages;
	private final String source;
	private final long id;

	public ChatQADatasetEntry(long id, String messages, String source) {
		this.id = id;
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
	public long id() {
		return id;
	}
}
