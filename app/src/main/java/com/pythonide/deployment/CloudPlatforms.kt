package com.pythonide.deployment

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * Cloud Platform Manager - Handles integrations with various cloud platforms
 */
class CloudPlatforms(private val context: Context) {
    
    companion object {
        private const val TAG = "CloudPlatforms"
        
        // Platform types
        const val TYPE_PAAS = "paas"
        const val TYPE_IAAS = "iaas"
        const val TYPE_FAAS = "faas"
        const val TYPE_SAAS = "saas"
        const val TYPE_CONTAINER = "container"
        const val TYPE_SERVERLESS = "serverless"
        
        // Deployment models
        const val MODEL_DOCKER = "docker"
        const val MODEL_KUBERNETES = "kubernetes"
        const val MODEL_VM = "vm"
        const val MODEL_SERVERLESS = "serverless"
        const val MODEL_STATIC = "static"
        const val MODEL_NATIVE = "native"
        
        // Resource types
        const val RESOURCE_CPU = "cpu"
        const val RESOURCE_MEMORY = "memory"
        const val RESOURCE_STORAGE = "storage"
        const val RESOURCE_BANDWIDTH = "bandwidth"
        const val RESOURCE_INSTANCES = "instances"
        
        // Platform status
        const val STATUS_ONLINE = "online"
        const val STATUS_OFFLINE = "offline"
        const val STATUS_MAINTENANCE = "maintenance"
        const val STATUS_DEGRADED = "degraded"
        
        // Default configurations
        private const val DEFAULT_TIMEOUT = 30000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RATE_LIMIT_WINDOW = 60000L // 1 minute
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    // Platform registry
    private val platforms = ConcurrentHashMap<String, CloudPlatform>()
    private val platformClients = ConcurrentHashMap<String, PlatformClient>()
    private val rateLimiters = ConcurrentHashMap<String, RateLimiter>()
    
    // Platform configurations
    private val platformConfigs = ConcurrentHashMap<String, PlatformConfiguration>()
    
    // Authentication managers
    private val authManagers = ConcurrentHashMap<String, PlatformAuthManager>()
    
    // Resource monitors
    private val resourceMonitors = ConcurrentHashMap<String, ResourceMonitor>()
    
    init {
        // Initialize all supported platforms
        initializePlatforms()
    }
    
    /**
     * Register a new cloud platform
     */
    fun registerPlatform(platform: CloudPlatform) {
        Log.d(TAG, "Registering platform: ${platform.name}")
        
        platforms[platform.id] = platform
        platformConfigs[platform.id] = platform.defaultConfiguration
        rateLimiters[platform.id] = RateLimiter(
            maxRequests = platform.rateLimits.maxRequests,
            windowMs = platform.rateLimits.windowMs
        )
        
        // Initialize platform client
        platformClients[platform.id] = createPlatformClient(platform.type)
        
        // Initialize authentication manager
        authManagers[platform.id] = createAuthManager(platform.id, platform.authType)
    }
    
    /**
     * Configure a platform with custom settings
     */
    fun configurePlatform(
        platformId: String,
        configuration: PlatformConfiguration
    ): Boolean {
        return try {
            val platform = platforms[platformId]
            if (platform == null) {
                Log.w(TAG, "Platform not found: $platformId")
                return false
            }
            
            // Validate configuration
            if (!validateConfiguration(platform, configuration)) {
                Log.w(TAG, "Invalid configuration for platform: $platformId")
                return false
            }
            
            platformConfigs[platformId] = configuration
            Log.d(TAG, "Platform configured: $platformId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure platform: $platformId", e)
            false
        }
    }
    
    /**
     * Authenticate with a platform
     */
    suspend fun authenticate(
        platformId: String,
        credentials: PlatformCredentials
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Authenticating with platform: $platformId")
            
            val authManager = authManagers[platformId]
            if (authManager == null) {
                Log.w(TAG, "No auth manager for platform: $platformId")
                return@withContext false
            }
            
            val success = authManager.authenticate(credentials)
            if (success) {
                Log.d(TAG, "Authentication successful for: $platformId")
                
                // Test connection
                val client = platformClients[platformId]
                if (client != null) {
                    val connectionTest = testConnection(platformId, client)
                    Log.d(TAG, "Connection test for $platformId: ${if (connectionTest) "success" else "failed"}")
                }
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed for platform: $platformId", e)
            false
        }
    }
    
    /**
     * Get platform status
     */
    suspend fun getPlatformStatus(platformId: String): PlatformStatus = withContext(Dispatchers.IO) {
        try {
            val platform = platforms[platformId]
                ?: return@withContext PlatformStatus(platformId, STATUS_OFFLINE, "Platform not found")
            
            val client = platformClients[platformId]
            if (client == null) {
                return@withContext PlatformStatus(platformId, STATUS_OFFLINE, "Client not initialized")
            }
            
            // Check rate limits
            val rateLimiter = rateLimiters[platformId]
            if (rateLimiter?.isRateLimited() == true) {
                return@withContext PlatformStatus(platformId, STATUS_DEGRADED, "Rate limited")
            }
            
            // Get status from platform
            val status = client.getStatus()
            val lastUpdate = System.currentTimeMillis()
            
            PlatformStatus(platformId, status, "OK", lastUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get platform status: $platformId", e)
            PlatformStatus(platformId, STATUS_OFFLINE, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Create a resource on the platform
     */
    suspend fun createResource(
        platformId: String,
        resourceSpec: ResourceSpecification
    ): Result<Resource> = withContext(Dispatchers.IO) {
        try {
            val client = platformClients[platformId]
            if (client == null) {
                return@withContext Result.failure(IllegalArgumentException("Platform client not found"))
            }
            
            // Check rate limits
            val rateLimiter = rateLimiters[platformId]
            if (rateLimiter?.isRateLimited() == true) {
                return@withContext Result.failure(Exception("Rate limit exceeded"))
            }
            
            Log.d(TAG, "Creating resource on $platformId: ${resourceSpec.type}")
            
            val resource = client.createResource(resourceSpec)
            
            // Start monitoring if configured
            if (resourceSpec.monitoringEnabled) {
                startResourceMonitoring(platformId, resource.id)
            }
            
            Result.success(resource)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create resource on $platformId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deploy application to platform
     */
    suspend fun deployApplication(
        platformId: String,
        deploymentSpec: DeploymentSpecification
    ): Result<Deployment> = withContext(Dispatchers.IO) {
        try {
            val client = platformClients[platformId]
            if (client == null) {
                return@withContext Result.failure(IllegalArgumentException("Platform client not found"))
            }
            
            // Check rate limits
            val rateLimiter = rateLimiters[platformId]
            if (rateLimiter?.isRateLimited() == true) {
                return@withContext Result.failure(Exception("Rate limit exceeded"))
            }
            
            Log.d(TAG, "Deploying application to $platformId: ${deploymentSpec.name}")
            
            val deployment = client.deployApplication(deploymentSpec)
            
            // Start monitoring deployment
            startDeploymentMonitoring(platformId, deployment.id)
            
            Result.success(deployment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deploy application to $platformId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get resource usage statistics
     */
    suspend fun getResourceUsage(
        platformId: String,
        resourceId: String,
        timeRange: TimeRange = TimeRange.LAST_HOUR
    ): Result<ResourceUsage> = withContext(Dispatchers.IO) {
        try {
            val client = platformClients[platformId]
            if (client == null) {
                return@withContext Result.failure(IllegalArgumentException("Platform client not found"))
            }
            
            val usage = client.getResourceUsage(resourceId, timeRange)
            Result.success(usage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get resource usage: $platformId/$resourceId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get available resource types for a platform
     */
    fun getAvailableResources(platformId: String): List<ResourceType> {
        val platform = platforms[platformId]
        return platform?.supportedResources ?: emptyList()
    }
    
    /**
     * Estimate costs for a resource specification
     */
    fun estimateCost(
        platformId: String,
        resourceSpec: ResourceSpecification
    ): Result<CostEstimate> {
        return try {
            val platform = platforms[platformId]
            if (platform == null) {
                return@try Result.failure(IllegalArgumentException("Platform not found"))
            }
            
            val cost = calculateCost(platform, resourceSpec)
            Result.success(cost)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to estimate cost: $platformId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get platform recommendations based on requirements
     */
    fun getRecommendations(requirements: PlatformRequirements): List<PlatformRecommendation> {
        return platforms.values
            .filter { it.type in requirements.allowedPlatformTypes }
            .mapNotNull { platform ->
                val score = calculatePlatformScore(platform, requirements)
                if (score >= requirements.minimumScore) {
                    PlatformRecommendation(
                        platformId = platform.id,
                        platformName = platform.name,
                        score = score,
                        estimatedMonthlyCost = estimatePlatformCost(platform, requirements),
                        pros = getPlatformPros(platform, requirements),
                        cons = getPlatformCons(platform, requirements)
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { it.score }
    }
    
    /**
     * Get all registered platforms
     */
    fun getPlatforms(): List<CloudPlatform> {
        return platforms.values.toList()
    }
    
    /**
     * Get platform by ID
     */
    fun getPlatform(platformId: String): CloudPlatform? {
        return platforms[platformId]
    }
    
    /**
     * Remove platform
     */
    fun removePlatform(platformId: String): Boolean {
        return try {
            val removed = platforms.remove(platformId) != null
            if (removed) {
                platformConfigs.remove(platformId)
                platformClients.remove(platformId)
                rateLimiters.remove(platformId)
                authManagers.remove(platformId)
                
                // Stop monitoring
                resourceMonitors[platformId]?.stop()
                resourceMonitors.remove(platformId)
                
                Log.d(TAG, "Removed platform: $platformId")
            }
            removed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove platform: $platformId", e)
            false
        }
    }
    
    private fun initializePlatforms() {
        // Register all supported platforms
        registerPlatform(HerokuPlatform())
        registerPlatform(VercelPlatform())
        registerPlatform(RailwayPlatform())
        registerPlatform(NetlifyPlatform())
        registerPlatform(AWSPlatform())
        registerPlatform(GCPPlatform())
        registerPlatform(AzurePlatform())
        registerPlatform(DigitalOceanPlatform())
        registerPlatform(FirebasePlatform())
        registerPlatform(FlyIOWlatform())
        registerPlatform(ReplitPlatform())
        
        Log.d(TAG, "Initialized ${platforms.size} cloud platforms")
    }
    
    private fun createPlatformClient(type: String): PlatformClient {
        return when (type) {
            TYPE_PAAS -> PaaSClient()
            TYPE_IAAS -> IaaSClient()
            TYPE_FAAS -> FaaSClient()
            TYPE_SERVERLESS -> ServerlessClient()
            else -> DefaultPlatformClient()
        }
    }
    
    private fun createAuthManager(platformId: String, authType: String): PlatformAuthManager {
        return when (authType) {
            "api_key" -> ApiKeyAuthManager()
            "oauth" -> OAuthAuthManager()
            "basic" -> BasicAuthManager()
            "token" -> TokenAuthManager()
            else -> DefaultAuthManager()
        }
    }
    
    private fun validateConfiguration(
        platform: CloudPlatform,
        config: PlatformConfiguration
    ): Boolean {
        // Validate required fields
        if (config.requiredFields.any { !config.containsKey(it) }) {
            return false
        }
        
        // Validate resource limits
        config.resourceLimits.forEach { (resource, limit) ->
            if (limit > platform.maxResourceLimits[resource] ?: Long.MAX_VALUE) {
                return false
            }
        }
        
        return true
    }
    
    private suspend fun testConnection(platformId: String, client: PlatformClient): Boolean {
        return try {
            val status = client.getStatus()
            status == STATUS_ONLINE
        } catch (e: Exception) {
            false
        }
    }
    
    private fun startResourceMonitoring(platformId: String, resourceId: String) {
        val monitor = ResourceMonitor(platformId, resourceId, platformClients[platformId])
        resourceMonitors["$platformId:$resourceId"] = monitor
        monitor.start()
    }
    
    private fun startDeploymentMonitoring(platformId: String, deploymentId: String) {
        // Start monitoring deployment status
        scope.launch {
            try {
                val client = platformClients[platformId]
                if (client != null) {
                    // Monitor deployment until completion
                    while (true) {
                        val status = client.getDeploymentStatus(deploymentId)
                        if (status?.isCompleted == true) {
                            Log.d(TAG, "Deployment completed: $platformId/$deploymentId")
                            break
                        }
                        delay(5000) // Check every 5 seconds
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring deployment", e)
            }
        }
    }
    
    private fun calculateCost(
        platform: CloudPlatform,
        resourceSpec: ResourceSpecification
    ): CostEstimate {
        // Simplified cost calculation
        val baseCost = platform.pricing.baseCost
        val cpuCost = platform.pricing.cpuCostPerUnit * resourceSpec.cpuCores
        val memoryCost = platform.pricing.memoryCostPerGB * resourceSpec.memoryGB
        val storageCost = platform.pricing.storageCostPerGB * resourceSpec.storageGB
        val bandwidthCost = platform.pricing.bandwidthCostPerGB * resourceSpec.estimatedBandwidthGB
        
        val monthlyCost = baseCost + cpuCost + memoryCost + storageCost + bandwidthCost
        
        return CostEstimate(
            platformId = platform.id,
            monthlyCostUSD = monthlyCost,
            currency = "USD",
            billingPeriod = "monthly",
            breakdown = mapOf(
                "base" to baseCost,
                "cpu" to cpuCost,
                "memory" to memoryCost,
                "storage" to storageCost,
                "bandwidth" to bandwidthCost
            ),
            estimatedAnnualCost = monthlyCost * 12
        )
    }
    
    private fun calculatePlatformScore(
        platform: CloudPlatform,
        requirements: PlatformRequirements
    ): Double {
        var score = 0.0
        
        // Type match score
        if (platform.type in requirements.allowedPlatformTypes) {
            score += 20.0
        }
        
        // Resource capability score
        requirements.requiredResources.forEach { resource ->
            if (platform.supportedResources.any { it.id == resource.id }) {
                score += 10.0
            }
        }
        
        // Geographic coverage score
        val matchingRegions = platform.regions.count { it in requirements.allowedRegions }
        score += (matchingRegions.toDouble() / platform.regions.size) * 20.0
        
        // Pricing score (inverse of estimated cost)
        val costEstimate = estimatePlatformCost(platform, requirements)
        val maxCost = requirements.maxMonthlyBudgetUSD ?: Double.MAX_VALUE
        if (costEstimate <= maxCost) {
            score += 20.0 * (1.0 - (costEstimate / maxCost))
        }
        
        // Reliability score
        score += platform.reliabilityScore * 10.0
        
        // Features score
        val matchingFeatures = platform.features.count { it in requirements.requiredFeatures }
        score += (matchingFeatures.toDouble() / requirements.requiredFeatures.size) * 10.0
        
        return minOf(score, 100.0)
    }
    
    private fun estimatePlatformCost(
        platform: CloudPlatform,
        requirements: PlatformRequirements
    ): Double {
        // Simple estimation based on required resources
        val baseCost = platform.pricing.baseCost
        val resourceCost = requirements.requiredResources.sumOf { resource ->
            val unitCost = platform.pricing.cpuCostPerUnit * (resource.cpuCores ?: 0) +
                    platform.pricing.memoryCostPerGB * (resource.memoryGB ?: 0) +
                    platform.pricing.storageCostPerGB * (resource.storageGB ?: 0)
            unitCost
        }
        
        return baseCost + resourceCost
    }
    
    private fun getPlatformPros(
        platform: CloudPlatform,
        requirements: PlatformRequirements
    ): List<String> {
        val pros = mutableListOf<String>()
        
        if (platform.type == TYPE_PAAS && "easy_deployment" in requirements.requiredFeatures) {
            pros.add("Easy deployment with no infrastructure management")
        }
        
        if (platform.reliabilityScore > 0.9) {
            pros.add("High reliability (${(platform.reliabilityScore * 100).toInt()}%)")
        }
        
        if (platform.regions.size > 10) {
            pros.add("Wide geographic coverage (${platform.regions.size} regions)")
        }
        
        if (platform.pricing.baseCost < 10.0) {
            pros.add("Low base cost ($${platform.pricing.baseCost}/month)")
        }
        
        return pros
    }
    
    private fun getPlatformCons(
        platform: CloudPlatform,
        requirements: PlatformRequirements
    ): List<String> {
        val cons = mutableListOf<String>()
        
        if (platform.pricing.baseCost > 50.0) {
            cons.add("High base cost ($${platform.pricing.baseCost}/month)")
        }
        
        if (platform.reliabilityScore < 0.8) {
            cons.add("Lower reliability (${(platform.reliabilityScore * 100).toInt()}%)")
        }
        
        if (platform.regions.size < 5) {
            cons.add("Limited geographic coverage")
        }
        
        return cons
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        
        // Stop all resource monitors
        resourceMonitors.values.forEach { it.stop() }
        resourceMonitors.clear()
        
        platforms.clear()
        platformClients.clear()
        rateLimiters.clear()
        platformConfigs.clear()
        authManagers.clear()
        
        Log.d(TAG, "Cloud platforms manager cleaned up")
    }
}

// Platform data classes
@Serializable
data class CloudPlatform(
    val id: String,
    val name: String,
    val type: String,
    val authType: String,
    val regions: List<String>,
    val supportedResources: List<ResourceType>,
    val features: List<String>,
    val pricing: PricingModel,
    val defaultConfiguration: PlatformConfiguration,
    val maxResourceLimits: Map<String, Long>,
    val rateLimits: RateLimit,
    val reliabilityScore: Double
)

@Serializable
data class PlatformConfiguration(
    val region: String = "us-east-1",
    val instanceType: String = "basic",
    val autoScaling: Boolean = false,
    val backupEnabled: Boolean = true,
    val monitoringEnabled: Boolean = true,
    val resourceLimits: Map<String, Long> = emptyMap(),
    val environmentVariables: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap()
) {
    fun containsKey(field: String): Boolean = when (field) {
        "region" -> region.isNotEmpty()
        "instanceType" -> instanceType.isNotEmpty()
        else -> resourceLimits.containsKey(field) || environmentVariables.containsKey(field)
    }
}

@Serializable
data class PlatformCredentials(
    val apiKey: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val username: String? = null,
    val password: String? = null,
    val tenantId: String? = null,
    val subscriptionId: String? = null
)

@Serializable
data class PlatformStatus(
    val platformId: String,
    val status: String,
    val message: String,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Serializable
data class PlatformRequirements(
    val allowedPlatformTypes: List<String>,
    val requiredResources: List<ResourceRequirement>,
    val allowedRegions: List<String> = emptyList(),
    val requiredFeatures: List<String> = emptyList(),
    val maxMonthlyBudgetUSD: Double? = null,
    val minMonthlyBudgetUSD: Double? = null,
    val minimumScore: Double = 50.0
)

@Serializable
data class PlatformRecommendation(
    val platformId: String,
    val platformName: String,
    val score: Double,
    val estimatedMonthlyCost: Double,
    val pros: List<String>,
    val cons: List<String>
)

// Resource data classes
@Serializable
data class ResourceSpecification(
    val type: String,
    val name: String,
    val cpuCores: Int = 1,
    val memoryGB: Int = 1,
    val storageGB: Int = 10,
    val estimatedBandwidthGB: Long = 0,
    val region: String = "us-east-1",
    val autoScaling: Boolean = false,
    val monitoringEnabled: Boolean = true
)

@Serializable
data class Resource(
    val id: String,
    val platformId: String,
    val type: String,
    val name: String,
    val status: String,
    val createdAt: Long,
    val configuration: ResourceSpecification
)

@Serializable
data class ResourceType(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val minSpecs: ResourceSpecification,
    val maxSpecs: ResourceSpecification,
    val pricing: PricingModel
)

@Serializable
data class ResourceRequirement(
    val type: String,
    val minCpuCores: Int = 1,
    val minMemoryGB: Int = 1,
    val minStorageGB: Int = 10,
    val cpuCores: Int? = null,
    val memoryGB: Int? = null,
    val storageGB: Int? = null
)

@Serializable
data class ResourceUsage(
    val resourceId: String,
    val timestamp: Long,
    val cpuUsage: Double,
    val memoryUsage: Double,
    val networkIn: Long,
    val networkOut: Long,
    val diskUsage: Double
)

@Serializable
data class TimeRange(
    val start: Long,
    val end: Long,
    val interval: String
) {
    companion object {
        val LAST_HOUR = TimeRange(
            start = System.currentTimeMillis() - 3600000L,
            end = System.currentTimeMillis(),
            interval = "5m"
        )
        val LAST_24_HOURS = TimeRange(
            start = System.currentTimeMillis() - 86400000L,
            end = System.currentTimeMillis(),
            interval = "1h"
        )
        val LAST_7_DAYS = TimeRange(
            start = System.currentTimeMillis() - 604800000L,
            end = System.currentTimeMillis(),
            interval = "1d"
        )
    }
}

// Deployment data classes
@Serializable
data class DeploymentSpecification(
    val name: String,
    val applicationType: String,
    val sourceUrl: String,
    val buildCommand: String? = null,
    val startCommand: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
    val domain: String? = null,
    val sslEnabled: Boolean = true,
    val autoScaling: Boolean = false
)

@Serializable
data class Deployment(
    val id: String,
    val platformId: String,
    val name: String,
    val status: String,
    val createdAt: Long,
    val url: String? = null,
    val configuration: DeploymentSpecification
)

// Pricing and billing
@Serializable
data class PricingModel(
    val baseCost: Double,
    val cpuCostPerUnit: Double,
    val memoryCostPerGB: Double,
    val storageCostPerGB: Double,
    val bandwidthCostPerGB: Double,
    val currency: String = "USD"
)

@Serializable
data class CostEstimate(
    val platformId: String,
    val monthlyCostUSD: Double,
    val currency: String,
    val billingPeriod: String,
    val breakdown: Map<String, Double>,
    val estimatedAnnualCost: Double
)

// Rate limiting and configuration
@Serializable
data class RateLimit(
    val maxRequests: Int,
    val windowMs: Long
)

data class RateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long,
    private val requestTimes: MutableList<Long> = mutableListOf()
) {
    fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        val cutoff = now - windowMs
        
        // Remove old requests
        requestTimes.removeAll { it < cutoff }
        
        // Check if we've exceeded the limit
        return requestTimes.size >= maxRequests
    }
    
    fun recordRequest() {
        requestTimes.add(System.currentTimeMillis())
    }
}

// Interface for platform clients
interface PlatformClient {
    suspend fun getStatus(): String
    suspend fun createResource(spec: ResourceSpecification): Resource
    suspend fun deployApplication(spec: DeploymentSpecification): Deployment
    suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage
    suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus?
}

@Serializable
data class DeploymentStatus(
    val deploymentId: String,
    val status: String,
    val progress: Double = 0.0,
    val isCompleted: Boolean = false,
    val logs: List<String> = emptyList()
)

// Authentication managers
interface PlatformAuthManager {
    suspend fun authenticate(credentials: PlatformCredentials): Boolean
    suspend fun refreshToken(): Boolean
    fun isAuthenticated(): Boolean
    fun getAuthToken(): String?
}

class ApiKeyAuthManager : PlatformAuthManager {
    private var apiKey: String? = null
    
    override suspend fun authenticate(credentials: PlatformCredentials): Boolean {
        apiKey = credentials.apiKey
        return !apiKey.isNullOrEmpty()
    }
    
    override suspend fun refreshToken(): Boolean = true
    override fun isAuthenticated(): Boolean = !apiKey.isNullOrEmpty()
    override fun getAuthToken(): String? = apiKey
}

class OAuthAuthManager : PlatformAuthManager {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    override suspend fun authenticate(credentials: PlatformCredentials): Boolean {
        accessToken = credentials.accessToken
        refreshToken = credentials.refreshToken
        return !accessToken.isNullOrEmpty()
    }
    
    override suspend fun refreshToken(): Boolean {
        // Implement token refresh logic
        return false
    }
    
    override fun isAuthenticated(): Boolean = !accessToken.isNullOrEmpty()
    override fun getAuthToken(): String? = accessToken
}

class BasicAuthManager : PlatformAuthManager {
    private var username: String? = null
    private var password: String? = null
    
    override suspend fun authenticate(credentials: PlatformCredentials): Boolean {
        username = credentials.username
        password = credentials.password
        return !username.isNullOrEmpty() && !password.isNullOrEmpty()
    }
    
    override suspend fun refreshToken(): Boolean = true
    override fun isAuthenticated(): Boolean = !username.isNullOrEmpty() && !password.isNullOrEmpty()
    override fun getAuthToken(): String? = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"
}

class TokenAuthManager : PlatformAuthManager {
    private var token: String? = null
    
    override suspend fun authenticate(credentials: PlatformCredentials): Boolean {
        token = credentials.accessToken
        return !token.isNullOrEmpty()
    }
    
    override suspend fun refreshToken(): Boolean = true
    override fun isAuthenticated(): Boolean = !token.isNullOrEmpty()
    override fun getAuthToken(): String? = token
}

class DefaultAuthManager : PlatformAuthManager {
    override suspend fun authenticate(credentials: PlatformCredentials): Boolean = true
    override suspend fun refreshToken(): Boolean = true
    override fun isAuthenticated(): Boolean = true
    override fun getAuthToken(): String? = null
}

// Resource monitoring
class ResourceMonitor(
    private val platformId: String,
    private val resourceId: String,
    private val client: PlatformClient?
) {
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun start() {
        if (isRunning) return
        
        isRunning = true
        scope.launch {
            while (isRunning) {
                try {
                    // Monitor resource usage
                    val usage = client?.getResourceUsage(resourceId, TimeRange.LAST_HOUR)
                    // Handle usage data
                    
                    delay(30000) // Monitor every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring resource: $platformId/$resourceId", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
    }
    
    fun stop() {
        isRunning = false
        scope.cancel()
    }
}

// Platform client implementations
class PaaSClient : PlatformClient {
    override suspend fun getStatus(): String = STATUS_ONLINE
    override suspend fun createResource(spec: ResourceSpecification): Resource = Resource(
        id = "mock_paas_${System.currentTimeMillis()}",
        platformId = "paas",
        type = spec.type,
        name = spec.name,
        status = "active",
        createdAt = System.currentTimeMillis(),
        configuration = spec
    )
    override suspend fun deployApplication(spec: DeploymentSpecification): Deployment = Deployment(
        id = "mock_deploy_${System.currentTimeMillis()}",
        platformId = "paas",
        name = spec.name,
        status = "running",
        createdAt = System.currentTimeMillis(),
        url = "https://${spec.name}.example.com",
        configuration = spec
    )
    override suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage = ResourceUsage(
        resourceId = resourceId,
        timestamp = System.currentTimeMillis(),
        cpuUsage = 0.5,
        memoryUsage = 0.3,
        networkIn = 1024L,
        networkOut = 2048L,
        diskUsage = 0.2
    )
    override suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus? = DeploymentStatus(
        deploymentId = deploymentId,
        status = "completed",
        progress = 1.0,
        isCompleted = true
    )
}

class IaaSClient : PlatformClient {
    override suspend fun getStatus(): String = STATUS_ONLINE
    override suspend fun createResource(spec: ResourceSpecification): Resource = Resource(
        id = "mock_iaas_${System.currentTimeMillis()}",
        platformId = "iaas",
        type = spec.type,
        name = spec.name,
        status = "active",
        createdAt = System.currentTimeMillis(),
        configuration = spec
    )
    override suspend fun deployApplication(spec: DeploymentSpecification): Deployment = Deployment(
        id = "mock_deploy_${System.currentTimeMillis()}",
        platformId = "iaas",
        name = spec.name,
        status = "running",
        createdAt = System.currentTimeMillis(),
        url = "https://${spec.name}.example.com",
        configuration = spec
    )
    override suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage = ResourceUsage(
        resourceId = resourceId,
        timestamp = System.currentTimeMillis(),
        cpuUsage = 0.7,
        memoryUsage = 0.6,
        networkIn = 2048L,
        networkOut = 4096L,
        diskUsage = 0.4
    )
    override suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus? = DeploymentStatus(
        deploymentId = deploymentId,
        status = "completed",
        progress = 1.0,
        isCompleted = true
    )
}

class FaaSClient : PlatformClient {
    override suspend fun getStatus(): String = STATUS_ONLINE
    override suspend fun createResource(spec: ResourceSpecification): Resource = Resource(
        id = "mock_faas_${System.currentTimeMillis()}",
        platformId = "faas",
        type = spec.type,
        name = spec.name,
        status = "active",
        createdAt = System.currentTimeMillis(),
        configuration = spec
    )
    override suspend fun deployApplication(spec: DeploymentSpecification): Deployment = Deployment(
        id = "mock_deploy_${System.currentTimeMillis()}",
        platformId = "faas",
        name = spec.name,
        status = "running",
        createdAt = System.currentTimeMillis(),
        url = "https://${spec.name}.example.com",
        configuration = spec
    )
    override suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage = ResourceUsage(
        resourceId = resourceId,
        timestamp = System.currentTimeMillis(),
        cpuUsage = 0.1,
        memoryUsage = 0.2,
        networkIn = 512L,
        networkOut = 1024L,
        diskUsage = 0.1
    )
    override suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus? = DeploymentStatus(
        deploymentId = deploymentId,
        status = "completed",
        progress = 1.0,
        isCompleted = true
    )
}

class ServerlessClient : PlatformClient {
    override suspend fun getStatus(): String = STATUS_ONLINE
    override suspend fun createResource(spec: ResourceSpecification): Resource = Resource(
        id = "mock_serverless_${System.currentTimeMillis()}",
        platformId = "serverless",
        type = spec.type,
        name = spec.name,
        status = "active",
        createdAt = System.currentTimeMillis(),
        configuration = spec
    )
    override suspend fun deployApplication(spec: DeploymentSpecification): Deployment = Deployment(
        id = "mock_deploy_${System.currentTimeMillis()}",
        platformId = "serverless",
        name = spec.name,
        status = "running",
        createdAt = System.currentTimeMillis(),
        url = "https://${spec.name}.example.com",
        configuration = spec
    )
    override suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage = ResourceUsage(
        resourceId = resourceId,
        timestamp = System.currentTimeMillis(),
        cpuUsage = 0.0,
        memoryUsage = 0.0,
        networkIn = 256L,
        networkOut = 512L,
        diskUsage = 0.0
    )
    override suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus? = DeploymentStatus(
        deploymentId = deploymentId,
        status = "completed",
        progress = 1.0,
        isCompleted = true
    )
}

class DefaultPlatformClient : PlatformClient {
    override suspend fun getStatus(): String = STATUS_ONLINE
    override suspend fun createResource(spec: ResourceSpecification): Resource = Resource(
        id = "mock_default_${System.currentTimeMillis()}",
        platformId = "default",
        type = spec.type,
        name = spec.name,
        status = "active",
        createdAt = System.currentTimeMillis(),
        configuration = spec
    )
    override suspend fun deployApplication(spec: DeploymentSpecification): Deployment = Deployment(
        id = "mock_deploy_${System.currentTimeMillis()}",
        platformId = "default",
        name = spec.name,
        status = "running",
        createdAt = System.currentTimeMillis(),
        url = "https://${spec.name}.example.com",
        configuration = spec
    )
    override suspend fun getResourceUsage(resourceId: String, timeRange: TimeRange): ResourceUsage = ResourceUsage(
        resourceId = resourceId,
        timestamp = System.currentTimeMillis(),
        cpuUsage = 0.3,
        memoryUsage = 0.4,
        networkIn = 1024L,
        networkOut = 2048L,
        diskUsage = 0.3
    )
    override suspend fun getDeploymentStatus(deploymentId: String): DeploymentStatus? = DeploymentStatus(
        deploymentId = deploymentId,
        status = "completed",
        progress = 1.0,
        isCompleted = true
    )
}

// Specific platform implementations
class HerokuPlatform : CloudPlatform(
    id = "heroku",
    name = "Heroku",
    type = TYPE_PAAS,
    authType = "api_key",
    regions = listOf("us-east-1", "eu-west-1"),
    supportedResources = listOf(
        ResourceType("web", "Web Dyno", "compute", "Web application container", 
            ResourceSpecification("web", "basic-1x"), ResourceSpecification("web", "performance-l"),
            PricingModel(0.0, 0.0, 0.0, 0.0, 0.0)),
        ResourceType("worker", "Worker Dyno", "compute", "Background worker process",
            ResourceSpecification("worker", "basic-1x"), ResourceSpecification("worker", "performance-l"),
            PricingModel(0.0, 0.0, 0.0, 0.0, 0.0))
    ),
    features = listOf("easy_deployment", "auto_scaling", "add_ons", "git_deployment"),
    pricing = PricingModel(0.0, 0.0, 0.0, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "us-east-1"),
    maxResourceLimits = mapOf("cpu" to 8, "memory" to 8, "storage" to 500),
    rateLimits = RateLimit(100, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.95
)

class VercelPlatform : CloudPlatform(
    id = "vercel",
    name = "Vercel",
    type = TYPE_SERVERLESS,
    authType = "token",
    regions = listOf("us-west-2", "us-east-1", "eu-west-1", "ap-northeast-1"),
    supportedResources = listOf(
        ResourceType("serverless", "Serverless Function", "compute", "Serverless compute function",
            ResourceSpecification("serverless", "basic"), ResourceSpecification("serverless", "large"),
            PricingModel(0.0, 0.0, 0.0, 0.0, 0.0))
    ),
    features = listOf("serverless", "edge_network", "cdn", "git_integration"),
    pricing = PricingModel(0.0, 0.0, 0.0, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "us-west-2"),
    maxResourceLimits = mapOf("cpu" to 2, "memory" to 2, "storage" to 100),
    rateLimits = RateLimit(1000, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.98
)

class RailwayPlatform : CloudPlatform(
    id = "railway",
    name = "Railway",
    type = TYPE_PAAS,
    authType = "token",
    regions = listOf("us-west-1", "us-east-1", "eu-west-1"),
    supportedResources = listOf(
        ResourceType("web", "Web Service", "compute", "Web application service",
            ResourceSpecification("web", "basic"), ResourceSpecification("web", "large"),
            PricingModel(5.0, 0.01, 0.001, 0.1, 0.09)),
        ResourceType("worker", "Worker Service", "compute", "Background worker service",
            ResourceSpecification("worker", "basic"), ResourceSpecification("worker", "large"),
            PricingModel(5.0, 0.01, 0.001, 0.1, 0.09))
    ),
    features = listOf("docker_deployment", "auto_scaling", "database", "redis"),
    pricing = PricingModel(5.0, 0.01, 0.001, 0.1, 0.09),
    defaultConfiguration = PlatformConfiguration(region = "us-west-1"),
    maxResourceLimits = mapOf("cpu" to 8, "memory" to 16, "storage" to 1000),
    rateLimits = RateLimit(200, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.92
)

class NetlifyPlatform : CloudPlatform(
    id = "netlify",
    name = "Netlify",
    type = TYPE_STATIC,
    authType = "token",
    regions = listOf("us-west-1", "us-east-1", "eu-west-1"),
    supportedResources = listOf(
        ResourceType("static", "Static Site", "hosting", "Static website hosting",
            ResourceSpecification("static", "starter"), ResourceSpecification("static", "enterprise"),
            PricingModel(19.0, 0.0, 0.0, 0.0, 0.0))
    ),
    features = listOf("static_hosting", "cdn", "form_handling", "functions"),
    pricing = PricingModel(19.0, 0.0, 0.0, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "us-west-1"),
    maxResourceLimits = mapOf("bandwidth" to 10000),
    rateLimits = RateLimit(500, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.97
)

class AWSPlatform : CloudPlatform(
    id = "aws",
    name = "Amazon Web Services",
    type = TYPE_IAAS,
    authType = "token",
    regions = listOf("us-east-1", "us-west-1", "us-west-2", "eu-west-1", "eu-central-1", "ap-southeast-1"),
    supportedResources = listOf(
        ResourceType("ec2", "EC2 Instance", "compute", "Virtual server instance",
            ResourceSpecification("ec2", "t3.micro"), ResourceSpecification("ec2", "m5.xlarge"),
            PricingModel(8.0, 0.096, 0.0045, 0.1, 0.09)),
        ResourceType("lambda", "Lambda Function", "faas", "Serverless compute function",
            ResourceSpecification("lambda", "128MB"), ResourceSpecification("lambda", "3GB"),
            PricingModel(0.0, 0.0000166667, 0.0000025, 0.0, 0.0))
    ),
    features = listOf("vm", "serverless", "storage", "database", "cdn", "monitoring"),
    pricing = PricingModel(8.0, 0.096, 0.0045, 0.1, 0.09),
    defaultConfiguration = PlatformConfiguration(region = "us-east-1"),
    maxResourceLimits = mapOf("cpu" to 128, "memory" to 488, "storage" to 20000),
    rateLimits = RateLimit(1000, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.99
)

class GCPPlatform : CloudPlatform(
    id = "gcp",
    name = "Google Cloud Platform",
    type = TYPE_IAAS,
    authType = "token",
    regions = listOf("us-central1", "us-east1", "us-west1", "europe-west1", "asia-east1"),
    supportedResources = listOf(
        ResourceType("compute", "Compute Engine", "compute", "Virtual machine instance",
            ResourceSpecification("compute", "micro"), ResourceSpecification("compute", "highmem"),
            PricingModel(10.0, 0.0375, 0.005, 0.12, 0.12)),
        ResourceType("cloud_run", "Cloud Run", "serverless", "Serverless container platform",
            ResourceSpecification("cloud_run", "256MB"), ResourceSpecification("cloud_run", "2GB"),
            PricingModel(0.0, 0.000024, 0.0000025, 0.0, 0.0))
    ),
    features = listOf("vm", "serverless", "storage", "database", "ai_ml", "monitoring"),
    pricing = PricingModel(10.0, 0.0375, 0.005, 0.12, 0.12),
    defaultConfiguration = PlatformConfiguration(region = "us-central1"),
    maxResourceLimits = mapOf("cpu" to 128, "memory" to 416, "storage" to 10000),
    rateLimits = RateLimit(1000, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.98
)

class AzurePlatform : CloudPlatform(
    id = "azure",
    name = "Microsoft Azure",
    type = TYPE_IAAS,
    authType = "oauth",
    regions = listOf("eastus", "westus", "westeurope", "southeastasia"),
    supportedResources = listOf(
        ResourceType("vm", "Virtual Machine", "compute", "Virtual machine instance",
            ResourceSpecification("vm", "basic-a0"), ResourceSpecification("vm", "standard-d4s-v3"),
            PricingModel(10.0, 0.095, 0.005, 0.12, 0.12)),
        ResourceType("function", "Azure Functions", "faas", "Serverless compute functions",
            ResourceSpecification("function", "consumption"), ResourceSpecification("function", "premium"),
            PricingModel(0.0, 0.000016, 0.000002, 0.0, 0.0))
    ),
    features = listOf("vm", "serverless", "storage", "database", "ai", "monitoring"),
    pricing = PricingModel(10.0, 0.095, 0.005, 0.12, 0.12),
    defaultConfiguration = PlatformConfiguration(region = "eastus"),
    maxResourceLimits = mapOf("cpu" to 128, "memory" to 416, "storage" to 10000),
    rateLimits = RateLimit(1000, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.96
)

class DigitalOceanPlatform : CloudPlatform(
    id = "digitalocean",
    name = "DigitalOcean",
    type = TYPE_IAAS,
    authType = "token",
    regions = listOf("nyc1", "sfo1", "ams1", "sgp1"),
    supportedResources = listOf(
        ResourceType("droplet", "Droplet", "compute", "Virtual machine instance",
            ResourceSpecification("droplet", "basic-xs"), ResourceSpecification("droplet", "m-2vcpu-8gb"),
            PricingModel(6.0, 0.012, 0.0015, 0.1, 0.01)),
        ResourceType("app", "App Platform", "paas", "Managed application platform",
            ResourceSpecification("app", "starter"), ResourceSpecification("app", "professional"),
            PricingModel(5.0, 0.0, 0.0, 0.0, 0.0))
    ),
    features = listOf("vm", "managed_database", "cdn", "monitoring"),
    pricing = PricingModel(6.0, 0.012, 0.0015, 0.1, 0.01),
    defaultConfiguration = PlatformConfiguration(region = "nyc1"),
    maxResourceLimits = mapOf("cpu" to 32, "memory" to 128, "storage" to 5000),
    rateLimits = RateLimit(500, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.93
)

class FirebasePlatform : CloudPlatform(
    id = "firebase",
    name = "Firebase",
    type = TYPE_SAAS,
    authType = "token",
    regions = listOf("us-central1", "us-east1", "europe-west1"),
    supportedResources = listOf(
        ResourceType("hosting", "Hosting", "hosting", "Static website hosting",
            ResourceSpecification("hosting", "spark"), ResourceSpecification("hosting", "blaze"),
            PricingModel(0.0, 0.0, 0.0, 0.0, 0.0)),
        ResourceType("functions", "Cloud Functions", "faas", "Serverless compute functions",
            ResourceSpecification("functions", "gen1"), ResourceSpecification("functions", "gen2"),
            PricingModel(0.0, 0.0000025, 0.0000025, 0.0, 0.0))
    ),
    features = listOf("hosting", "functions", "database", "auth", "analytics"),
    pricing = PricingModel(0.0, 0.0000025, 0.0000025, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "us-central1"),
    maxResourceLimits = mapOf("cpu" to 4, "memory" to 8, "storage" in listOf("hosting", "functions")),
    rateLimits = RateLimit(1000, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.94
)

class FlyIOWlatform : CloudPlatform(
    id = "flyio",
    name = "Fly.io",
    type = TYPE_CONTAINER,
    authType = "token",
    regions = listOf("ord", "sfo", "cdg", "sin", "fra"),
    supportedResources = listOf(
        ResourceType("app", "Fly App", "container", "Containerized application",
            ResourceSpecification("app", "shared-cpu-1x"), ResourceSpecification("app", "shared-cpu-4x"),
            PricingModel(0.0, 0.000003, 0.000001, 0.0, 0.0))
    ),
    features = listOf("containers", "edge_network", "auto_scaling"),
    pricing = PricingModel(0.0, 0.000003, 0.000001, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "ord"),
    maxResourceLimits = mapOf("cpu" to 16, "memory" to 32, "storage" in listOf("app")),
    rateLimits = RateLimit(500, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.91
)

class ReplitPlatform : CloudPlatform(
    id = "replit",
    name = "Replit",
    type = TYPE_PAAS,
    authType = "token",
    regions = listOf("us-west1", "us-central1", "europe-west1"),
    supportedResources = listOf(
        ResourceType("repl", "Replit", "compute", "Online coding environment",
            ResourceSpecification("repl", "always-on"), ResourceSpecification("repl", "core"),
            PricingModel(7.0, 0.0, 0.0, 0.0, 0.0))
    ),
    features = listOf("online_ide", "collaboration", "version_control"),
    pricing = PricingModel(7.0, 0.0, 0.0, 0.0, 0.0),
    defaultConfiguration = PlatformConfiguration(region = "us-west1"),
    maxResourceLimits = mapOf("memory" to 2, "storage" in listOf("repl")),
    rateLimits = RateLimit(100, RATE_LIMIT_WINDOW),
    reliabilityScore = 0.88
)