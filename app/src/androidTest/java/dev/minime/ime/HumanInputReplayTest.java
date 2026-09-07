package dev.minime.ime;

/** Slow wall-clock replays reuse the visible-IME fixture, with explicit runner selection. */
public final class HumanInputReplayTest extends KeyboardInteractionTest {
    public void testDevelopmentReplay() throws Throwable {humanReplay("dev");}
    public void testHoldoutReplay() throws Throwable {humanReplay("test");}
}
