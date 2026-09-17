package com.jarvis.nextgen;

import static org.junit.Assert.*;
import org.junit.Test;

public class IntentEngineTest {
    private final IntentEngine engine = new IntentEngine();

    @Test public void normalizeRemovesWakeWord() {
        assertEquals("открой Telegram", engine.normalize("Джарвис, открой Telegram"));
        assertEquals("погода", engine.normalize("jarvis погода"));
    }

    @Test public void confirmationsAndCancellationsAreDetected() {
        assertTrue(engine.isConfirmation("Да"));
        assertTrue(engine.isConfirmation("jarvis, confirm"));
        assertTrue(engine.isCancellation("Нет"));
        assertTrue(engine.isCancellation("отмена"));
        assertFalse(engine.isConfirmation("может быть"));
    }
}
