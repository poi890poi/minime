#pragma once
#include <chrono>
#include <cstddef>

struct PredictionBudgetExceeded {};
struct PredictionBudget {
    using Clock = std::chrono::steady_clock;
    inline static thread_local bool enabled = false;
    inline static thread_local size_t used = 0, maximum = 8192;
    inline static thread_local Clock::time_point deadline;
    static void begin(bool active, size_t limit = 8192) {
        enabled = active; used = 0; maximum = limit;
        deadline = Clock::now() + std::chrono::milliseconds(8);
    }
    static void check() {
        if (enabled && (++used > maximum || Clock::now() >= deadline))
            throw PredictionBudgetExceeded{};
    }
};
