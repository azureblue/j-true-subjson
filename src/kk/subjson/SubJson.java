package kk.subjson;

import java.util.Optional;

public class SubJson {
    private static class JsonTraverser {
        private final char[] jsonChars;
        private final int len;
        private int pos = 0;

        JsonTraverser(String json) {
            this.jsonChars = json.toCharArray();
            this.len = json.length();
        }

        void goToField(String name) throws PathNotFoundException {
            expectNext('{');
            while (true) {
                expectNext('"');
                if (nextMatches(name)) {
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

        private boolean nextMatches(String name) throws PathNotFoundException {
            int nameLen = name.length();
            for (int i = 0; i < nameLen; i++)
                if (next() != name.charAt(i))
                    return false;
            return true;
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
            return new String(jsonChars, start, pos - start);
        }

        void skipSubJson() throws PathNotFoundException {
            skipWhiteSpaces();
            char ch = next();
            if (ch == '{' || ch == '[') {
                char closing = (char) (ch + 2);
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
                ch = jsonChars[pos];
                if (ch >= 'a' && ch <= 'u')
                    continue;
                if (ch >= '-' && ch <= '9')
                    continue;
                if (ch == 'E')
                    continue;
                return;
            }
            throw new PathNotFoundException();
        }

        void skipWhiteSpaces() {
            while(pos < len)
                if (!Character.isWhitespace(jsonChars[pos++])) {
                    pos--;
                    return;
                }

        }

        private char next() throws PathNotFoundException {
            if (pos >= len)
                throw new PathNotFoundException();
            return jsonChars[pos++];
        }

        private void moveAfterClosingQuote() throws PathNotFoundException {
            while (true)
                if (next() == '"' && jsonChars[pos - 2] != '\\')
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
                int start = pos;
                pos++;
                if (subPathCh == '.')
                    continue;
                if (subPathCh == '[') {
                    int idx = path.indexOf(']', pos);
                    jt.goToArray(Integer.parseInt(path.substring(pos, idx)));
                    pos = idx + 1;
                    continue;
                }
                for (; pos < len; pos++) {
                    char ch = path.charAt(pos);
                    if (ch == '[' || ch == '.')
                        break;
                }
                jt.goToField(path.substring(start, pos));

            }
            return Optional.of(jt.get());
        } catch (PathNotFoundException e) {
            return Optional.empty();
        }
    }

    private static class PathNotFoundException extends Throwable {
    }
}
