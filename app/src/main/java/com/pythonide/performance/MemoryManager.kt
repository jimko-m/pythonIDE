package com.pythonide.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Advanced memory management system for Android applications
 * Provides memory monitoring, optimization, garbage collection management, and leak detection
 */
class MemoryManager(private val context: Context) {
    companion object {
        private const val TAG = "MemoryManager"
        private const val MEMORY_CHECK_INTERVAL = 5000L // 5 seconds
        private const val GC_TRIGGER_THRESHOLD = 75 // 75% memory usage
        private const val CRITICAL_THRESHOLD = 85 // 85% memory usage
        private const val HEAP_UTILIZATION_TARGET = 70 // Target 70% heap utilization
        private const val MAX_CACHE_SIZE = 50 // Maximum cached objects
        private const val LEAK_DETECTION_INTERVAL = 30000L // 30 seconds
    }

    private val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Memory monitoring
    private val _currentMemoryUsage = MutableStateFlow(MemoryUsage(0, 0, 0, 0, 0))
    val currentMemoryUsage: StateFlow<MemoryUsage> = _currentMemoryUsage.asStateFlow()
    
    private val _memoryTrends = MutableStateFlow<List<MemorySnapshot>>(emptyList())
    val memoryTrends: StateFlow<List<MemorySnapshot>> = _memoryTrends.asStateFlow()
    
    private val _isLowMemory = MutableStateFlow(false)
    val isLowMemory: StateFlow<Boolean> = _isLowMemory.asStateFlow()
    
    private val _memoryLeaks = MutableStateFlow<List<MemoryLeak>>(emptyList())
    val memoryLeaks: StateFlow<List<MemoryLeak>> = _memoryLeaks.asStateFlow()
    
    private var observers = mutableListOf<MemoryObserver>()
    
    // Memory cache system
    private val memoryCache = LruCache<String, Any>(MAX_CACHE_SIZE)
    
    // Reference tracking for leak detection
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    private val strongReferences = mutableMapOf<String, Any>()
    
    // Memory statistics
    private val memoryStats = MemoryStatistics()
    
    // Garbage collection management
    private var lastGCTime = System.currentTimeMillis()
    private var gcThreshold = GC_TRIGGER_THRESHOLD
    
    /**
     * Memory usage data class
     */
    data class MemoryUsage(
        val usedHeap: Long,
        val maxHeap: Long,
        val nativeHeap: Long,
        val usedMemoryPercent: Int,
        val availableProcessors: Int
    )
    
    /**
     * Memory snapshot for trending
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val usedHeap: Long,
        val maxHeap: Long,
        val nativeHeap: Long,
        val gcCount: Long,
        val processMemoryInfo: ActivityManager.MemoryInfo?
    )
    
    /**
     * Memory leak detection
     */
    data class MemoryLeak(
        val objectName: String,
        val leakType: LeakType,
        val size: Long,
        val description: String,
        val stackTrace: String? = null
    )
    
    /**
     * Memory statistics
     */
    data class MemoryStatistics(
        var totalGCCount: AtomicLong = AtomicLong(0),
        var totalGCTime: AtomicLong = AtomicLong(0),
        var peakMemoryUsage: Long = 0,
        var averageMemoryUsage: Long = 0,
        var memoryPressureEvents: AtomicLong = AtomicLong(0),
        var cacheHitRate: AtomicLong = AtomicLong(0),
        var cacheMissRate: AtomicLong = AtomicLong(0)
    )
    
    /**
     * Memory leak types
     */
    enum class LeakType {
        CONTEXT_LEAK, ACTIVITY_LEAK, HANDLER_LEAK, LISTENER_LEAK, RESOURCE_LEAK, UNKNOWN
    }
    
    /**
     * Memory observer interface
     */
    interface MemoryObserver {
        fun onMemoryUsageChanged(usage: MemoryUsage)
        fun onLowMemoryDetected(usage: MemoryUsage)
        fun onGarbageCollectionTriggered(reason: String)
        fun onMemoryLeakDetected(leak: MemoryLeak)
        fun onMemoryOptimizationCompleted(optimizedBytes: Long)
        fun onCacheSizeChanged(size: Int, capacity: Int)
    }

    /**
     * Start memory monitoring
     */
    fun startMonitoring() {
        Log.d(TAG, "Starting memory monitoring")
        
        backgroundScope.launch {
            memoryMonitoringLoop()
        }
        
        backgroundScope.launch {
            leakDetectionLoop()
        }
    }

