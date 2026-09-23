"""Telemetry contract checks, not candidate-quality or performance acceptance."""
import unittest
from report_candidate_stages import observations


class ObservationContract(unittest.TestCase):
    def check_state(self, finished, changed, expected, frame=0, mode="chinese"):
        touches = [dict(action="key", mode=mode, interval_ms="60", expected="ab", up_ns="100", down_ns="75", candidate_submit_ns=str(frame)),
                   dict(action="space", mode=mode, interval_ms="60", expected="ab", up_ns="200", down_ns="175", candidate_submit_ns="0")]
        stages = [dict(mode=mode, query="ab", requested_ns="101", finished_ns=str(finished), presentation_changed=changed)]
        self.assertEqual({"letters": 1, expected: 1}, observations(touches, stages)[mode+"/60"])

    def test_unchanged_is_not_missing(self): self.check_state(130, "false", "ready_unchanged_presentation")
    def test_changed_unobserved_is_not_unchanged(self): self.check_state(130, "true", "ready_changed_without_observed_frame")
    def test_next_down_does_not_supersede_query(self): self.check_state(190, "false", "ready_unchanged_presentation")
    def test_late_completion_is_not_early_success(self): self.check_state(210, "false", "ready_after_observation_window")
    def test_cancelled_is_not_zero_latency(self): self.check_state(0, "false", "undelivered")
    def test_frame_is_observed_even_if_content_repeats(self): self.check_state(130, "false", "observed_frame", frame=150)
    def test_english_uses_no_async_request(self): self.check_state(0, "false", "synchronous_unobserved", mode="english")

    def test_frame_during_next_press_is_counted(self):
        touches = [dict(action="key", mode="chinese", interval_ms="60", expected="ab", up_ns="100", down_ns="75", candidate_submit_ns="190", candidate_pre_draw_ns="180"),
                   dict(action="space", mode="chinese", interval_ms="60", expected="ab", up_ns="200", down_ns="175", candidate_submit_ns="0")]
        self.assertEqual({"letters": 1, "observed_frame": 1, "observed_during_next_press_subset": 1}, observations(touches, [])["chinese/60"])


if __name__ == "__main__": unittest.main()
