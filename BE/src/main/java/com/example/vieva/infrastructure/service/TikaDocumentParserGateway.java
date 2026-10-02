package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.DocumentParserPort;
import com.example.vieva.application.ports.output.ParsedSection;
import com.example.vieva.domain.exception.AppException;
import com.example.vieva.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tika XHTML parsing that keeps structure: PDF pages ({@code div.page}), PPTX slides
 * ({@code div.slide-content}) and headings (h1-h6) become section metadata of the chunks.
 */
@Slf4j
@Component
public class TikaDocumentParserGateway implements DocumentParserPort {

    /** Upper bound on extracted characters, guards memory on huge documents. */
    private static final int MAX_CHARS = 5_000_000;

    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public List<ParsedSection> extract(byte[] content, String mimeType) {
        StructureHandler handler = new StructureHandler();
        Metadata metadata = new Metadata();
        if (mimeType != null) {
            metadata.set(Metadata.CONTENT_TYPE, mimeType);
        }
        try (InputStream input = new ByteArrayInputStream(content)) {
            parser.parse(input, handler, metadata, new ParseContext());
        } catch (EncryptedDocumentException e) {
            throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT, "Document is password-protected; text cannot be extracted");
        } catch (Exception e) {
            if (handler.sections().isEmpty()) {
                log.warn("Tika failed to parse a {} document: {}", mimeType, e.getClass().getSimpleName());
                throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT,
                        "The file could not be read as " + (mimeType == null ? "a document" : mimeType));
            }
            // Keep what was extracted before a late failure (e.g. a corrupt trailing object).
            log.warn("Partial text extraction for a {} document: {}", mimeType, e.getClass().getSimpleName());
        }
        if (metadata.get(TikaCoreProperties.TIKA_PARSED_BY) == null && handler.sections().isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_DOCUMENT_TEXT);
        }
        return handler.sections();
    }

    /** SAX handler splitting Tika's XHTML output into page/slide sections. */
    static final class StructureHandler extends DefaultHandler {
        private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
        private static final Set<String> BLOCKS = Set.of("p", "div", "li", "tr", "br", "h1", "h2", "h3", "h4", "h5", "h6");

        private final List<ParsedSection> sections = new ArrayList<>();
        private final StringBuilder current = new StringBuilder();
        private final StringBuilder heading = new StringBuilder();
        private Integer page;
        private int pageCounter;
        private String sectionTitle;
        private boolean inHeading;
        private int totalChars;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName == null || localName.isEmpty() ? qName : localName;
            String cssClass = attributes.getValue("class");
            if ("div".equals(name) && ("page".equals(cssClass) || "slide-content".equals(cssClass))) {
                flush();
                pageCounter++;
                page = pageCounter;
            }
            if (HEADINGS.contains(name)) {
                flush();
                inHeading = true;
                heading.setLength(0);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName == null || localName.isEmpty() ? qName : localName;
            if (HEADINGS.contains(name)) {
                inHeading = false;
                String title = heading.toString().replaceAll("\\s+", " ").trim();
                if (!title.isEmpty()) {
                    sectionTitle = title.length() > 200 ? title.substring(0, 200) : title;
                    current.append(title).append('\n');
                }
            } else if (BLOCKS.contains(name)) {
                current.append('\n');
            }
            if ("div".equals(name) && page != null && current.length() > 0) {
                // End of a page/slide container: close the section on the page boundary.
                flush();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (totalChars >= MAX_CHARS) {
                return;
            }
            int accepted = Math.min(length, MAX_CHARS - totalChars);
            totalChars += accepted;
            if (inHeading) {
                heading.append(ch, start, accepted);
            } else {
                current.append(ch, start, accepted);
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            current.append(' ');
        }

        @Override
        public void endDocument() {
            flush();
        }

        private void flush() {
            String text = current.toString()
                    .replace(' ', ' ')
                    .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                    .replaceAll(" *\\n[ \\n]*", "\n")
                    .trim();
            current.setLength(0);
            if (!text.isEmpty()) {
                sections.add(new ParsedSection(text, page, sectionTitle));
            }
        }

        List<ParsedSection> sections() {
            return sections;
        }
    }
}