    /**
     * Stop memory monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping memory monitoring")
        backgroundScope.cancel()
        clearMemoryCache()
        weakReferences.clear()
        strongReferences.clear()
    }

    /**
     * Get current memory usage
     */
    fun getCurrentMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        val usedMemoryPercent = ((usedHeap * 100) / maxHeap).toInt()
        val availableProcessors = runtime.availableProcessors()
        
        return MemoryUsage(
            usedHeap = usedHeap,
            maxHeap = maxHeap,
            nativeHeap = nativeHeap,
            usedMemoryPercent = usedMemoryPercent,
            availableProcessors = availableProcessors
        )
    }

    /**
     * Force garbage collection
     */
    @JvmOverloads
    fun triggerGarbageCollection(reason: String = "Manual trigger"): Boolean {
        val duration = measureTimeMillis {
            Log.d(TAG, "Triggering garbage collection: $reason")
            
            // Record GC statistics
            val beforeGC = System.nanoTime()
            System.gc()
            System.runFinalization()
            val afterGC = System.nanoTime()
            
            memoryStats.totalGCCount.incrementAndGet()
            memoryStats.totalGCTime.addAndGet(afterGC - beforeGC)
            lastGCTime = System.currentTimeMillis()
            
            // Clear weak references
            clearStaleReferences()
        }
        
        Log.d(TAG, "Garbage collection completed in ${duration}ms")
        notifyGarbageCollectionTriggered(reason)
        return true
    }

    /**
     * Optimize memory usage
     */
    fun optimizeMemory(): Long {
        val beforeMemory = getCurrentMemoryUsage().usedHeap
        var optimizedBytes = 0L
        
        Log.d(TAG, "Starting memory optimization")
        
        try {
            // Clear cache
            optimizedBytes += clearMemoryCache()
            
            // Clear stale references
            optimizedBytes += clearStaleReferences()
            
            // Run garbage collection
            triggerGarbageCollection("Memory optimization")
            
            // Get memory after optimization
            val afterMemory = getCurrentMemoryUsage().usedHeap
            optimizedBytes = beforeMemory - afterMemory
            
            Log.d(TAG, "Memory optimization completed, freed: ${optimizedBytes} bytes")
            
            // Notify observers
            notifyMemoryOptimizationCompleted(optimizedBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory optimization", e)
        }
        
        return optimizedBytes
    }

    /**
     * Add object to memory cache
     */
    @JvmOverloads
    fun cacheObject(key: String, value: Any, strongReference: Boolean = false): Boolean {
        try {
            memoryCache.put(key, value)
            
            if (strongReference) {
                strongReferences[key] = value
            } else {
                weakReferences[key] = WeakReference(value)
            }
            
            notifyCacheSizeChanged(memoryCache.size(), memoryCache.maxSize())
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error caching object: $key", e)
            return false
        }
    }

    /**
     * Get object from memory cache
     */
    fun getCachedObject(key: String): Any? {
        return try {
            val value = memoryCache.get(key)
            if (value != null) {
                memoryStats.cacheHitRate.incrementAndGet()
            } else {
                memoryStats.cacheMissRate.incrementAndGet()
            }
            value
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached object: $key", e)
            null
        }
    }

    /**
     * Remove object from memory cache
     */
    fun removeCachedObject(key: String): Boolean {
        return try {
            memoryCache.remove(key)
            weakReferences.remove(key)
            strongReferences.remove(key)
            notifyCacheSizeChanged(memoryCache.size(), memoryCache.maxSize())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cached object: $key", e)
            false
        }
    }

    /**
     * Clear all cached objects
     */
    fun clearMemoryCache(): Long {
        val cacheSize = memoryCache.size()
        if (cacheSize == 0) return 0L
        
        val cacheSizeInBytes = cacheSize * 1024L // Estimate
        memoryCache.evictAll()
        
        Log.d(TAG, "Memory cache cleared, removed $cacheSize objects")
        notifyCacheSizeChanged(0, memoryCache.maxSize())
        
        return cacheSizeInBytes
    }

    /**
     * Get memory statistics
     */
    fun getMemoryStatistics(): Map<String, Any> {
        val usage = getCurrentMemoryUsage()
        val cacheHitRate = if (memoryStats.cacheHitRate.get() + memoryStats.cacheMissRate.get() > 0) {
            (memoryStats.cacheHitRate.get().toFloat() / 
             (memoryStats.cacheHitRate.get() + memoryStats.cacheMissRate.get())).toFloat()
        } else 0f
        
        return mapOf(
            "currentUsage" to usage,
            "gcCount" to memoryStats.totalGCCount.get(),
            "gcTimeMs" to memoryStats.totalGCTime.get() / 1_000_000,
            "peakMemoryUsage" to memoryStats.peakMemoryUsage,
            "averageMemoryUsage" to memoryStats.averageMemoryUsage,
            "memoryPressureEvents" to memoryStats.memoryPressureEvents.get(),
            "cacheHitRate" to cacheHitRate,
            "cacheSize" to memoryCache.size(),
            "activeReferences" to weakReferences.size + strongReferences.size
        )
    }

    /**
     * Detect memory leaks
     */
    fun detectMemoryLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        
        try {
            // Check for context leaks
            leaks.addAll(detectContextLeaks())
            
            // Check for handler leaks
            leaks.addAll(detectHandlerLeaks())
            
            // Check for listener leaks
            leaks.addAll(detectListenerLeaks())
            
            // Update state
            _memoryLeaks.value = leaks
            
            // Notify observers
            leaks.forEach { leak ->
                notifyMemoryLeakDetected(leak)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting memory leaks", e)
        }
        
        return leaks
    }

    /**
     * Set memory pressure threshold
     */
    fun setMemoryPressureThreshold(threshold: Int) {
        require(threshold in 50..95) { "Threshold must be between 50 and 95" }
        gcThreshold = threshold
        Log.d(TAG, "Memory pressure threshold set to: $threshold%")
    }

    /**
     * Get memory recommendations
     */
    fun getMemoryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val usage = getCurrentMemoryUsage()
        
        when {
            usage.usedMemoryPercent > CRITICAL_THRESHOLD -> {
                recommendations.add("Critical memory usage detected. Consider closing background apps.")
                recommendations.add("Force garbage collection immediately.")
                recommendations.add("Clear memory cache to free up space.")
            }
            usage.usedMemoryPercent > GC_TRIGGER_THRESHOLD -> {
                recommendations.add("High memory usage. Consider running garbage collection.")
                recommendations.add("Review memory cache size.")
            }
            memoryStats.peakMemoryUsage > usage.maxHeap * 0.8 -> {
                recommendations.add("Peak memory usage is high. Monitor for potential memory leaks.")
            }
            memoryStats.memoryPressureEvents.get() > 10 -> {
                recommendations.add("Frequent memory pressure events. Consider optimization strategies.")
            }
        }
        
        return recommendations
    }

    /**
     * Add memory observer
     */
    fun addObserver(observer: MemoryObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    /**
     * Remove memory observer
     */
    fun removeObserver(observer: MemoryObserver) {
        observers.remove(observer)
    }

    // Private methods
    private suspend fun memoryMonitoringLoop() {
        while (true) {
            try {
                val usage = getCurrentMemoryUsage()
                
                // Update statistics
                memoryStats.peakMemoryUsage = maxOf(memoryStats.peakMemoryUsage, usage.usedHeap)
                memoryStats.averageMemoryUsage = (memoryStats.averageMemoryUsage + usage.usedHeap) / 2
                
                // Update state
                _currentMemoryUsage.value = usage
                _isLowMemory.value = usage.usedMemoryPercent > GC_TRIGGER_THRESHOLD
                
                // Add to trends
                addMemorySnapshot(usage)
                
                // Trigger GC if needed
                if (usage.usedMemoryPercent > gcThreshold) {
                    memoryStats.memoryPressureEvents.incrementAndGet()
                    triggerGarbageCollection("High memory pressure: ${usage.usedMemoryPercent}%")
                    
                    if (usage.usedMemoryPercent > CRITICAL_THRESHOLD) {
                        optimizeMemory()
                    }
                }
                
                notifyMemoryUsageChanged(usage)
                
                delay(MEMORY_CHECK_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error in memory monitoring loop", e)
                delay(MEMORY_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun leakDetectionLoop() {
        while (true) {
            try {
                detectMemoryLeaks()
                delay(LEAK_DETECTION_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Error in leak detection loop", e)
                delay(LEAK_DETECTION_INTERVAL)
            }
        }
    }

    private fun addMemorySnapshot(usage: MemoryUsage) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedHeap = usage.usedHeap,
            maxHeap = usage.maxHeap,
            nativeHeap = usage.nativeHeap,
            gcCount = memoryStats.totalGCCount.get(),
            processMemoryInfo = memoryInfo
        )
        
        val currentTrends = _memoryTrends.value.toMutableList()
        currentTrends.add(snapshot)
        
        // Keep only last 100 snapshots
        if (currentTrends.size > 100) {
            currentTrends.removeFirst()
        }
        
        _memoryTrends.value = currentTrends
    }

    private fun clearStaleReferences(): Long {
        var freedBytes = 0L
        val iterator = weakReferences.entries.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val ref = entry.value.get()
            
            if (ref == null) {
                iterator.remove()
                freedBytes += 1024L // Estimate freed memory
            }
        }
        
        return freedBytes
    }

    private fun detectContextLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        
        // Simplified context leak detection
        // In a real implementation, you would analyze object references more thoroughly
        weakReferences.forEach { (key, ref) ->
            ref.get()?.let { obj ->
                if (obj.javaClass.name.contains("Context")) {
                    leaks.add(
                        MemoryLeak(
                            objectName = key,
                            leakType = LeakType.CONTEXT_LEAK,
                            size = 1024L, // Estimated
                            description = "Context object retained in memory",
                            stackTrace = Thread.currentThread().stackTrace.toString()
                        )
                    )
                }
            }
        }
        
        return leaks
    }

    private fun detectHandlerLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        
        // Simplified handler leak detection
        weakReferences.forEach { (key, ref) ->
            ref.get()?.let { obj ->
                if (obj.javaClass.name.contains("Handler")) {
                    leaks.add(
                        MemoryLeak(
                            objectName = key,
                            leakType = LeakType.HANDLER_LEAK,
                            size = 2048L, // Estimated
                            description = "Handler object may cause memory leak",
                            stackTrace = Thread.currentThread().stackTrace.toString()
                        )
                    )
                }
            }
        }
        
        return leaks
    }

    private fun detectListenerLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        
        // Simplified listener leak detection
        weakReferences.forEach { (key, ref) ->
            ref.get()?.let { obj ->
                if (obj.javaClass.name.contains("Listener") || 
                    obj.javaClass.name.contains("Observer")) {
                    leaks.add(
                        MemoryLeak(
                            objectName = key,
                            leakType = LeakType.LISTENER_LEAK,
                            size = 512L, // Estimated
                            description = "Listener/Observer object may cause memory leak",
                            stackTrace = Thread.currentThread().stackTrace.toString()
                        )
                    )
                }
            }
        }
        
        return leaks
    }

    private fun notifyMemoryUsageChanged(usage: MemoryUsage) {
        observers.forEach { observer ->
            try {
                observer.onMemoryUsageChanged(usage)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying memory usage observer", e)
            }
        }
    }

    private fun notifyLowMemoryDetected(usage: MemoryUsage) {
        observers.forEach { observer ->
            try {
                observer.onLowMemoryDetected(usage)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying low memory observer", e)
            }
        }
    }

    private fun notifyGarbageCollectionTriggered(reason: String) {
        observers.forEach { observer ->
            try {
                observer.onGarbageCollectionTriggered(reason)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying GC observer", e)
            }
        }
    }

    private fun notifyMemoryLeakDetected(leak: MemoryLeak) {
        observers.forEach { observer ->
            try {
                observer.onMemoryLeakDetected(leak)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying memory leak observer", e)
            }
        }
    }

    private fun notifyMemoryOptimizationCompleted(optimizedBytes: Long) {
        observers.forEach { observer ->
            try {
                observer.onMemoryOptimizationCompleted(optimizedBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying memory optimization observer", e)
            }
        }
    }

    private fun notifyCacheSizeChanged(size: Int, capacity: Int) {
        observers.forEach { observer ->
            try {
                observer.onCacheSizeChanged(size, capacity)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying cache size observer", e)
            }
        }
    }
}

/**
 * LRU Cache implementation for memory management
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val cache = LinkedHashMap<K, V>(0, 0.75f, true)
    private var size = 0

    fun get(key: K): V? {
        synchronized(cache) {
            return cache[key]
        }
    }

    fun put(key: K, value: V): V? {
        synchronized(cache) {
            val previous = cache.put(key, value)
            
            if (previous != null) {
                size -= calculateSize(previous)
            }
            
            size += calculateSize(value)
            
            while (size > maxSize && cache.isNotEmpty()) {
                val eldest = cache.entries.iterator().next()
                eldest.value?.let {
                    size -= calculateSize(it)
                }
                cache.remove(eldest.key)
            }
            
            return previous
        }
    }

    fun remove(key: K): V? {
        synchronized(cache) {
            val removed = cache.remove(key)
            removed?.let {
                size -= calculateSize(it)
            }
            return removed
        }
    }

    fun size(): Int = cache.size
    fun maxSize(): Int = maxSize
    fun evictAll() {
        synchronized(cache) {
            cache.clear()
            size = 0
        }
    }

    private fun calculateSize(value: V): Int {
        return 1 // Simplified - in real implementation, calculate actual size
    }
}