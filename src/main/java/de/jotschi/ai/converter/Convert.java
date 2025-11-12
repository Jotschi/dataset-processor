package de.jotschi.ai.converter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.table.Row;
import org.apache.arrow.vector.table.Table;
import org.apache.arrow.vector.util.JsonStringHashMap;

import me.tongfei.progressbar.ProgressBar;

public class Convert {

	public void convert() {
		ProgressBar pb = new ProgressBar("Scan", 1000);
		DatasetEntryProcessor processor = new TranslatingProcessor();

		File datasetFolder = new File("dataset");
		FilenameFilter filenameFilter = (dir, name) -> name.endsWith(".parquet");
		for (File file : datasetFolder.listFiles(filenameFilter)) {
			scanFile(pb, file, processor);
		}
	}

	private void scanFile(ProgressBar pb, File file, DatasetEntryProcessor processor) {
		scanFile(file, row -> {
			List<?> msgs = row.getList("messages");
			for (Object msg : msgs) {
				if (msg instanceof JsonStringHashMap jsonMap) {
					Object role = jsonMap.get("role");
					Object content = jsonMap.get("content");
					System.out.println("Role:" + role);
					System.out.println("Content:" + content);
				} else {
					System.out.println(msg.getClass().getName());
				}
			}
			String source = row.getVarCharObj("source");

			DatasetEntry entry = new DatasetEntry("", source);
			processor.process(entry);

			if (pb != null) {
				pb.step();
			}
		}, "messages", "source");
	}

	private void scanFile(File file, Consumer<Row> rowConsumer, String... fields) {
		String uri = file.toURI().toString();
		ScanOptions options = new ScanOptions(/* batchSize */ 32768, Optional.of(fields));
		try (BufferAllocator allocator = new RootAllocator();
				DatasetFactory datasetFactory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(),
						FileFormat.PARQUET, uri)) {

			Dataset dataset = datasetFactory.finish();
			Scanner scanner = dataset.newScan(options);
			try (ArrowReader reader = scanner.scanBatches()) {
				while (reader.loadNextBatch()) {
					try (VectorSchemaRoot root = reader.getVectorSchemaRoot()) {
						try (Table t = new Table(root)) {
							t.forEach(rowConsumer);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
