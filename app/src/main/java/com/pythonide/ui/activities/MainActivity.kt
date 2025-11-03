package com.pythonide.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.pythonide.R
import com.pythonide.databinding.ActivityMainBinding
import com.pythonide.ui.adapters.MainPagerAdapter
import com.pythonide.ui.fragments.EditorFragment
import com.pythonide.ui.fragments.TerminalFragment
import com.pythonide.ui.fragments.FilesFragment
import com.pythonide.ui.fragments.GitFragment
import com.pythonide.utils.ThemeManager
import com.pythonide.utils.PermissionManager

/**
 * Main activity for Python IDE application
 * Handles navigation, theme management, and main UI
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabNewFile: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeViews()
        setupToolbar()
        setupViewPager()
        setupBottomNavigation()
        setupFab()
        setupListeners()
        
        // Check permissions on startup
        checkRequiredPermissions()
        
        // Handle intent extras
        handleIntent(intent)
    }
    
    private fun initializeViews() {
        toolbar = binding.toolbar
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        bottomNavigation = binding.bottomNavigation
        fabNewFile = binding.fabNewFile
        progressBar = binding.progressBar
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupViewPager() {
        val fragments = listOf(
            EditorFragment(),
            TerminalFragment(),
            FilesFragment(),
            GitFragment()
        )
        
        val adapter = MainPagerAdapter(supportFragmentManager, lifecycle, fragments)
        viewPager.adapter = adapter
        
        // Setup tab layout with view pager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.tab_editor)
                1 -> tab.text = getString(R.string.tab_terminal)
                2 -> tab.text = getString(R.string.tab_files)
                3 -> tab.text = getString(R.string.tab_git)
            }
        }.attach()
        
        // Set current tab based on intent
        val tabIndex = intent.getIntExtra("tab_index", 0)
        if (tabIndex < fragments.size) {
            viewPager.setCurrentItem(tabIndex, false)
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_editor -> {
                    viewPager.setCurrentItem(0, true)
                    true
                }
                R.id.nav_terminal -> {
                    viewPager.setCurrentItem(1, true)
                    true
                }
                R.id.nav_files -> {
                    viewPager.setCurrentItem(2, true)
                    true
                }
                R.id.nav_git -> {
                    viewPager.setCurrentItem(3, true)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupFab() {
        fabNewFile.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(viewPager.id)
            when (currentFragment) {
                is EditorFragment -> {
                    // Create new file in editor
                    currentFragment.createNewFile()
                }
                is FilesFragment -> {
                    // Show file creation dialog
                    currentFragment.showCreateFileDialog()
                }
                else -> {
                    // Default action - create new Python file
                    val intent = Intent(this, CodeEditorActivity::class.java)
                    intent.putExtra("action", "new_file")
                    intent.putExtra("file_type", "py")
                    startActivity(intent)
                }
            }
        }
    }
    
    private fun setupListeners() {
        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // Update bottom navigation selection
                val menuItem = when (position) {
                    0 -> R.id.nav_editor
                    1 -> R.id.nav_terminal
                    2 -> R.id.nav_files
                    3 -> R.id.nav_git
                    else -> R.id.nav_editor
                }
                bottomNavigation.selectedItemId = menuItem
                
                // Update FAB visibility and icon based on current fragment
                updateFabForCurrentFragment(position)
            }
        })
        
        // Handle keyboard visibility changes
        val rootView = findViewById<CoordinatorLayout>(R.id.snackbar_container)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val insets = android.view.ViewCompat.getRootWindowInsets(rootView)
            val isKeyboardVisible = insets?.isVisible(android.view.WindowInsetsCompat.Type.ime()) == true
            
            // Hide/show FAB when keyboard is visible
            if (isKeyboardVisible) {
                fabNewFile.hide()
            } else {
                fabNewFile.show()
            }
        }
    }
    
    private fun updateFabForCurrentFragment(position: Int) {
        when (position) {
            0 -> {
                fabNewFile.setImageResource(R.drawable.ic_add)
                fabNewFile.contentDescription = getString(R.string.editor_new_file)
            }
            1 -> {
                fabNewFile.setImageResource(R.drawable.ic_play_arrow)
                fabNewFile.contentDescription = getString(R.string.action_run)
            }
            2 -> {
                fabNewFile.setImageResource(R.drawable.ic_folder_add)
                fabNewFile.contentDescription = getString(R.string.file_new_folder)
            }
            3 -> {
                fabNewFile.setImageResource(R.drawable.ic_git_commit)
                fabNewFile.contentDescription = getString(R.string.git_commit)
            }
        }
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                Intent.ACTION_VIEW -> {
                    // File opened from external source
                    val uri = it.data
                    if (uri != null) {
                        openFileInEditor(uri)
                    }
                }
                Intent.ACTION_EDIT -> {
                    // File opened for editing
                    val uri = it.data
                    if (uri != null) {
                        openFileInEditor(uri)
                    }
                }
            }
        }
    }
    
    private fun openFileInEditor(uri: android.net.Uri) {
        val intent = Intent(this, CodeEditorActivity::class.java)
        intent.putExtra("file_uri", uri.toString())
        intent.putExtra("action", "open_file")
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new -> {
                // New file action
                createNewFile()
                true
            }
            R.id.action_open -> {
                // Open file action
                openFile()
                true
            }
            R.id.action_save -> {
                // Save current file
                saveCurrentFile()
                true
            }
            R.id.action_file_manager -> {
                // Open file manager
                val intent = Intent(this, FileManagerActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                // Open settings
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_about -> {
                // Show about dialog
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun createNewFile() {
        val currentFragment = supportFragmentManager.findFragmentById(viewPager.id)
        when (currentFragment) {
            is EditorFragment -> {
                currentFragment.createNewFile()
            }
            else -> {
                val intent = Intent(this, CodeEditorActivity::class.java)
                intent.putExtra("action", "new_file")
                startActivity(intent)
            }
        }
    }
    
    private fun openFile() {
        // Open file chooser
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(intent, REQUEST_OPEN_FILE)
    }
    
    private fun saveCurrentFile() {
        val currentFragment = supportFragmentManager.findFragmentById(viewPager.id)
        when (currentFragment) {
            is EditorFragment -> {
                currentFragment.saveCurrentFile()
            }
            else -> {
                showMessage(getString(R.string.msg_save_changes))
            }
        }
    }
    
    private fun showAboutDialog() {
        // Implement about dialog
        val aboutText = """
            ${getString(R.string.app_name)} v${BuildConfig.VERSION_NAME}
            
            ${getString(R.string.feature_monaco_editor)}
            ${getString(R.string.feature_termux_integration)}
            ${getString(R.string.feature_git_support)}
            ${getString(R.string.feature_room_database)}
            ${getString(R.string.feature_webview_support)}
            
            بيئة تطوير متكاملة للبرمجة بـ Python
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_about))
            .setMessage(aboutText)
            .setPositiveButton(getString(R.string.msg_ok), null)
            .show()
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun checkRequiredPermissions() {
        PermissionManager.checkAndRequestPermissions(this) { allGranted, deniedPermissions ->
            if (!allGranted) {
                val message = getString(R.string.permission_storage_required)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.permission_grant)) {
                        PermissionManager.requestPermissions(this, deniedPermissions)
                    }
                    .show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_FILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        openFileInEditor(uri)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes (orientation, etc.)
        ThemeManager.applyTheme(this)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handlePermissionResult(requestCode, grantResults)
    }
    
    companion object {
        private const val REQUEST_OPEN_FILE = 1001
    }
}