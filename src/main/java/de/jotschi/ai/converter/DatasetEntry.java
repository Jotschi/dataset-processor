package de.jotschi.ai.converter;

public class DatasetEntry {

	private final String messages;
	private final String source;

	public DatasetEntry(String messages, String source) {
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
}
