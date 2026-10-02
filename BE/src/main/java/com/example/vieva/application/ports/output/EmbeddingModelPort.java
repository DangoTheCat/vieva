package com.example.vieva.application.ports.output;

import java.util.List;

public interface EmbeddingModelPort {
    float[] generateEmbedding(String text);
    List<float[]> generateEmbeddings(List<String> texts);
}
