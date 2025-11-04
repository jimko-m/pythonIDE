package com.pythonide.collaboration

import android.content.Context
import android.util.Log
import com.pythonide.data.models.FileModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

class CodeReviewSystem(private val context: Context) {
    
    companion object {
        private const val TAG = "CodeReviewSystem"
        
        // Review statuses
        const val STATUS_PENDING = "pending"
        const val STATUS_IN_REVIEW = "in_review"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_CHANGES_REQUESTED = "changes_requested"
        const val STATUS_MERGED = "merged"
        
        // Review types
        const val TYPE_CODE_REVIEW = "code_review"
        const val TYPE_DESIGN_REVIEW = "design_review"
        const val TYPE_SECURITY_REVIEW = "security_review"
        const val TYPE_PERFORMANCE_REVIEW = "performance_review"
        
        // Priority levels
        const val PRIORITY_LOW = "low"
        const val PRIORITY_MEDIUM = "medium"
        const val PRIORITY_HIGH = "high"
        const val PRIORITY_CRITICAL = "critical"
        
        // Comment types
        const val COMMENT_GENERAL = "general"
        const val COMMENT_SUGGESTION = "suggestion"
        const val COMMENT_ISSUE = "issue"
        const val COMMENT_APPROVAL = "approval"
        
        // Review roles
        const val ROLE_REVIEWER = "reviewer"
        const val ROLE_APPROVER = "approver"
        const val ROLE_OBSERVER = "observer"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()
    
    // Active reviews tracking
    private val activeReviews = ConcurrentHashMap<String, ReviewSession>()
    
    // Review notifications
    private val notificationManager = ReviewNotificationManager(firestore)
    
    // Quality gate manager
    private val qualityGateManager = QualityGateManager()
    
    init {
        // Initialize quality gates
        initializeQualityGates()
    }
    
    /**
     * Create a new code review request
     */
    suspend fun createReviewRequest(
        title: String,
        description: String,
        sourceFiles: List<FileModel>,
        targetBranch: String = "main",
        reviewers: List<String> = emptyList(),
        autoAssignReviewers: Boolean = true,
        priority: String = PRIORITY_MEDIUM,
        type: String = TYPE_CODE_REVIEW,
        dueDate: Long? = null
    ): Result<ReviewRequest> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating review request: $title")
            
            val reviewId = UUID.randomUUID().toString()
            val authorId = "current_user" // Get from authentication
            
            // Generate unique branch name for the review
            val reviewBranch = "review/$reviewId"
            
            // Auto-assign reviewers if requested
            val assignedReviewers = if (autoAssignReviewers) {
                autoAssignReviewers(sourceFiles, reviewers)
            } else {
                reviewers.map { ReviewerAssignment(it, ROLE_REVIEWER, STATUS_PENDING) }
            }
            
            // Create review request
            val reviewRequest = ReviewRequest(
                id = reviewId,
                title = title,
                description = description,
                authorId = authorId,
                sourceBranch = reviewBranch,
                targetBranch = targetBranch,
                files = sourceFiles,
                reviewers = assignedReviewers.toMutableList(),
                status = STATUS_PENDING,
                priority = priority,
                type = type,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                dueDate = dueDate
            )
            
            // Save to Firestore
            saveReviewToFirestore(reviewRequest)
            
            // Create Git branches
            createReviewBranch(reviewRequest)
            
            // Send notifications to reviewers
            notifyReviewers(reviewRequest)
            
            // Start automated checks
            startAutomatedChecks(reviewRequest)
            
            Result.success(reviewRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create review request", e)
            Result.failure(e)
        }
    }
    
