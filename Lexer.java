package plc.project;

import java.util.List;
import java.util.ArrayList;

//import regex
//import java.util.regex.Pattern;
//import java.util.regex.Matcher;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> res = new ArrayList<>();
        while (chars.index != chars.input.length()) {
            res.add(lexToken());
        }
        return res;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        char first = chars.input.charAt(0);
        if (Character.isDigit(first) || (first == '-' &&  chars.input.length() > 1 && Character.isDigit(chars.input.charAt(1))) || first == '.')
            return lexNumber();
        else if (first == '\'')
            return lexCharacter();
        else if (first == '\"')
            return lexString();
        else if (Character.isAlphabetic(first) || first == '@' || (first == '-' &&  chars.input.length() > 1 && Character.isAlphabetic(chars.input.charAt(1))))
            return lexIdentifier();
        else
            return lexOperator();
    }

    public Token lexIdentifier() {
        String pattern = "@?[A-Za-z]([A-Za-z0-9_-])*";
        String temp = "";
        for (int i = 0; i < chars.input.length(); i++) {
            temp += chars.input.charAt(i);
            if (temp.matches(pattern)) {
                chars.advance();
            }
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (chars.input.contains("."))
            return lexDecimal();
        String pattern = "-?[1-9][0-9]*";
        String temp = "";
        for (int i = 0; i < chars.input.length(); i++) {
            temp += chars.input.charAt(i);
            if (chars.input.charAt(i) == '-' && i == 0 && chars.input.length() > 1) {
                chars.advance();
            } else if (temp.matches(pattern)) {
                chars.advance();
            }
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexDecimal() {
        String input = chars.input.toString();

        // Define a regular expression pattern for a valid decimal number
        String decimalPattern = "([-]?[1-9][0-9]*\\.[0-9]+|[0-9]+\\.[0-9]+)";

        // Check if the input matches the decimal pattern
        if (input.matches("^" + decimalPattern)) {
            return new Token(Token.Type.DECIMAL, input, 0);
        } else {
            return new Token(Token.Type.DECIMAL, "Invalid decimal", 0);
        }
    }

    public Token lexCharacter() {
        String input = chars.input.toString();

        // Check if the input matches the character pattern
        if (input.matches("^'([^'\n\r\\\\]|\\\\.)'$")) {
            return new Token(Token.Type.CHARACTER, input, 0);
        } else {
            return new Token(Token.Type.CHARACTER, "Invalid character", 0);
        }
    }

    public Token lexString() {
        if (chars.input.length() > 0 || chars.input.charAt(0) == '\"') {
            chars.advance();
        }
        else {
            throw new ParseException(chars.input.substring(0, 1), 0);
        }
        for (int i = 1; i < chars.input.length() - 1; i++) {
            char temp = chars.input.charAt(i);
            if ((chars.input.charAt(i) + "").matches("[^\\\\]")) {
                chars.advance();
            }
            else if (chars.input.charAt(i) == '\\') {
                if (chars.input.length() > i + 1 && (chars.input.charAt(i + 1) + "").matches("[bnrt\'\"\\\\]")) {
                    chars.advance();
                    chars.advance();
                    i++;
                }
                else {
                    throw new ParseException(chars.input.substring(0, chars.index), chars.index);
                }
            }
            else {
                throw new ParseException(chars.input.substring(0, chars.index), chars.index);
            }
        }
        if (chars.input.charAt(chars.input.length()-1) == '\"') {
            chars.advance();
        }
        else {
            throw new ParseException(chars.input.substring(0, chars.input.length()-1), chars.input.length()-1);
        }
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        match("\\", "[bnrts]");
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i=0; i < patterns.length; i++){
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
