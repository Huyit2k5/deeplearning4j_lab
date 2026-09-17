package vn.huit.dl4j.training;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import java.io.File;
import java.io.IOException;

/**
 * Doc, chuan hoa (Z-score) va chia train/test cho dataset creditcard.csv
 * (30 feature: Time, V1..V28, Amount; cot cuoi Class la label 0/1).
 */
public final class DataPipeline {

    public static final int NUM_FEATURES = 30;
    public static final int NUM_CLASSES = 2;
    public static final int LABEL_INDEX = 30;

    private DataPipeline() {
    }

    public static DataSet loadFullDataset(File csvFile) throws IOException, InterruptedException {
        RecordReader recordReader = new CSVRecordReader(1, ',');
        recordReader.initialize(new FileSplit(csvFile));

        int totalRows = countDataRows(csvFile);

        DataSetIterator iterator = new RecordReaderDataSetIterator(
                recordReader, totalRows, LABEL_INDEX, NUM_CLASSES);
        return iterator.next();
    }

    public static SplitTestAndTrain splitTrainTest(DataSet fullDataSet, double trainFraction, long seed) {
        fullDataSet.shuffle(seed);
        return fullDataSet.splitTestAndTrain(trainFraction);
    }

    public static NormalizerStandardize fitNormalizer(DataSet trainData) {
        NormalizerStandardize normalizer = new NormalizerStandardize();
        normalizer.fit(trainData);
        return normalizer;
    }

    public static DataSetIterator asyncIterator(DataSetIterator base) {
        return new AsyncDataSetIterator(base, 2);
    }

    private static int countDataRows(File csvFile) throws IOException {
        try (var lines = java.nio.file.Files.lines(csvFile.toPath())) {
            return (int) lines.count() - 1;
        }
    }
}
