# Candidate-capacity experiment

Development experiment, declared after baseline inspection and before variants:
compare candidate capacities 64 and 128 against 24, retaining the identical 2048
search-state budget, score model and traversal. This tests deeper scrolling access,
not a new ranking model. Require no previously reachable target loss; inspect all
first-eight changes and compare uncached lookup latency before choosing a limit.
Reserved families remain uninspected until the implementation choice is fixed.
