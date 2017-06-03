package kk.subjson;

import java.util.Optional;

public class SubJson {
    private static class JsonTraverser {
        private final String json;
        private final int len;
        private int pos = 0;

        JsonTraverser(String json) {
            this.json = json;
            this.len = json.length();
        }

        void goToField(String name) throws PathNotFoundException {
            int nameLen = name.length();
            expectNext('{');
            while (true) {
                expectNext('"');
                if (json.regionMatches(pos, name, 0, nameLen)) {
                    pos += nameLen;
                    if (next() == '"') {
                        expectNext(':');
                        return;
                    }
                }
                moveAfterClosingQuote();
                expectNext(':');
                skipSubJson();
                expectNext(',');
            }
        }

        void goToArray(int idx) throws PathNotFoundException {
            expectNext('[');
            for (int i = 0; i < idx; i++) {
                skipSubJson();
                expectNext(',');
            }
        }

        String get() throws PathNotFoundException {
            skipWhiteSpaces();
            int start = pos;
            skipSubJson();
            return json.substring(start, pos);
        }

        void skipSubJson() throws PathNotFoundException {
            skipWhiteSpaces();
            char ch = next();
            if (ch == '{' || ch == '[') {
                char closing = ch == '{' ? '}' : ']';
                int openings = 0;
                while (true) {
                    char next = next();
                    if (next == '"') {
                        moveAfterClosingQuote();
                        continue;
                    }
                    if (next == closing)
                        if (openings == 0)
                            return;
                        else openings--;
                    else if (next == ch)
                        openings++;
                }
            }

            if (ch == '"') {
                moveAfterClosingQuote();
                return;
            }

            for (; pos < len; pos++) {
                ch = ch();
                if (!(Character.isDigit(ch) || ch == '-' || ch == '.' || ch == 'e' || ch == 'E'))
                    return;
            }
            throw new PathNotFoundException();
        }

        void skipWhiteSpaces() {
            for (; pos < len; pos++)
                if (!Character.isWhitespace(ch()))
                    return;
        }

        private char ch() {
            return json.charAt(pos);
        }

        private char ch(int posOffset) {
            return json.charAt(pos + posOffset);
        }

        private char next() throws PathNotFoundException {
            if (pos >= len)
                throw new PathNotFoundException();
            return json.charAt(pos++);
        }

        private void moveAfterClosingQuote() throws PathNotFoundException {
            while (true)
                if (next() == '"' && ch(-2) != '\\')
                    return;
        }

        private void expectNext(char ch) throws PathNotFoundException {
            skipWhiteSpaces();
            if (next() != ch)
                throw new PathNotFoundException();
        }
    }

    private SubJson() {
    }

    public static Optional<String> subJson(String json, String path) {
        JsonTraverser jt = new JsonTraverser(json);
        int len = path.length();
        int pos = 0;
        try {
            while (pos < len) {
                char subPathCh = path.charAt(pos);
                if (subPathCh == '.') {
                    pos++;
                } else if (subPathCh == '[') {
                    int start = pos;
                    for (; pos < len; pos++) {
                        char ch = path.charAt(pos);
                        if (ch == ']') {
                            jt.goToArray(Integer.parseInt(path.substring(start + 1, pos)));
                            pos++;
                            break;
                        }
                    }
                } else {
                    int start = pos;
                    for (; pos < len; pos++) {
                        char ch = path.charAt(pos);
                        if (ch == '[' || ch == '.')
                            break;
                    }
                    jt.goToField(path.substring(start, pos));
                }
            }
            return Optional.of(jt.get());
        } catch (PathNotFoundException e) {
            return Optional.empty();
        }
    }

    private static class PathNotFoundException extends Throwable {
    }
}
