package com.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_CHARS.length();

    /**
     * Encode a Long ID to Base62 string
     */
    public String encode(Long id) {
        if (id == null || id == 0) {
            return BASE62_CHARS.substring(0, 1);
        }

        StringBuilder encoded = new StringBuilder();
        long num = id;

        while (num > 0) {
            int remainder = (int) (num % BASE);
            encoded.insert(0, BASE62_CHARS.charAt(remainder));
            num = num / BASE;
        }

        return encoded.toString();
    }

    /**
     * Decode a Base62 string to Long ID
     */
    public Long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return 0L;
        }

        long decoded = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int value = BASE62_CHARS.indexOf(c);
            if (value == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            decoded = decoded * BASE + value;
        }

        return decoded;
    }
}
