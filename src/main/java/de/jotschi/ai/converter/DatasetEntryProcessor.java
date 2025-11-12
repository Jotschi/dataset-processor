package de.jotschi.ai.converter;

@FunctionalInterface
public interface DatasetEntryProcessor {

    void process(DatasetEntry entry);
}
