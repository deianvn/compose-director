package com.github.deianvn.compose.director.android.viewmodel.job


/**
 * Determines how [JobLauncher.launch] treats existing jobs that share the same key.
 */
enum class LaunchStrategy {
    /** Cancel any running job(s) for the key and start a fresh one. */
    LATEST,

    /** Start only if no job for the key is currently active; otherwise ignore the request. */
    IF_IDLE,

    /** Queue the new job to run after the most recent job for the key finishes. */
    SEQUENTIAL,

    /** Run the new job concurrently with any existing jobs for the key. */
    PARALLEL
}
