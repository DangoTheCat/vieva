package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.application.ports.output.TextChunk;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import com.example.vieva.infrastructure.ai.MockEmbeddingGateway;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentPipelineAdaptersTest {

    private final TikaDocumentParserGateway parser = new TikaDocumentParserGateway();
    private final TikaFileTypeDetectorGateway detector = new TikaFileTypeDetectorGateway();

    @Test
    @DisplayName("chunks respect the token budget, overlap and keep page ranges")
    void splitterOverlapAndPages() {
        // ~27 tokens per sentence: 4 sentences per chunk, the last one repeated as overlap
        TokenTextSplitterGateway splitter = new TokenTextSplitterGateway(120, 40, 5);
        StringBuilder page1 = new StringBuilder();
        StringBuilder page2 = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            page1.append("Câu số ").append(i).append(" nói về giao dịch và tính nhất quán dữ liệu. ");
            page2.append("Mệnh đề ").append(i).append(" mô tả chỉ mục B-tree trong cơ sở dữ liệu. ");
        }
        List<TextChunk> chunks = splitter.split(List.of(
                new ParsedSection(page1.toString(), 1, "Giao dịch"),
                new ParsedSection(page2.toString(), 2, "Chỉ mục")));

        assertThat(chunks).hasSizeGreaterThan(3);
        assertThat(chunks).allMatch(c -> c.tokenCount() <= 120 + 5);
        assertThat(chunks.get(0).pageStart()).isEqualTo(1);
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("Giao dịch");
        assertThat(chunks.get(chunks.size() - 1).pageEnd()).isEqualTo(2);
        assertThat(chunks).anyMatch(c -> c.pageStart() == 1 && c.pageEnd() == 2);
        // overlap: the last sentence of a chunk is repeated at the start of the next one
        for (int i = 0; i + 1 < chunks.size(); i++) {
            String[] sentences = chunks.get(i).text().split("(?<=\\.)\\s+");
            assertThat(chunks.get(i + 1).text()).startsWith(sentences[sentences.length - 1]);
        }
    }

    @Test
    void splitterHardSplitsVeryLongSentences() {
        TokenTextSplitterGateway splitter = new TokenTextSplitterGateway(50, 10, 5);
        String longSentence = "từ ".repeat(400);
        List<TextChunk> chunks = splitter.split(List.of(new ParsedSection(longSentence, null, null)));
        assertThat(chunks).hasSizeGreaterThan(5).allMatch(c -> c.tokenCount() <= 55);
        assertThatThrownBy(() -> new TokenTextSplitterGateway(100, 100, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PPTX slides become page-numbered sections; DOCX and TXT are extracted")
    void parsesOfficeAndText() throws Exception {
        byte[] pptx = pptx("Slide một: khái niệm giao dịch ACID", "Slide hai: chỉ mục và tối ưu truy vấn");
        assertThat(detector.detect(pptx, "slides.pptx"))
                .isEqualTo("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        List<ParsedSection> slides = parser.extract(pptx,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        assertThat(slides).extracting(ParsedSection::pageNumber).contains(1, 2);
        assertThat(slides.stream().filter(s -> Integer.valueOf(2).equals(s.pageNumber())).map(ParsedSection::text))
                .anyMatch(t -> t.contains("chỉ mục"));

        byte[] docx = docx("Chuẩn hoá dữ liệu giúp loại bỏ dư thừa.");
        assertThat(detector.detect(docx, "giao-trinh.docx"))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(parser.extract(docx, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .extracting(ParsedSection::text).anyMatch(t -> t.contains("loại bỏ dư thừa"));

        byte[] txt = "Dòng một.\nDòng hai có dấu tiếng Việt.".getBytes(StandardCharsets.UTF_8);
        assertThat(detector.detect(txt, "notes.txt")).isEqualTo("text/plain");
        assertThat(parser.extract(txt, "text/plain")).extracting(ParsedSection::text)
                .anyMatch(t -> t.contains("tiếng Việt"));
    }

    @Test
    void detectsSpoofedExtensionAndUnreadableFile() {
        byte[] text = "chỉ là văn bản".getBytes(StandardCharsets.UTF_8);
        assertThat(detector.detect(text, "fake.pdf")).isNotEqualTo("application/pdf");
        assertThatThrownBy(() -> parser.extract("%PDF-1.7 broken".getBytes(), "application/pdf"))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.EMPTY_DOCUMENT_TEXT);
    }

    @Test
    @DisplayName("mock embeddings: unit length, deterministic, similar texts are closer")
    void mockEmbeddings() {
        MockEmbeddingGateway embeddings = new MockEmbeddingGateway(1536);
        float[] a = embeddings.generateEmbedding("giao dịch ACID tính nguyên tử");
        float[] b = embeddings.generateEmbedding("tính nguyên tử của giao dịch");
        float[] c = embeddings.generateEmbedding("mạng nơ-ron tích chập xử lý ảnh");
        assertThat(a).hasSize(1536).isEqualTo(embeddings.generateEmbedding("giao dịch ACID tính nguyên tử"));
        assertThat(dot(a, a)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
        assertThat(dot(a, b)).isGreaterThan(dot(a, c));
    }

    @Test
    void localStorageRoundTripAndPathTraversal(@TempDir Path root) {
        LocalFileStorageGateway storage = new LocalFileStorageGateway(root.toString());
        var stored = storage.store("Giáo trình.pdf", new byte[]{1, 2, 3}, "application/pdf");
        assertThat(stored.storageKey()).endsWith(".pdf").doesNotContain("Giáo");
        assertThat(storage.load(stored.storageKey())).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> storage.load("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
    }

    private static double dot(float[] x, float[] y) {
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }
        return sum;
    }

    private static byte[] pptx(String... slideTexts) throws Exception {
        try (XMLSlideShow show = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : slideTexts) {
                XSLFSlide slide = show.createSlide();
                XSLFTextBox box = slide.createTextBox();
                box.setText(text);
            }
            show.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] docx(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(out);
            return out.toByteArray();
        }
    }
}
