package consulo.kotlin.plugin.test;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.file.LanguageFileType;
import consulo.language.lexer.Lexer;
import consulo.test.junit.impl.language.SimpleParsingTest;
import consulo.kotlin.compiler.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlighter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs the remapped KotlinHighlighter over highlighting/*.kt and compares every token's text attribute keys
 * with highlighting/*.txt. Extends SimpleParsingTest only for its Consulo test environment.
 */
public class KotlinSyntaxHighlightingTest extends SimpleParsingTest<Object> {
    public KotlinSyntaxHighlightingTest() {
        super("highlighting", "kt");
    }

    @Override
    protected LanguageFileType getFileType(Context context, @Nullable Object testContext) {
        return KotlinFileType.INSTANCE;
    }

    @Test
    public void testHighlighting(Context context) throws Exception {
        doHighlightingTest("Highlighting");
    }

    private void doHighlightingTest(String name) throws IOException {
        String text = read("highlighting/" + name + ".kt");
        assertNotNull(text, name + ".kt");

        SyntaxHighlighter highlighter = new KotlinHighlighter();
        Lexer lexer = highlighter.getHighlightingLexer();
        lexer.start(text);
        StringBuilder actual = new StringBuilder();
        while (lexer.getTokenType() != null) {
            String tokenText = text.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            TextAttributesKey[] keys = highlighter.getTokenHighlights(lexer.getTokenType());
            if (!tokenText.isBlank()) {
                actual.append(lexer.getTokenType()).append(" '").append(tokenText.replace("\n", "\\n")).append("'");
                if (keys.length > 0) {
                    actual.append(" -> ").append(Arrays.stream(keys).map(TextAttributesKey::getExternalName).collect(Collectors.joining(", ")));
                }
                actual.append('\n');
            }
            lexer.advance();
        }

        String expected = read("highlighting/" + name + ".txt");
        if (expected == null) {
            Path out = Path.of("target", "highlighting-actual", name + ".txt");
            Files.createDirectories(out.getParent());
            Files.writeString(out, actual);
        }
        assertEquals(expected, actual.toString(), "expected highlighting/" + name + ".txt (actual written to target/highlighting-actual)");
    }

    private static @Nullable String read(String resource) throws IOException {
        try (InputStream in = KotlinSyntaxHighlightingTest.class.getClassLoader().getResourceAsStream(resource)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
