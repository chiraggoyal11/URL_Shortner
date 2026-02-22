package com.urlshortener.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Base62EncoderTest {

    @Autowired
    private Base62Encoder encoder;

    @Test
    void testEncode() {
        assertEquals("1", encoder.encode(1L));
        assertEquals("a", encoder.encode(10L));
        assertEquals("A", encoder.encode(36L));
        assertEquals("19", encoder.encode(61L));
        assertEquals("1B", encoder.encode(63L));
    }

    @Test
    void testDecode() {
        assertEquals(1L, encoder.decode("1"));
        assertEquals(10L, encoder.decode("a"));
        assertEquals(36L, encoder.decode("A"));
        assertEquals(61L, encoder.decode("19"));
        assertEquals(63L, encoder.decode("1B"));
    }

    @Test
    void testEncodeDecodeRoundTrip() {
        for (long i = 1; i <= 1000; i++) {
            String encoded = encoder.encode(i);
            long decoded = encoder.decode(encoded);
            assertEquals(i, decoded, "Round trip failed for " + i);
        }
    }

    @Test
    void testInvalidCharacter() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("!@#"));
    }
}
