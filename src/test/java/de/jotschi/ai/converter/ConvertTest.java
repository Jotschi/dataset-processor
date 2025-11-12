package de.jotschi.ai.converter;

import org.junit.jupiter.api.Test;

public class ConvertTest {

	@Test
	public void testConvert() {
		// add --add-opens=java.base/java.nio=ALL-UNNAMED

		new Convert().convert();
	}
}