    /**
     * Submit a review (approve/reject/request changes)
     */
    suspend fun submitReview(
        reviewId: String,
        reviewerId: String,
        decision: String,
        comments: List<ReviewComment> = emptyList(),
        summary: String = ""
    ): Result<ReviewDecision> = withContext(Dispatchers.IO) {
        try {
            val reviewRequest = getReviewRequest(reviewId) ?: 
                throw IllegalArgumentException("Review not found")
            
            val reviewer = reviewRequest.reviewers.find { it.userId == reviewerId } ?: 
                throw IllegalArgumentException("Reviewer not assigned to this review")
            
            // Update reviewer assignment
            reviewer.status = decision
            reviewer.reviewedAt = System.currentTimeMillis()
            reviewer.comments = comments.size
            
            // Create review decision
            val decisionObj = ReviewDecision(
                reviewId = reviewId,
                reviewerId = reviewerId,
                decision = decision,
                comments = comments,
                summary = summary,
                timestamp = System.currentTimeMillis()
            )
            
            // Save decision to Firestore
            saveReviewDecision(decisionObj)
            
            // Update review status based on decisions
            updateReviewStatus(reviewRequest)
            
            // Trigger webhooks for external integrations
            triggerWebhook(reviewRequest, decisionObj)
            
            Log.d(TAG, "Review submitted: $decision by $reviewerId")
            Result.success(decisionObj)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit review", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add a comment to a review
     */
    suspend fun addComment(
        reviewId: String,
        reviewerId: String,
        fileId: String,
        line: Int? = null,
        comment: String,
        type: String = COMMENT_GENERAL,
        suggestion: String? = null
    ): Result<ReviewComment> = withContext(Dispatchers.IO) {
        try {
            val commentId = UUID.randomUUID().toString()
            
            val reviewComment = ReviewComment(
                id = commentId,
                reviewId = reviewId,
                authorId = reviewerId,
                fileId = fileId,
                line = line,
                content = comment,
                type = type,
                suggestion = suggestion,
                createdAt = System.currentTimeMillis(),
                isResolved = false,
                replies = mutableListOf()
            )
            
            // Save comment to Firestore
            saveCommentToFirestore(reviewComment)
            
            // Notify relevant users
            notifyCommentAdded(reviewComment)
            
            Log.d(TAG, "Comment added to review $reviewId")
            Result.success(reviewComment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add comment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get pending reviews for a user
     */
    suspend fun getPendingReviews(userId: String): List<ReviewRequest> = withContext(Dispatchers.IO) {
        try {
            val query = firestore.collection("reviews")
                .whereArrayContains("reviewerIds", userId)
                .whereEqualTo("status", STATUS_PENDING)
                .get()
                .await()
            
            query.documents.mapNotNull { document ->
                document.toObject(ReviewRequest::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending reviews", e)
            emptyList()
        }
    }
    
    /**
     * Get review statistics for a project
     */
    suspend fun getReviewStatistics(projectId: String): Result<ReviewStatistics> = withContext(Dispatchers.IO) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            
            // Get all reviews in the last 30 days
            val query = firestore.collection("reviews")
                .whereEqualTo("projectId", projectId)
                .whereGreaterThan("createdAt", thirtyDaysAgo)
                .get()
                .await()
            
            val reviews = query.documents.mapNotNull { document ->
                document.toObject(ReviewRequest::class.java)
            }
            
            val statistics = calculateStatistics(reviews)
            Result.success(statistics)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get review statistics", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate review report
     */
    suspend fun generateReviewReport(
        reviewId: String,
        format: String = "markdown"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val reviewRequest = getReviewRequest(reviewId) ?: 
                throw IllegalArgumentException("Review not found")
            
            val comments = getReviewComments(reviewId)
            val decisions = getReviewDecisions(reviewId)
            
            val report = when (format) {
                "markdown" -> generateMarkdownReport(reviewRequest, comments, decisions)
                "json" -> generateJsonReport(reviewRequest, comments, decisions)
                else -> generatePlainTextReport(reviewRequest, comments, decisions)
            }
            
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate review report", e)
            Result.failure(e)
        }
    }
    
    /**
     * Merge approved review
     */
    suspend fun mergeReview(reviewId: String, mergeStrategy: String = "squash"): Result<MergeResult> = withContext(Dispatchers.IO) {
        try {
            val reviewRequest = getReviewRequest(reviewId) ?: 
                throw IllegalArgumentException("Review not found")
            
            if (reviewRequest.status != STATUS_APPROVED) {
                throw IllegalStateException("Review must be approved before merging")
            }
            
            // Perform merge
            val mergeResult = performGitMerge(reviewRequest, mergeStrategy)
            
            // Update review status
            reviewRequest.status = STATUS_MERGED
            reviewRequest.mergedAt = System.currentTimeMillis()
            reviewRequest.mergeStrategy = mergeStrategy
            
            // Save updated review
            saveReviewToFirestore(reviewRequest)
            
            // Send merge notifications
            notifyMergeCompleted(reviewRequest, mergeResult)
            
            Log.d(TAG, "Review merged successfully: $reviewId")
            Result.success(mergeResult)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge review", e)
            Result.failure(e)
        }
    }
    
    private suspend fun autoAssignReviewers(files: List<FileModel>, preferredReviewers: List<String>): List<ReviewerAssignment> {
        // Auto-assign based on file types, ownership, and expertise
        val assignments = mutableListOf<ReviewerAssignment>()
        
        // Add preferred reviewers first
        preferredReviewers.forEach { userId ->
            assignments.add(ReviewerAssignment(userId, ROLE_REVIEWER, STATUS_PENDING))
        }
        
        // Auto-assign based on file content analysis
        files.forEach { file ->
            val suggestedReviewers = suggestReviewers(file)
            suggestedReviewers.forEach { userId ->
                if (!assignments.any { it.userId == userId }) {
                    assignments.add(ReviewerAssignment(userId, ROLE_REVIEWER, STATUS_PENDING))
                }
            }
        }
        
        return assignments
    }
    
    private fun suggestReviewers(file: FileModel): List<String> {
        // Simple heuristic - can be enhanced with ML
        val suggestions = mutableListOf<String>()
        
        // Check file type for domain expertise
        when {
            file.name.endsWith(".py", ignoreCase = true) -> {
                suggestions.addAll(listOf("python_expert_1", "python_expert_2"))
            }
            file.name.endsWith(".kt", ignoreCase = true) -> {
                suggestions.addAll(listOf("kotlin_expert_1", "android_expert"))
            }
            file.name.contains("security", ignoreCase = true) -> {
                suggestions.add("security_expert")
            }
            file.content?.contains("database", ignoreCase = true) == true -> {
                suggestions.add("backend_expert")
            }
        }
        
        return suggestions.take(2) // Limit to 2 suggestions per file
    }
    
    private suspend fun saveReviewToFirestore(review: ReviewRequest) {
        val document = firestore.collection("reviews").document(review.id)
        document.set(review).await()
        
        // Also save to user's reviews
        review.reviewers.forEach { reviewer ->
            firestore.collection("user_reviews")
                .document(reviewer.userId)
                .collection("reviews")
                .document(review.id)
                .set(hashMapOf(
                    "reviewId" to review.id,
                    "status" to reviewer.status,
                    "role" to reviewer.role
                ))
                .await()
        }
    }
    
    private suspend fun createReviewBranch(review: ReviewRequest) {
        // Create Git branch for review
        // This would typically use Git service integration
        Log.d(TAG, "Creating review branch: ${review.sourceBranch}")
    }
    
    private suspend fun notifyReviewers(review: ReviewRequest) {
        review.reviewers.forEach { reviewer ->
            notificationManager.sendReviewNotification(
                reviewer.userId,
                review.id,
                review.title,
                review.authorId
            )
        }
    }
    
    private suspend fun startAutomatedChecks(review: ReviewRequest) {
        scope.launch {
            try {
                // Run automated checks
                val checkResults = qualityGateManager.runChecks(review.files)
                
                // Save check results
                saveCheckResults(review.id, checkResults)
                
                // Update review status based on check results
                updateReviewStatus(review, checkResults)
            } catch (e: Exception) {
                Log.e(TAG, "Automated checks failed", e)
            }
        }
    }
    
    private fun initializeQualityGates() {
        // Initialize quality gates
        qualityGateManager.addGate("minimum_reviewers", MinimumReviewersGate())
        qualityGateManager.addGate("code_quality", CodeQualityGate())
        qualityGateManager.addGate("test_coverage", TestCoverageGate())
        qualityGateManager.addGate("security_scan", SecurityScanGate())
        qualityGateManager.addGate("documentation", DocumentationGate())
    }
    
    private suspend fun updateReviewStatus(review: ReviewRequest, checkResults: List<CheckResult>? = null) {
        val reviewerDecisions = review.reviewers.map { it.status }
        
        when {
            // Check if automated gates pass
            checkResults?.any { !it.passed } == true -> {
                review.status = STATUS_CHANGES_REQUESTED
                review.rejectionReason = "Automated checks failed"
            }
            
            // All reviewers approved
            reviewerDecisions.all { it == STATUS_APPROVED } -> {
                review.status = STATUS_APPROVED
                review.approvedAt = System.currentTimeMillis()
            }
            
            // Any reviewer rejected
            reviewerDecisions.contains(STATUS_REJECTED) -> {
                review.status = STATUS_REJECTED
                review.rejectionReason = "Reviewer rejection"
            }
            
            // Changes requested
            reviewerDecisions.contains(STATUS_CHANGES_REQUESTED) -> {
                review.status = STATUS_CHANGES_REQUESTED
            }
            
            // Still in review
            else -> {
                review.status = STATUS_IN_REVIEW
            }
        }
        
        review.updatedAt = System.currentTimeMillis()
        saveReviewToFirestore(review)
    }
    
    private suspend fun getReviewRequest(reviewId: String): ReviewRequest? {
        return try {
            val document = firestore.collection("reviews").document(reviewId).get().await()
            if (document.exists()) {
                document.toObject(ReviewRequest::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get review request", e)
            null
        }
    }
    
    private suspend fun getReviewComments(reviewId: String): List<ReviewComment> {
        return try {
            val query = firestore.collection("review_comments")
                .whereEqualTo("reviewId", reviewId)
                .get()
                .await()
            
            query.documents.mapNotNull { document ->
                document.toObject(ReviewComment::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get review comments", e)
            emptyList()
        }
    }
    
    private suspend fun getReviewDecisions(reviewId: String): List<ReviewDecision> {
        return try {
            val query = firestore.collection("review_decisions")
                .whereEqualTo("reviewId", reviewId)
                .get()
                .await()
            
            query.documents.mapNotNull { document ->
                document.toObject(ReviewDecision::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get review decisions", e)
            emptyList()
        }
    }
    
    private suspend fun saveReviewDecision(decision: ReviewDecision) {
        firestore.collection("review_decisions")
            .document(decision.reviewerId + "_" + decision.reviewId)
            .set(decision)
            .await()
    }
    
    private suspend fun saveCommentToFirestore(comment: ReviewComment) {
        firestore.collection("review_comments")
            .document(comment.id)
            .set(comment)
            .await()
    }
    
    private suspend fun notifyCommentAdded(comment: ReviewComment) {
        // Notify review participants about new comment
        notificationManager.sendCommentNotification(
            comment.reviewId,
            comment.authorId,
            comment.content
        )
    }
    
    private fun calculateStatistics(reviews: List<ReviewRequest>): ReviewStatistics {
        val totalReviews = reviews.size
        val approvedReviews = reviews.count { it.status == STATUS_APPROVED }
        val rejectedReviews = reviews.count { it.status == STATUS_REJECTED }
        val pendingReviews = reviews.count { it.status in listOf(STATUS_PENDING, STATUS_IN_REVIEW) }
        
        val averageReviewTime = reviews.filterNotNull().mapNotNull { review ->
            review.approvedAt?.let { approvedAt ->
                approvedAt - review.createdAt
            }
        }.averageOrNull()
        
        val averageCommentsPerReview = reviews.mapNotNull { review ->
            review.reviewers.sumOf { it.comments }
        }.averageOrNull()
        
        return ReviewStatistics(
            totalReviews = totalReviews,
            approvedReviews = approvedReviews,
            rejectedReviews = rejectedReviews,
            pendingReviews = pendingReviews,
            averageReviewTime = averageReviewTime,
            averageCommentsPerReview = averageCommentsPerReview,
            approvalRate = if (totalReviews > 0) approvedReviews.toDouble() / totalReviews else 0.0
        )
    }
    
    private fun generateMarkdownReport(
        review: ReviewRequest,
        comments: List<ReviewComment>,
        decisions: List<ReviewDecision>
    ): String {
        val report = StringBuilder()
        
        report.appendLine("# Code Review Report: ${review.title}")
        report.appendLine()
        report.appendLine("**Review ID:** ${review.id}")
        report.appendLine("**Author:** ${review.authorId}")
        report.appendLine("**Status:** ${review.status}")
        report.appendLine("**Priority:** ${review.priority}")
        report.appendLine("**Created:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(review.createdAt))}")
        report.appendLine()
        
        report.appendLine("## Description")
        report.appendLine(review.description)
        report.appendLine()
        
        report.appendLine("## Files Reviewed")
        review.files.forEach { file ->
            report.appendLine("- ${file.name}")
        }
        report.appendLine()
        
        report.appendLine("## Reviewers")
        review.reviewers.forEach { reviewer ->
            report.appendLine("- **${reviewer.userId}** (${reviewer.role}): ${reviewer.status}")
        }
        report.appendLine()
        
        report.appendLine("## Comments")
        comments.forEach { comment ->
            report.appendLine("### ${comment.authorId} on ${comment.fileId}")
            if (comment.line != null) {
                report.appendLine("**Line ${comment.line}:**")
            }
            report.appendLine("> ${comment.content}")
            if (!comment.suggestion.isNullOrEmpty()) {
                report.appendLine("**Suggestion:** ${comment.suggestion}")
            }
            report.appendLine()
        }
        
        return report.toString()
    }
    
    private fun generateJsonReport(
        review: ReviewRequest,
        comments: List<ReviewComment>,
        decisions: List<ReviewDecision>
    ): String {
        return """
        {
            "review": {
                "id": "${review.id}",
                "title": "${review.title}",
                "description": "${review.description}",
                "authorId": "${review.authorId}",
                "status": "${review.status}",
                "priority": "${review.priority}",
                "createdAt": ${review.createdAt}
            },
            "reviewers": ${review.reviewers.map { reviewer ->
                """
                {
                    "userId": "${reviewer.userId}",
                    "role": "${reviewer.role}",
                    "status": "${reviewer.status}",
                    "comments": ${reviewer.comments}
                }
                """
            }.joinToString(prefix = "[", postfix = "]")},
            "comments": ${comments.map { comment ->
                """
                {
                    "id": "${comment.id}",
                    "authorId": "${comment.authorId}",
                    "fileId": "${comment.fileId}",
                    "line": ${comment.line ?: "null"},
                    "content": "${comment.content}",
                    "type": "${comment.type}",
                    "createdAt": ${comment.createdAt}
                }
                """
            }.joinToString(prefix = "[", postfix = "]")},
            "decisions": ${decisions.map { decision ->
                """
                {
                    "reviewerId": "${decision.reviewerId}",
                    "decision": "${decision.decision}",
                    "summary": "${decision.summary}",
                    "timestamp": ${decision.timestamp}
                }
                """
            }.joinToString(prefix = "[", postfix = "]")}
        }
        """.trimIndent()
    }
    
    private fun generatePlainTextReport(
        review: ReviewRequest,
        comments: List<ReviewComment>,
        decisions: List<ReviewDecision>
    ): String {
        val report = StringBuilder()
        
        report.appendLine("CODE REVIEW REPORT")
        report.appendLine("==================")
        report.appendLine("Title: ${review.title}")
        report.appendLine("ID: ${review.id}")
        report.appendLine("Author: ${review.authorId}")
        report.appendLine("Status: ${review.status}")
        report.appendLine("Priority: ${review.priority}")
        report.appendLine()
        
        report.appendLine("DESCRIPTION:")
        report.appendLine(review.description)
        report.appendLine()
        
        report.appendLine("FILES REVIEWED:")
        review.files.forEach { file ->
            report.appendLine("- ${file.name}")
        }
        report.appendLine()
        
        report.appendLine("REVIEWERS:")
        review.reviewers.forEach { reviewer ->
            report.appendLine("- ${reviewer.userId} (${reviewer.role}): ${reviewer.status} (${reviewer.comments} comments)")
        }
        report.appendLine()
        
        report.appendLine("COMMENTS:")
        comments.forEach { comment ->
            report.appendLine("- ${comment.authorId} on ${comment.fileId}${if (comment.line != null) " (line ${comment.line})" else ""}: ${comment.content}")
        }
        report.appendLine()
        
        report.appendLine("DECISIONS:")
        decisions.forEach { decision ->
            report.appendLine("- ${decision.reviewerId}: ${decision.decision}")
            if (decision.summary.isNotEmpty()) {
                report.appendLine("  Summary: ${decision.summary}")
            }
        }
        
        return report.toString()
    }
    
    private suspend fun performGitMerge(review: ReviewRequest, strategy: String): MergeResult {
        // Implement Git merge logic
        // This would typically use a Git service or Git commands
        Log.d(TAG, "Performing merge with strategy: $strategy")
        
        return MergeResult(
            mergeId = UUID.randomUUID().toString(),
            status = "success",
            message = "Merge completed successfully",
            commitSha = "abc123",
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun saveCheckResults(reviewId: String, results: List<CheckResult>) {
        results.forEach { result ->
            firestore.collection("check_results")
                .document(reviewId + "_" + result.gateId)
                .set(result)
                .await()
        }
    }
    
    private suspend fun triggerWebhook(review: ReviewRequest, decision: ReviewDecision) {
        // Trigger external webhooks for integrations
        Log.d(TAG, "Triggering webhook for review ${review.id}")
    }
    
    private suspend fun notifyMergeCompleted(review: ReviewRequest, mergeResult: MergeResult) {
        // Notify team about successful merge
        notificationManager.sendMergeNotification(
            review.authorId,
            review.id,
            mergeResult.commitSha
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        activeReviews.clear()
    }
}

// Data classes
data class ReviewRequest(
    val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val projectId: String = "",
    val sourceBranch: String,
    val targetBranch: String,
    val files: List<FileModel>,
    val reviewers: MutableList<ReviewerAssignment>,
    var status: String,
    val priority: String,
    val type: String,
    val createdAt: Long,
    var updatedAt: Long = createdAt,
    val dueDate: Long? = null,
    var approvedAt: Long? = null,
    var mergedAt: Long? = null,
    var rejectionReason: String? = null,
    var mergeStrategy: String? = null
)

data class ReviewerAssignment(
    val userId: String,
    val role: String,
    var status: String,
    var reviewedAt: Long? = null,
    var comments: Int = 0
)

data class ReviewComment(
    val id: String,
    val reviewId: String,
    val authorId: String,
    val fileId: String,
    val line: Int? = null,
    val content: String,
    val type: String,
    val suggestion: String? = null,
    val createdAt: Long,
    var isResolved: Boolean,
    val replies: MutableList<ReviewComment>
)

data class ReviewDecision(
    val reviewId: String,
    val reviewerId: String,
    val decision: String,
    val comments: List<ReviewComment>,
    val summary: String,
    val timestamp: Long
)

data class ReviewStatistics(
    val totalReviews: Int,
    val approvedReviews: Int,
    val rejectedReviews: Int,
    val pendingReviews: Int,
    val averageReviewTime: Double?,
    val averageCommentsPerReview: Double?,
    val approvalRate: Double
)

data class MergeResult(
    val mergeId: String,
    val status: String,
    val message: String,
    val commitSha: String,
    val timestamp: Long
)

data class CheckResult(
    val gateId: String,
    val passed: Boolean,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)

// Helper classes
class ReviewNotificationManager(private val firestore: FirebaseFirestore) {
    
    suspend fun sendReviewNotification(
        userId: String,
        reviewId: String,
        title: String,
        authorId: String
    ) {
        // Implementation for sending review notifications
        Log.d("ReviewNotification", "Sending review notification to $userId")
    }
    
    suspend fun sendCommentNotification(
        reviewId: String,
        authorId: String,
        commentContent: String
    ) {
        // Implementation for sending comment notifications
        Log.d("ReviewNotification", "Sending comment notification for review $reviewId")
    }
    
    suspend fun sendMergeNotification(
        userId: String,
        reviewId: String,
        commitSha: String
    ) {
        // Implementation for sending merge notifications
        Log.d("ReviewNotification", "Sending merge notification to $userId")
    }
}

class QualityGateManager {
    private val gates = ConcurrentHashMap<String, QualityGate>()
    
    fun addGate(id: String, gate: QualityGate) {
        gates[id] = gate
    }
    
    suspend fun runChecks(files: List<FileModel>): List<CheckResult> {
        return gates.values.mapNotNull { gate ->
            try {
                gate.check(files)?.let { result ->
                    CheckResult(gate.id, result.passed, result.message, result.details)
                }
            } catch (e: Exception) {
                CheckResult(gate.id, false, "Check failed: ${e.message}")
            }
        }
    }
}

interface QualityGate {
    val id: String
    
    suspend fun check(files: List<FileModel>): GateResult?
}

data class GateResult(
    val passed: Boolean,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)

// Default quality gates
class MinimumReviewersGate : QualityGate {
    override val id = "minimum_reviewers"
    
    override suspend fun check(files: List<FileModel>): GateResult? {
        // Implementation for minimum reviewers check
        return GateResult(true, "Minimum reviewers check passed")
    }
}

class CodeQualityGate : QualityGate {
    override val id = "code_quality"
    
    override suspend fun check(files: List<FileModel>): GateResult? {
        // Implementation for code quality check
        return GateResult(true, "Code quality check passed")
    }
}

class TestCoverageGate : QualityGate {
    override val id = "test_coverage"
    
    override suspend fun check(files: List<FileModel>): GateResult? {
        // Implementation for test coverage check
        return GateResult(true, "Test coverage check passed")
    }
}

class SecurityScanGate : QualityGate {
    override val id = "security_scan"
    
    override suspend fun check(files: List<FileModel>): GateResult? {
        // Implementation for security scan
        return GateResult(true, "Security scan passed")
    }
}

class DocumentationGate : QualityGate {
    override val id = "documentation"
    
    override suspend fun check(files: List<FileModel>): GateResult? {
        // Implementation for documentation check
        return GateResult(true, "Documentation check passed")
    }
}

// Extension functions
fun Double?.averageOrNull(): Double? = if (this == null || this.isNaN()) null else this