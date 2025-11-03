package com.pythonide.advanced;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

/**
 * Git Integration for tracking code changes and providing version control
 * Supports repository operations, change detection, and history management
 */
public class GitIntegration {
    private static final String GIT_COMMAND = "git";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private String repositoryPath;
    private ExecutorService executorService;
    private List<GitChangeListener> listeners;
    
    public interface GitChangeListener {
        void onChangeDetected(List<GitChange> changes);
        void onCommitCompleted(String commitHash);
        void onBranchChanged(String branchName);
    }
    
    public static class GitChange {
        private String filePath;
        private ChangeType type;
        private String diff;
        private Date timestamp;
        private String author;
        
        public enum ChangeType {
            ADDED, MODIFIED, DELETED, RENAMED
        }
        
        public GitChange(String filePath, ChangeType type, String diff) {
            this.filePath = filePath;
            this.type = type;
            this.diff = diff;
            this.timestamp = new Date();
        }
        
        // Getters and setters
        public String getFilePath() { return filePath; }
        public ChangeType getType() { return type; }
        public String getDiff() { return diff; }
        public Date getTimestamp() { return timestamp; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }
    
    public GitIntegration(String repositoryPath) {
        this.repositoryPath = repositoryPath;
        this.executorService = Executors.newCachedThreadPool();
        this.listeners = new ArrayList<>();
        initializeRepository();
    }
    
    private void initializeRepository() {
        if (!isGitRepository()) {
            try {
                executeCommand("init");
            } catch (Exception e) {
                // Repository might already exist
            }
        }
    }
    
    private boolean isGitRepository() {
        try {
            File gitDir = new File(repositoryPath + "/.git");
            return gitDir.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect changes since last commit
     */
    public CompletableFuture<List<GitChange>> detectChanges() {
        return CompletableFuture.supplyAsync(() -> {
            List<GitChange> changes = new ArrayList<>();
            
            try {
                // Get changed files
                String statusOutput = executeCommand("status", "--porcelain");
                String[] lines = statusOutput.split("\n");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    String status = line.substring(0, 2);
                    String filePath = line.substring(3);
                    
                    ChangeType changeType = mapStatusToChangeType(status);
                    String diff = getFileDiff(filePath);
                    
                    GitChange change = new GitChange(filePath, changeType, diff);
                    changes.add(change);
                }
                
            } catch (Exception e) {
                // Handle error
            }
            
            return changes;
        });
    }
    
    private ChangeType mapStatusToChangeType(String status) {
        if (status.startsWith("??")) return ChangeType.ADDED;
        if (status.startsWith("M") || status.endsWith("M")) return ChangeType.MODIFIED;
        if (status.startsWith("D") || status.endsWith("D")) return ChangeType.DELETED;
        if (status.startsWith("R") || status.endsWith("R")) return ChangeType.RENAMED;
        return ChangeType.MODIFIED;
    }
    
    private String getFileDiff(String filePath) {
        try {
            return executeCommand("diff", "--cached", filePath);
        } catch (Exception e) {
            try {
                return executeCommand("diff", filePath);
            } catch (Exception ex) {
                return "";
            }
        }
    }
    
    /**
     * Commit changes with automatic change detection
     */
    public CompletableFuture<String> commitChanges(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Stage all changes
                executeCommand("add", ".");
                
                // Commit with message
                String commitOutput = executeCommand("commit", "-m", message);
                
                // Extract commit hash
                String[] lines = commitOutput.split("\n");
                for (String line : lines) {
                    if (line.startsWith("[")) {
                        String[] parts = line.split(" ");
                        if (parts.length > 1) {
                            String commitHash = parts[1].replace("(", "").replace(")", "");
                            notifyCommitCompleted(commitHash);
                            return commitHash;
                        }
                    }
                }
                
            } catch (Exception e) {
                // Handle error
            }
            return "";
        });
    }
    
    /**
     * Create branch
     */
    public CompletableFuture<String> createBranch(String branchName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                executeCommand("checkout", "-b", branchName);
                notifyBranchChanged(branchName);
                return branchName;
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Switch branch
     */
    public CompletableFuture<String> switchBranch(String branchName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                executeCommand("checkout", branchName);
                notifyBranchChanged(branchName);
                return branchName;
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Get commit history
     */
    public CompletableFuture<List<CommitHistory>> getCommitHistory() {
        return CompletableFuture.supplyAsync(() -> {
            List<CommitHistory> history = new ArrayList<>();
            
            try {
                String logOutput = executeCommand("log", "--oneline", "--graph", "-10");
                String[] lines = logOutput.split("\n");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    // Parse commit line
                    String commitHash = extractCommitHash(line);
                    String message = extractCommitMessage(line);
                    String author = extractAuthor(line);
                    Date date = extractDate(line);
                    
                    if (commitHash != null) {
                        history.add(new CommitHistory(commitHash, message, author, date));
                    }
                }
                
            } catch (Exception e) {
                // Handle error
            }
            
            return history;
        });
    }
    
    public static class CommitHistory {
        private String hash;
        private String message;
        private String author;
        private Date date;
        
        public CommitHistory(String hash, String message, String author, Date date) {
            this.hash = hash;
            this.message = message;
            this.author = author;
            this.date = date;
        }
        
        // Getters
        public String getHash() { return hash; }
        public String getMessage() { return message; }
        public String getAuthor() { return author; }
        public Date getDate() { return date; }
    }
    
    private String executeCommand(String... args) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(new File(repositoryPath));
        
        Process process = processBuilder.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Git command failed with exit code: " + exitCode);
        }
        
        return output.toString().trim();
    }
    
    private String extractCommitHash(String line) {
        // Extract hash from git log output
        return line.substring(0, Math.min(7, line.length()));
    }
    
    private String extractCommitMessage(String line) {
        // Extract commit message from git log output
        int firstSpace = line.indexOf(' ');
        if (firstSpace > 0) {
            return line.substring(firstSpace + 1).trim();
        }
        return "";
    }
    
    private String extractAuthor(String line) {
        // Extract author from git log output
        return "Author"; // Simplified for demo
    }
    
    private Date extractDate(String line) {
        // Extract date from git log output
        return new Date();
    }
    
    public void addChangeListener(GitChangeListener listener) {
        listeners.add(listener);
    }
    
    private void notifyChangeDetected(List<GitChange> changes) {
        for (GitChangeListener listener : listeners) {
            listener.onChangeDetected(changes);
        }
    }
    
    private void notifyCommitCompleted(String commitHash) {
        for (GitChangeListener listener : listeners) {
            listener.onCommitCompleted(commitHash);
        }
    }
    
    private void notifyBranchChanged(String branchName) {
        for (GitChangeListener listener : listeners) {
            listener.onBranchChanged(branchName);
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}