package com.pythonide.templates.templates

import com.pythonide.templates.*
import java.io.File

/**
 * Data Science and Machine Learning template
 */
class DataScienceTemplate : ProjectTemplate {
    
    override fun getTemplateInfo(): ProjectTemplate {
        return ProjectTemplate(
            id = "datascience",
            name = "Data Science & ML",
            description = "Comprehensive data science and machine learning project setup",
            category = "Data Science",
            icon = "datascience",
            version = "1.0.0",
            author = "Python IDE",
            tags = listOf("data", "machine-learning", "science", "jupyter", "pandas", "numpy")
        )
    }
    
    override fun getTemplateFiles(): List<TemplateFile> {
        return listOf(
            // Main analysis script
            TemplateFile(
                targetPath = "main.py",
                content = """
#!/usr/bin/env python3
\"\"\"
{{ PROJECT_NAME }} - Data Science and Machine Learning Project
\"\"\"

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
import logging
from typing import Dict, Any, List
import joblib

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('logs/analysis.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Import custom modules
<!-- API:START -->
<!-- API:DISABLED -->
try:
    from api.api_server import start_api_server
    API_AVAILABLE = True
except ImportError:
    API_AVAILABLE = False
    logger.warning("API server not available. Install FastAPI to enable.")
<!-- API:END -->

try:
    from src.data_loader import DataLoader
    from src.data_preprocessor import DataPreprocessor
    from src.analytics import DataAnalytics
    from src.visualization import DataVisualizer
    from src.models import ModelTrainer, ModelEvaluator
    from src.config import Config
    MODULES_AVAILABLE = True
except ImportError:
    MODULES_AVAILABLE = False
    logger.error("Required modules not found. Please check project structure.")

def main():
    \"\"\"Main execution function.\"\"\"
    logger.info("Starting {{ PROJECT_NAME }} data science analysis")
    
    # Load configuration
    config = Config()
    
    # Initialize components
    if MODULES_AVAILABLE:
        data_loader = DataLoader(config)
        preprocessor = DataPreprocessor(config)
        analytics = DataAnalytics(config)
        visualizer = DataVisualizer(config)
        model_trainer = ModelTrainer(config)
        model_evaluator = ModelEvaluator(config)
        
        # Data pipeline
        try:
            # Load data
            logger.info("Loading data...")
            data = data_loader.load_data()
            logger.info(f"Data loaded successfully. Shape: {data.shape}")
            
            # Preprocess data
            logger.info("Preprocessing data...")
            processed_data = preprocessor.preprocess_data(data)
            logger.info(f"Data preprocessed successfully. Shape: {processed_data.shape}")
            
            # Perform analysis
            logger.info("Performing data analysis...")
            analysis_results = analytics.analyze_data(processed_data)
            logger.info("Analysis completed successfully")
            
            # Create visualizations
            logger.info("Creating visualizations...")
            visualizer.create_all_plots(processed_data, analysis_results)
            logger.info("Visualizations created successfully")
            
            # Train models
            logger.info("Training machine learning models...")
            trained_models = model_trainer.train_all_models(processed_data)
            logger.info("Models trained successfully")
            
            # Evaluate models
            logger.info("Evaluating models...")
            evaluation_results = model_evaluator.evaluate_all_models(trained_models, processed_data)
            logger.info("Model evaluation completed successfully")
            
            # Generate report
            logger.info("Generating analysis report...")
            generate_report(analysis_results, evaluation_results, config)
            logger.info("Report generated successfully")
            
        except Exception as e:
            logger.error(f"Error in data pipeline: {str(e)}")
            raise
    
    # Start API server if available
    <!-- API:START -->
    <!-- API:DISABLED -->
    if API_AVAILABLE and config.ENABLE_API:
        logger.info("Starting API server...")
        start_api_server()
    <!-- API:END -->
    
    logger.info("{{ PROJECT_NAME }} analysis completed")

def generate_report(analysis_results: Dict[str, Any], evaluation_results: Dict[str, Any], config: Config):
    \"\"\"Generate a comprehensive analysis report.\"\"\"
    report_path = Path(config.REPORTS_DIR) / "analysis_report.md"
    report_path.parent.mkdir(exist_ok=True)
    
    with open(report_path, 'w') as f:
        f.write(f\"\"\"# {{ PROJECT_NAME }} - Data Science Analysis Report
        
## Project Overview
- **Project Name**: {{ PROJECT_NAME }}
- **Generated**: {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}
- **Data Source**: {config.DATA_SOURCE if hasattr(config, 'DATA_SOURCE') else 'Not specified'}

## Data Summary
- **Total Records**: {analysis_results.get('total_records', 'N/A')}
- **Features**: {analysis_results.get('total_features', 'N/A')}
- **Missing Values**: {analysis_results.get('missing_values', 'N/A')}

## Data Quality
- **Completeness**: {analysis_results.get('completeness', 'N/A')}
- **Data Types**: 
\"\"\")
        
        if 'data_types' in analysis_results:
            for col, dtype in analysis_results['data_types'].items():
                f.write(f\"  - {col}: {dtype}\\n\")
        
        f.write(\"\"\"
## Statistical Analysis
\"\"\")
        
        if 'statistics' in analysis_results:
            stats = analysis_results['statistics']
            for col, stat_dict in stats.items():
                f.write(f\"\"\"### {col}
- Mean: {stat_dict.get('mean', 'N/A')}
- Median: {stat_dict.get('median', 'N/A')}
- Std: {stat_dict.get('std', 'N/A')}
- Min: {stat_dict.get('min', 'N/A')}
- Max: {stat_dict.get('max', 'N/A')}
\"\"\")
        
        f.write(\"\"\"
## Machine Learning Models
\"\"\")
        
        if evaluation_results:
            for model_name, metrics in evaluation_results.items():
                f.write(f\"\"\"### {model_name}
\"\"\")
                for metric_name, value in metrics.items():
                    f.write(f\"- {metric_name}: {value:.4f}\\n\")
                f.write(\"\\n\")
        
        f.write(\"\"\"
## Files Generated
- `data/raw/`: Raw data files
- `data/processed/`: Processed data files
- `models/`: Trained machine learning models
- `reports/`: Analysis reports and visualizations
- `notebooks/`: Jupyter notebooks for exploration

## Recommendations
1. Review data quality issues identified in the analysis
2. Consider feature engineering based on correlation analysis
3. Explore ensemble methods for improved model performance
4. Implement cross-validation for more robust model evaluation

---
Generated by Python IDE Data Science Template
\"\"\")
    
    logger.info(f"Analysis report saved to: {report_path}")

if __name__ == "__main__":
    main()
                """.trimIndent()
            ),
            
            // Data Science requirements
            TemplateFile(
                targetPath = "requirements.txt",
                content = """
# Core Data Science Libraries
pandas==2.1.3
numpy==1.24.3
scipy==1.11.4
scikit-learn==1.3.2

# Visualization
matplotlib==3.8.2
seaborn==0.13.0
plotly==5.17.0
bokeh==3.3.1

# Jupyter and notebooks
jupyter==1.0.0
jupyterlab==4.0.9
notebook==7.0.6
ipywidgets==8.1.1

# Machine Learning
xgboost==2.0.3
lightgbm==4.1.0
catboost==1.2.2
tensorflow==2.15.0
torch==2.1.1

<!-- API:START -->
<!-- API:DISABLED -->
# API and Web Framework
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
python-multipart==0.0.6
<!-- API:END -->

# Data Storage
joblib==1.3.2
pickle5==0.0.12

# Utilities
pathlib2==2.3.7
tqdm==4.66.1
click==8.1.7
python-dotenv==1.0.0

<!-- TESTS:START -->
<!-- TESTS:DISABLED -->
# Testing
pytest==7.4.3
pytest-cov==4.1.0
hypothesis==6.92.1
<!-- TESTS:END -->
                """.trimIndent()
            ),
            
            // Configuration file
            TemplateFile(
                targetPath = "src/config.py",
                content = """
\"\"\"
Configuration settings for {{ PROJECT_NAME }} data science project.
\"\"\"

import os
from pathlib import Path
from typing import List, Dict, Any

class Config:
    \"\"\"Configuration class for data science project.\"\"\"
    
    # Project paths
    PROJECT_ROOT = Path(__file__).parent.parent
    DATA_DIR = PROJECT_ROOT / "data"
    RAW_DATA_DIR = DATA_DIR / "raw"
    PROCESSED_DATA_DIR = DATA_DIR / "processed"
    MODELS_DIR = PROJECT_ROOT / "models"
    REPORTS_DIR = PROJECT_ROOT / "reports"
    PLOTS_DIR = REPORTS_DIR / "plots"
    LOGS_DIR = PROJECT_ROOT / "logs"
    NOTEBOOKS_DIR = PROJECT_ROOT / "notebooks"
    
    # Data configuration
    DATA_SOURCE = os.getenv("DATA_SOURCE", "sample_data.csv")
    TARGET_COLUMN = os.getenv("TARGET_COLUMN", "target")
    TEST_SIZE = float(os.getenv("TEST_SIZE", "0.2"))
    VALIDATION_SIZE = float(os.getenv("VALIDATION_SIZE", "0.1"))
    RANDOM_STATE = int(os.getenv("RANDOM_STATE", "42"))
    
    # Preprocessing configuration
    HANDLE_MISSING = os.getenv("HANDLE_MISSING", "drop").lower()
    SCALE_FEATURES = os.getenv("SCALE_FEATURES", "true").lower() == "true"
    ENCODE_CATEGORICAL = os.getenv("ENCODE_CATEGORICAL", "true").lower() == "true"
    
    # Model configuration
    MODELS_TO_TRAIN = [
        "RandomForest",
        "GradientBoosting",
        "LogisticRegression",
        "SVM",
        "XGBoost"
    ]
    CROSS_VALIDATION_FOLDS = int(os.getenv("CV_FOLDS", "5"))
    
    # Visualization configuration
    PLOT_STYLE = os.getenv("PLOT_STYLE", "seaborn")
    FIGURE_SIZE = eval(os.getenv("FIGURE_SIZE", "(12, 8)"))
    DPI = int(os.getenv("FIGURE_DPI", "150"))
    
    <!-- API:START -->
    <!-- API:DISABLED -->
    # API configuration
    ENABLE_API = os.getenv("ENABLE_API", "false").lower() == "true"
    API_HOST = os.getenv("API_HOST", "0.0.0.0")
    API_PORT = int(os.getenv("API_PORT", "8000"))
    <!-- API:END -->
    
    # Logging configuration
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
    LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    
    def __init__(self):
        \"\"\"Initialize configuration and create necessary directories.\"\"\"
        self.create_directories()
    
    def create_directories(self):
        \"\"\"Create necessary directories if they don't exist.\"\"\"
        directories = [
            self.DATA_DIR,
            self.RAW_DATA_DIR,
            self.PROCESSED_DATA_DIR,
            self.MODELS_DIR,
            self.REPORTS_DIR,
            self.PLOTS_DIR,
            self.LOGS_DIR,
            self.NOTEBOOKS_DIR
        ]
        
        for directory in directories:
            directory.mkdir(parents=True, exist_ok=True)
    
    def get_model_config(self, model_name: str) -> Dict[str, Any]:
        \"\"\"Get configuration for specific model.\"\"\"
        model_configs = {
            "RandomForest": {
                "n_estimators": 100,
                "max_depth": 10,
                "random_state": self.RANDOM_STATE
            },
            "GradientBoosting": {
                "n_estimators": 100,
                "learning_rate": 0.1,
                "max_depth": 5,
                "random_state": self.RANDOM_STATE
            },
            "LogisticRegression": {
                "random_state": self.RANDOM_STATE,
                "max_iter": 1000
            },
            "SVM": {
                "random_state": self.RANDOM_STATE
            },
            "XGBoost": {
                "n_estimators": 100,
                "max_depth": 5,
                "learning_rate": 0.1,
                "random_state": self.RANDOM_STATE
            }
        }
        
        return model_configs.get(model_name, {})
                """.trimIndent()
            ),
            
            // Data loader module
            TemplateFile(
                targetPath = "src/data_loader.py",
                content = """
\"\"\"
Data loading utilities for {{ PROJECT_NAME }}.
\"\"\"

import pandas as pd
import numpy as np
from pathlib import Path
import logging
from typing import Dict, Any, Union
from sklearn.datasets import load_iris, load_boston, load_wine, load_breast_cancer
from sklearn.model_selection import train_test_split

logger = logging.getLogger(__name__)

class DataLoader:
    \"\"\"Data loading class with support for various data sources.\"\"\"
    
    def __init__(self, config):
        self.config = config
        self.data_cache = {}
    
    def load_data(self, source: str = None) -> pd.DataFrame:
        \"\"\"
        Load data from various sources.
        
        Args:
            source: Data source (file path, url, or dataset name)
        
        Returns:
            Loaded DataFrame
        \"\"\"
        source = source or self.config.DATA_SOURCE
        
        # Check cache first
        if source in self.data_cache:
            logger.info(f"Loading {source} from cache")
            return self.data_cache[source]
        
        # Determine data source type
        if source.startswith('http'):
            df = self.load_from_url(source)
        elif source.startswith('sklearn:'):
            df = self.load_from_sklearn(source)
        elif Path(source).exists():
            df = self.load_from_file(source)
        else:
            # Try to load sample dataset
            df = self.load_sample_dataset(source)
        
        # Cache the data
        self.data_cache[source] = df
        logger.info(f"Loaded data from {source}. Shape: {df.shape}")
        
        return df
    
    def load_from_file(self, file_path: str) -> pd.DataFrame:
        \"\"\"Load data from various file formats.\"\"\"
        file_path = Path(file_path)
        suffix = file_path.suffix.lower()
        
        try:
            if suffix == '.csv':
                df = pd.read_csv(file_path)
            elif suffix in ['.xlsx', '.xls']:
                df = pd.read_excel(file_path)
            elif suffix == '.json':
                df = pd.read_json(file_path)
            elif suffix == '.parquet':
                df = pd.read_parquet(file_path)
            else:
                raise ValueError(f"Unsupported file format: {suffix}")
            
            logger.info(f"Loaded data from {file_path}")
            return df
            
        except Exception as e:
            logger.error(f"Error loading file {file_path}: {str(e)}")
            raise
    
    def load_from_url(self, url: str) -> pd.DataFrame:
        \"\"\"Load data from URL.\"\"\"
        try:
            if url.endswith('.csv'):
                df = pd.read_csv(url)
            elif url.endswith('.json'):
                df = pd.read_json(url)
            else:
                raise ValueError(f"Unsupported URL format: {url}")
            
            logger.info(f"Loaded data from URL: {url}")
            return df
            
        except Exception as e:
            logger.error(f"Error loading from URL {url}: {str(e)}")
            raise
    
    def load_from_sklearn(self, dataset_name: str) -> pd.DataFrame:
        \"\"\"Load datasets from sklearn.\"\"\"
        dataset_name = dataset_name.replace('sklearn:', '')
        
        dataset_map = {
            'iris': load_iris,
            'boston': load_boston,
            'wine': load_wine,
            'breast_cancer': load_breast_cancer
        }
        
        if dataset_name not in dataset_map:
            raise ValueError(f"Unknown sklearn dataset: {dataset_name}")
        
        loader = dataset_map[dataset_name]
        data = loader()
        
        # Convert to DataFrame
        df = pd.DataFrame(data.data, columns=data.feature_names)
        if hasattr(data, 'target'):
            df['target'] = data.target
        
        # Add dataset info
        df.attrs['dataset_name'] = dataset_name
        df.attrs['target_names'] = data.target_names if hasattr(data, 'target_names') else None
        
        logger.info(f"Loaded sklearn dataset: {dataset_name}")
        return df
    
    def load_sample_dataset(self, dataset_name: str = 'iris') -> pd.DataFrame:
        \"\"\"Load a sample dataset for demonstration.\"\"\"
        if dataset_name.lower() == 'iris':
            data = load_iris()
            df = pd.DataFrame(data.data, columns=data.feature_names)
            df['target'] = data.target
            df['target_names'] = data.target_names.tolist()
            
            # Save to raw data directory
            file_path = self.config.RAW_DATA_DIR / 'iris.csv'
            df.to_csv(file_path, index=False)
            
            logger.info("Loaded iris sample dataset")
            return df
        else:
            # Create synthetic data
            logger.warning(f"Unknown dataset '{dataset_name}', creating synthetic data")
            return self.create_synthetic_data()
    
    def create_synthetic_data(self, n_samples: int = 1000, n_features: int = 5) -> pd.DataFrame:
        \"\"\"Create synthetic dataset for testing.\"\"\"
        np.random.seed(self.config.RANDOM_STATE)
        
        # Generate features
        X = np.random.randn(n_samples, n_features)
        
        # Generate target with some correlation to features
        target = (
            0.5 * X[:, 0] + 
            0.3 * X[:, 1] - 
            0.2 * X[:, 2] + 
            np.random.randn(n_samples) * 0.1
        )
        
        # Create DataFrame
        feature_names = [f'feature_{i}' for i in range(n_features)]
        df = pd.DataFrame(X, columns=feature_names)
        df['target'] = target
        
        # Add some missing values
        missing_indices = np.random.choice(df.index, size=int(0.05 * len(df)), replace=False)
        df.loc[missing_indices[:len(missing_indices)//2], 'feature_0'] = np.nan
        
        # Save to raw data directory
        file_path = self.config.RAW_DATA_DIR / 'synthetic_data.csv'
        df.to_csv(file_path, index=False)
        
        logger.info(f"Created synthetic dataset with {n_samples} samples and {n_features} features")
        return df
    
    def split_data(self, df: pd.DataFrame, target_column: str = None) -> tuple:
        \"\"\"Split data into features and target.\"\"\"
        target_column = target_column or self.config.TARGET_COLUMN
        
        if target_column not in df.columns:
            logger.warning(f"Target column '{target_column}' not found. Using last column.")
            target_column = df.columns[-1]
        
        X = df.drop(columns=[target_column])
        y = df[target_column]
        
        # Split into train/validation/test
        X_train, X_temp, y_train, y_temp = train_test_split(
            X, y, test_size=(self.config.TEST_SIZE + self.config.VALIDATION_SIZE),
            random_state=self.config.RANDOM_STATE, stratify=y if len(np.unique(y)) < 10 else None
        )
        
        X_val, X_test, y_val, y_test = train_test_split(
            X_temp, y_temp, test_size=self.config.VALIDATION_SIZE / (self.config.TEST_SIZE + self.config.VALIDATION_SIZE),
            random_state=self.config.RANDOM_STATE, stratify=y_temp if len(np.unique(y_temp)) < 10 else None
        )
        
        logger.info(f"Data split - Train: {len(X_train)}, Validation: {len(X_val)}, Test: {len(X_test)}")
        
        return {
            'train': (X_train, y_train),
            'validation': (X_val, y_val),
            'test': (X_test, y_test)
        }
                """.trimIndent()
            ),
            
            // Data preprocessor module
            TemplateFile(
                targetPath = "src/data_preprocessor.py",
                content = """
\"\"\"
Data preprocessing utilities for {{ PROJECT_NAME }}.
\"\"\"

import pandas as pd
import numpy as np
import logging
from sklearn.preprocessing import StandardScaler, LabelEncoder, OneHotEncoder
from sklearn.impute import SimpleImputer, KNNImputer
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from typing import Dict, Any, List, Tuple

logger = logging.getLogger(__name__)

class DataPreprocessor:
    \"\"\"Data preprocessing class with various transformation options.\"\"\"
    
    def __init__(self, config):
        self.config = config
        self.preprocessors = {}
        self.feature_names = None
    
    def preprocess_data(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"
        Preprocess the entire dataset.
        
        Args:
            df: Input DataFrame
            
        Returns:
            Preprocessed DataFrame
        \"\"\"
        logger.info("Starting data preprocessing...")
        
        # Create a copy to avoid modifying original data
        processed_df = df.copy()
        
        # Remove completely empty rows and columns
        processed_df = self._remove_empty_data(processed_df)
        
        # Handle missing values
        if self.config.HANDLE_MISSING != 'none':
            processed_df = self._handle_missing_values(processed_df)
        
        # Encode categorical variables
        if self.config.ENCODE_CATEGORICAL:
            processed_df = self._encode_categorical_variables(processed_df)
        
        # Scale features
        if self.config.SCALE_FEATURES:
            processed_df = self._scale_features(processed_df)
        
        logger.info(f"Data preprocessing completed. Final shape: {processed_df.shape}")
        return processed_df
    
    def _remove_empty_data(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Remove completely empty rows and columns.\"\"\"
        initial_shape = df.shape
        
        # Remove rows with all NaN values
        df = df.dropna(how='all')
        
        # Remove columns with all NaN values
        df = df.dropna(axis=1, how='all')
        
        # Remove columns with only one unique value (constant columns)
        constant_cols = []
        for col in df.columns:
            if df[col].nunique() <= 1:
                constant_cols.append(col)
        
        if constant_cols:
            df = df.drop(columns=constant_cols)
            logger.info(f"Removed constant columns: {constant_cols}")
        
        final_shape = df.shape
        logger.info(f"Removed empty data. Shape: {initial_shape} -> {final_shape}")
        
        return df
    
    def _handle_missing_values(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Handle missing values based on configuration.\"\"\"
        logger.info("Handling missing values...")
        
        # Identify missing values
        missing_info = df.isnull().sum()
        missing_cols = missing_info[missing_info > 0]
        
        if len(missing_cols) == 0:
            logger.info("No missing values found")
            return df
        
        logger.info(f"Columns with missing values: {list(missing_cols.index)}")
        
        strategy = self.config.HANDLE_MISSING.lower()
        
        if strategy == 'drop':
            # Drop rows with missing values
            df_clean = df.dropna()
            logger.info(f"Dropped rows with missing values. Shape: {df.shape} -> {df_clean.shape}")
            
        elif strategy == 'mean':
            # Impute with mean for numerical columns
            df_clean = self._impute_with_mean(df)
            
        elif strategy == 'median':
            # Impute with median for numerical columns
            df_clean = self._impute_with_median(df)
            
        elif strategy == 'mode':
            # Impute with mode
            df_clean = self._impute_with_mode(df)
            
        elif strategy == 'knn':
            # KNN imputation
            df_clean = self._impute_with_knn(df)
            
        else:
            logger.warning(f"Unknown missing value strategy '{strategy}'. Using mean imputation.")
            df_clean = self._impute_with_mean(df)
        
        return df_clean
    
    def _impute_with_mean(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Impute missing values with mean.\"\"\"
        df_clean = df.copy()
        numeric_cols = df_clean.select_dtypes(include=[np.number]).columns
        
        for col in numeric_cols:
            if df_clean[col].isnull().any():
                mean_value = df_clean[col].mean()
                df_clean[col].fillna(mean_value, inplace=True)
                logger.info(f"Imputed {col} with mean: {mean_value:.4f}")
        
        return df_clean
    
    def _impute_with_median(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Impute missing values with median.\"\"\"
        df_clean = df.copy()
        numeric_cols = df_clean.select_dtypes(include=[np.number]).columns
        
        for col in numeric_cols:
            if df_clean[col].isnull().any():
                median_value = df_clean[col].median()
                df_clean[col].fillna(median_value, inplace=True)
                logger.info(f"Imputed {col} with median: {median_value:.4f}")
        
        return df_clean
    
    def _impute_with_mode(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Impute missing values with mode.\"\"\"
        df_clean = df.copy()
        
        for col in df_clean.columns:
            if df_clean[col].isnull().any():
                mode_value = df_clean[col].mode()
                if len(mode_value) > 0:
                    df_clean[col].fillna(mode_value[0], inplace=True)
                    logger.info(f"Imputed {col} with mode: {mode_value[0]}")
        
        return df_clean
    
    def _impute_with_knn(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Impute missing values using KNN.\"\"\"
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        df_numeric = df[numeric_cols]
        
        if len(numeric_cols) > 0:
            imputer = KNNImputer(n_neighbors=5)
            df_imputed = pd.DataFrame(
                imputer.fit_transform(df_numeric),
                columns=numeric_cols,
                index=df_numeric.index
            )
            
            # Combine with non-numeric columns
            df_clean = df.copy()
            df_clean[numeric_cols] = df_imputed
            
            logger.info("Applied KNN imputation to numeric columns")
        
        return df_clean
    
    def _encode_categorical_variables(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Encode categorical variables.\"\"\"
        logger.info("Encoding categorical variables...")
        
        df_encoded = df.copy()
        categorical_cols = df_encoded.select_dtypes(include=['object', 'category']).columns
        
        if len(categorical_cols) == 0:
            logger.info("No categorical columns found")
            return df_encoded
        
        logger.info(f"Categorical columns: {list(categorical_cols)}")
        
        # For high cardinality categorical columns, use label encoding
        # For low cardinality, use one-hot encoding
        for col in categorical_cols:
            unique_count = df_encoded[col].nunique()
            
            if unique_count <= 10:  # One-hot encoding for low cardinality
                dummies = pd.get_dummies(df_encoded[col], prefix=col)
                df_encoded = pd.concat([df_encoded.drop(columns=[col]), dummies], axis=1)
                logger.info(f"Applied one-hot encoding to {col} ({unique_count} unique values)")
                
            else:  # Label encoding for high cardinality
                le = LabelEncoder()
                df_encoded[col] = le.fit_transform(df_encoded[col].astype(str))
                logger.info(f"Applied label encoding to {col} ({unique_count} unique values)")
        
        return df_encoded
    
    def _scale_features(self, df: pd.DataFrame) -> pd.DataFrame:
        \"\"\"Scale numerical features.\"\"\"
        logger.info("Scaling features...")
        
        df_scaled = df.copy()
        
        # Identify numerical columns (excluding target if binary classification)
        numeric_cols = df_scaled.select_dtypes(include=[np.number]).columns
        target_cols = []
        
        # Check if target column exists and is binary
        if self.config.TARGET_COLUMN in df_scaled.columns:
            target_unique = df_scaled[self.config.TARGET_COLUMN].nunique()
            if target_unique <= 2:
                target_cols.append(self.config.TARGET_COLUMN)
        
        # Scale only non-target numerical columns
        scale_cols = [col for col in numeric_cols if col not in target_cols]
        
        if len(scale_cols) > 0:
            scaler = StandardScaler()
            df_scaled[scale_cols] = scaler.fit_transform(df_scaled[scale_cols])
            logger.info(f"Scaled {len(scale_cols)} features")
            
            # Store scaler for later use
            self.preprocessors['scaler'] = scaler
        
        return df_scaled
    
    def get_preprocessing_pipeline(self) -> Pipeline:
        \"\"\"Get scikit-learn preprocessing pipeline.\"\"\"
        # This would be used for consistent preprocessing in ML pipeline
        numeric_transformer = Pipeline(steps=[
            ('imputer', SimpleImputer(strategy='mean')),
            ('scaler', StandardScaler())
        ])
        
        categorical_transformer = Pipeline(steps=[
            ('imputer', SimpleImputer(strategy='constant', fill_value='missing')),
            ('onehot', OneHotEncoder(handle_unknown='ignore'))
        ])
        
        preprocessor = ColumnTransformer(
            transformers=[
                ('num', numeric_transformer, self._get_numeric_columns()),
                ('cat', categorical_transformer, self._get_categorical_columns())
            ]
        )
        
        return preprocessor
    
    def _get_numeric_columns(self) -> List[str]:
        \"\"\"Get numeric column names.\"\"\"
        # This would need access to the dataframe
        # Implementation depends on how this is used
        return []
    
    def _get_categorical_columns(self) -> List[str]:
        \"\"\"Get categorical column names.\"\"\"
        # This would need access to the dataframe
        # Implementation depends on how this is used
        return []
                """.trimIndent()
            ),
            
            // Analytics module
            TemplateFile(
                targetPath = "src/analytics.py",
                content = """
\"\"\"
Data analytics utilities for {{ PROJECT_NAME }}.
\"\"\"

import pandas as pd
import numpy as np
import logging
from typing import Dict, Any, List
from scipy import stats
from sklearn.feature_selection import mutual_info_classif, f_classif
import warnings

logger = logging.getLogger(__name__)

class DataAnalytics:
    \"\"\"Data analytics class for comprehensive data analysis.\"\"\"
    
    def __init__(self, config):
        self.config = config
    
    def analyze_data(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"
        Perform comprehensive data analysis.
        
        Args:
            df: Input DataFrame
            
        Returns:
            Dictionary containing analysis results
        \"\"\"
        logger.info("Starting comprehensive data analysis...")
        
        analysis_results = {
            'basic_info': self._get_basic_info(df),
            'data_quality': self._assess_data_quality(df),
            'statistics': self._calculate_statistics(df),
            'correlations': self._analyze_correlations(df),
            'distributions': self._analyze_distributions(df),
            'feature_importance': self._analyze_feature_importance(df),
            'outliers': self._detect_outliers(df)
        }
        
        logger.info("Data analysis completed successfully")
        return analysis_results
    
    def _get_basic_info(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Get basic information about the dataset.\"\"\"
        info = {
            'total_records': len(df),
            'total_features': len(df.columns),
            'memory_usage': df.memory_usage(deep=True).sum(),
            'data_types': df.dtypes.to_dict(),
            'shape': df.shape
        }
        
        # Dataset info if available
        if 'dataset_name' in df.attrs:
            info['dataset_source'] = df.attrs['dataset_name']
        
        if 'target_names' in df.attrs:
            info['target_classes'] = df.attrs['target_names']
        
        logger.info(f"Dataset info - Records: {info['total_records']}, Features: {info['total_features']}")
        return info
    
    def _assess_data_quality(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Assess data quality metrics.\"\"\"
        quality_metrics = {}
        
        # Missing values
        missing_counts = df.isnull().sum()
        missing_percentages = (missing_counts / len(df)) * 100
        
        quality_metrics['missing_values'] = {
            'count': missing_counts.to_dict(),
            'percentage': missing_percentages.to_dict(),
            'total_missing': missing_counts.sum(),
            'missing_features': len(missing_counts[missing_counts > 0])
        }
        
        # Completeness
        total_cells = df.shape[0] * df.shape[1]
        quality_metrics['completeness'] = {
            'percentage': ((total_cells - missing_counts.sum()) / total_cells) * 100,
            'complete_rows': len(df.dropna()),
            'complete_features': len(df.columns[missing_counts == 0])
        }
        
        # Data types
        dtype_counts = df.dtypes.value_counts()
        quality_metrics['data_types_distribution'] = dtype_counts.to_dict()
        
        # Duplicate rows
        duplicate_count = df.duplicated().sum()
        quality_metrics['duplicates'] = {
            'count': duplicate_count,
            'percentage': (duplicate_count / len(df)) * 100
        }
        
        # Cardinality
        cardinality = {}
        for col in df.columns:
            unique_count = df[col].nunique()
            cardinality[col] = {
                'unique_count': unique_count,
                'unique_percentage': (unique_count / len(df)) * 100,
                'is_high_cardinality': unique_count > len(df) * 0.8
            }
        
        quality_metrics['cardinality'] = cardinality
        
        logger.info(f"Data quality assessment completed. Missing values: {quality_metrics['missing_values']['total_missing']}")
        return quality_metrics
    
    def _calculate_statistics(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Calculate descriptive statistics.\"\"\"
        stats_dict = {}
        
        # Numerical statistics
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        if len(numeric_cols) > 0:
            stats_dict['numerical'] = {}
            
            for col in numeric_cols:
                col_data = df[col].dropna()
                if len(col_data) > 0:
                    stats_dict['numerical'][col] = {
                        'count': len(col_data),
                        'mean': float(col_data.mean()),
                        'median': float(col_data.median()),
                        'std': float(col_data.std()),
                        'variance': float(col_data.var()),
                        'min': float(col_data.min()),
                        'max': float(col_data.max()),
                        'q25': float(col_data.quantile(0.25)),
                        'q75': float(col_data.quantile(0.75)),
                        'skewness': float(stats.skew(col_data)),
                        'kurtosis': float(stats.kurtosis(col_data))
                    }
        
        # Categorical statistics
        categorical_cols = df.select_dtypes(include=['object', 'category']).columns
        if len(categorical_cols) > 0:
            stats_dict['categorical'] = {}
            
            for col in categorical_cols:
                col_data = df[col].dropna()
                if len(col_data) > 0:
                    value_counts = col_data.value_counts()
                    stats_dict['categorical'][col] = {
                        'unique_count': col_data.nunique(),
                        'most_frequent': value_counts.index[0] if len(value_counts) > 0 else None,
                        'most_frequent_count': int(value_counts.iloc[0]) if len(value_counts) > 0 else 0,
                        'value_counts': value_counts.head(10).to_dict()
                    }
        
        logger.info(f"Statistics calculated for {len(stats_dict.get('numerical', {}))} numerical and {len(stats_dict.get('categorical', {}))} categorical features")
        return stats_dict
    
    def _analyze_correlations(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Analyze correlations between features.\"\"\"
        correlation_analysis = {}
        
        # Numerical correlations
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        if len(numeric_cols) > 1:
            correlation_matrix = df[numeric_cols].corr()
            
            # Find highly correlated pairs
            high_corr_pairs = []
            for i in range(len(correlation_matrix.columns)):
                for j in range(i+1, len(correlation_matrix.columns)):
                    corr_value = correlation_matrix.iloc[i, j]
                    if abs(corr_value) > 0.8:  # Threshold for high correlation
                        high_corr_pairs.append({
                            'feature_1': correlation_matrix.columns[i],
                            'feature_2': correlation_matrix.columns[j],
                            'correlation': float(corr_value)
                        })
            
            correlation_analysis['numerical'] = {
                'correlation_matrix': correlation_matrix.to_dict(),
                'high_correlations': high_corr_pairs,
                'average_correlation': float(correlation_matrix.mean().mean())
            }
        
        # Feature-target correlations if target exists
        target_col = self.config.TARGET_COLUMN
        if target_col in df.columns and df[target_col].dtype in [np.number, 'int64', 'float64']:
            numeric_features = [col for col in numeric_cols if col != target_col]
            if len(numeric_features) > 0:
                target_corrs = df[numeric_features].corrwith(df[target_col]).abs().sort_values(ascending=False)
                correlation_analysis['target_correlations'] = target_corrs.to_dict()
        
        logger.info(f"Correlation analysis completed. Found {len(correlation_analysis.get('high_correlations', []))} high correlations")
        return correlation_analysis
    
    def _analyze_distributions(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Analyze feature distributions.\"\"\"
        distribution_analysis = {}
        
        # Numerical distributions
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        distribution_analysis['numerical'] = {}
        
        for col in numeric_cols[:10]:  # Analyze top 10 numerical features
            col_data = df[col].dropna()
            if len(col_data) > 0:
                # Normality tests
                try:
                    with warnings.catch_warnings():
                        warnings.simplefilter("ignore")
                        shapiro_stat, shapiro_p = stats.shapiro(col_data.sample(min(5000, len(col_data))))
                        ks_stat, ks_p = stats.kstest(col_data, 'norm')
                except:
                    shapiro_stat, shapiro_p = None, None
                    ks_stat, ks_p = None, None
                
                distribution_analysis['numerical'][col] = {
                    'normality_shapiro': {'statistic': shapiro_stat, 'p_value': shapiro_p},
                    'normality_ks': {'statistic': ks_stat, 'p_value': ks_p},
                    'is_normal': (shapiro_p > 0.05) if shapiro_p else None
                }
        
        # Categorical distributions
        categorical_cols = df.select_dtypes(include=['object', 'category']).columns
        distribution_analysis['categorical'] = {}
        
        for col in categorical_cols[:5]:  # Analyze top 5 categorical features
            col_data = df[col].dropna()
            if len(col_data) > 0:
                value_counts = col_data.value_counts(normalize=True)
                distribution_analysis['categorical'][col] = {
                    'value_distribution': value_counts.head(10).to_dict(),
                    'entropy': float(-sum(value_counts * np.log2(value_counts + 1e-10)))
                }
        
        logger.info(f"Distribution analysis completed for {len(distribution_analysis['numerical'])} numerical and {len(distribution_analysis['categorical'])} categorical features")
        return distribution_analysis
    
    def _analyze_feature_importance(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Analyze feature importance using various methods.\"\"\"
        importance_analysis = {}
        
        # Check if target exists
        target_col = self.config.TARGET_COLUMN
        if target_col not in df.columns:
            logger.warning("Target column not found. Skipping feature importance analysis.")
            return importance_analysis
        
        # Prepare data
        X = df.drop(columns=[target_col])
        y = df[target_col]
        
        # Remove non-numeric columns for feature importance
        X_numeric = X.select_dtypes(include=[np.number])
        if len(X_numeric.columns) == 0:
            logger.warning("No numeric features found for importance analysis.")
            return importance_analysis
        
        # Mutual Information (for classification and regression)
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                mi_scores = mutual_info_classif(X_numeric, y, random_state=self.config.RANDOM_STATE)
                
                importance_analysis['mutual_information'] = {
                    feature: float(score) 
                    for feature, score in zip(X_numeric.columns, mi_scores)
                }
        except Exception as e:
            logger.warning(f"Could not compute mutual information: {str(e)}")
        
        # ANOVA F-test
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                f_scores, f_p_values = f_classif(X_numeric, y)
                
                importance_analysis['anova_f'] = {
                    'scores': {
                        feature: float(score) 
                        for feature, score in zip(X_numeric.columns, f_scores)
                    },
                    'p_values': {
                        feature: float(p_val) 
                        for feature, p_val in zip(X_numeric.columns, f_p_values)
                    }
                }
        except Exception as e:
            logger.warning(f"Could not compute ANOVA F-test: {str(e)}")
        
        logger.info(f"Feature importance analysis completed using {len(importance_analysis)} methods")
        return importance_analysis
    
    def _detect_outliers(self, df: pd.DataFrame) -> Dict[str, Any]:
        \"\"\"Detect outliers in numerical features.\"\"\"
        outlier_analysis = {}
        
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        outlier_analysis['outliers'] = {}
        
        for col in numeric_cols:
            col_data = df[col].dropna()
            if len(col_data) < 10:  # Skip if too few data points
                continue
            
            # IQR method
            Q1 = col_data.quantile(0.25)
            Q3 = col_data.quantile(0.75)
            IQR = Q3 - Q1
            lower_bound = Q1 - 1.5 * IQR
            upper_bound = Q3 + 1.5 * IQR
            
            outliers = col_data[(col_data < lower_bound) | (col_data > upper_bound)]
            outlier_percentage = (len(outliers) / len(col_data)) * 100
            
            outlier_analysis['outliers'][col] = {
                'method': 'IQR',
                'outlier_count': len(outliers),
                'outlier_percentage': float(outlier_percentage),
                'lower_bound': float(lower_bound),
                'upper_bound': float(upper_bound),
                'outlier_values': outliers.head(10).tolist() if len(outliers) > 0 else []
            }
        
        logger.info(f"Outlier detection completed for {len(outlier_analysis['outliers'])} features")
        return outlier_analysis
    
    def generate_summary_report(self, analysis_results: Dict[str, Any]) -> str:
        \"\"\"Generate a summary report from analysis results.\"\"\"
        basic_info = analysis_results.get('basic_info', {})
        data_quality = analysis_results.get('data_quality', {})
        correlations = analysis_results.get('correlations', {})
        
        report = f\"\"\"
# Data Analysis Summary Report

## Dataset Overview
- **Total Records**: {basic_info.get('total_records', 'N/A'):,}
- **Total Features**: {basic_info.get('total_features', 'N/A')}
- **Memory Usage**: {basic_info.get('memory_usage', 0) / 1024 / 1024:.2f} MB

## Data Quality
- **Completeness**: {data_quality.get('completeness', {}).get('percentage', 0):.1f}%
- **Missing Values**: {data_quality.get('missing_values', {}).get('total_missing', 0):,}
- **Duplicate Rows**: {data_quality.get('duplicates', {}).get('count', 0):,}

## Key Findings
\"\"\"
        
        # Add correlations summary
        high_corrs = correlations.get('numerical', {}).get('high_correlations', [])
        if high_corrs:
            report += f\"- Found {len(high_corrs)} highly correlated feature pairs\\n\"
        
        # Add target correlations summary
        target_corrs = correlations.get('target_correlations', {})
        if target_corrs:
            top_corr_features = list(target_corrs.keys())[:5]
            report += f\"- Top features correlated with target: {', '.join(top_corr_features)}\\n\"
        
        report += \"\\n---\\nGenerated by Python IDE Data Science Template\"
        
        return report
                """.trimIndent()
            ),
            
            <!-- API:START -->
            <!-- API:DISABLED -->
            // API server for predictions
            TemplateFile(
                targetPath = "api/api_server.py",
                content = """
\"\"\"
FastAPI server for {{ PROJECT_NAME }} data science predictions.
\"\"\"

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional
import pandas as pd
import numpy as np
from contextlib import asynccontextmanager

# Import our modules
from src.config import Config
from src.models import ModelLoader

config = Config()
model_loader = ModelLoader(config)

# Request/Response models
class PredictionRequest(BaseModel):
    data: Dict[str, Any] = Field(..., description="Input data for prediction")
    
class PredictionResponse(BaseModel):
    prediction: Any
    confidence: Optional[float] = None
    probability: Optional[Dict[str, float]] = None
    model_used: str

class BatchPredictionRequest(BaseModel):
    data: List[Dict[str, Any]] = Field(..., description="Batch input data")

class BatchPredictionResponse(BaseModel):
    predictions: List[Any]
    confidences: List[Optional[float]] = None

# Global app instance
app = FastAPI(
    title="{{ PROJECT_NAME }} API",
    description="Data Science Model API",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@asynccontextmanager
async def lifespan(app: FastAPI):
    \"\"\"Application lifespan manager.\"\"\"
    # Startup
    print("Starting {{ PROJECT_NAME }} API server...")
    await model_loader.load_models()
    print("API server ready!")
    yield
    # Shutdown
    print("Shutting down {{ PROJECT_NAME }} API server...")

# Set lifespan
app.router.add_api_route("/api/v1", lambda: None, methods=["GET"])
app.router.add_event_handler("startup", lambda: None)
app.router.add_event_handler("shutdown", lambda: None)

@app.get("/", summary="Root endpoint")
async def root():
    \"\"\"Root endpoint.\"\"\"
    return {
        "app": "{{ PROJECT_NAME }}",
        "version": "1.0.0",
        "docs": "/docs",
        "status": "running"
    }

@app.get("/health", summary="Health check")
async def health_check():
    \"\"\"Health check endpoint.\"\"\"
    return {
        "status": "healthy",
        "models_loaded": model_loader.is_model_loaded(),
        "available_models": model_loader.get_available_models()
    }

@app.post("/api/v1/predict", response_model=PredictionResponse, summary="Make prediction")
async def predict(request: PredictionRequest):
    \"\"\"Make a single prediction.\"\"\"
    try:
        if not model_loader.is_model_loaded():
            raise HTTPException(status_code=503, detail="Models not loaded")
        
        # Convert request to DataFrame
        df = pd.DataFrame([request.data])
        
        # Make prediction
        result = model_loader.predict(df)
        
        return PredictionResponse(
            prediction=result['prediction'],
            confidence=result.get('confidence'),
            probability=result.get('probability'),
            model_used=result.get('model_used', 'unknown')
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")

@app.post("/api/v1/predict/batch", response_model=BatchPredictionResponse, summary="Make batch predictions")
async def predict_batch(request: BatchPredictionRequest):
    \"\"\"Make batch predictions.\"\"\"
    try:
        if not model_loader.is_model_loaded():
            raise HTTPException(status_code=503, detail="Models not loaded")
        
        # Convert request to DataFrame
        df = pd.DataFrame(request.data)
        
        # Make batch predictions
        results = model_loader.predict_batch(df)
        
        return BatchPredictionResponse(
            predictions=results['predictions'],
            confidences=results.get('confidences')
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch prediction error: {str(e)}")

@app.get("/api/v1/models", summary="List available models")
async def list_models():
    \"\"\"List available models.\"\"\"
    return {
        "available_models": model_loader.get_available_models(),
        "default_model": model_loader.get_default_model()
    }

@app.get("/api/v1/models/{model_name}/info", summary="Get model information")
async def model_info(model_name: str):
    \"\"\"Get information about a specific model.\"\"\"
    try:
        info = model_loader.get_model_info(model_name)
        return info
    except Exception as e:
        raise HTTPException(status_code=404, detail=f"Model not found: {str(e)}")

def start_api_server():
    \"\"\"Start the API server.\"\"\"
    import uvicorn
    
    uvicorn.run(
        app,
        host=config.API_HOST,
        port=config.API_PORT,
        reload=False,
        log_level="info"
    )

if __name__ == "__main__":
    start_api_server()
                """.trimIndent()
            ),
            <!-- API:END -->
            
            // Main Jupyter notebook
            TemplateFile(
                targetPath = "notebooks/01_data_exploration.ipynb",
                content = """
{
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "# {{ PROJECT_NAME }} - Data Exploration\\n",
    "\\n",
    "This notebook explores the dataset and provides insights into the data structure, \\n",
    "quality, and patterns."
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Import necessary libraries\\n",
    "import pandas as pd\\n",
    "import numpy as np\\n",
    "import matplotlib.pyplot as plt\\n",
    "import seaborn as sns\\n",
    "import warnings\\n",
    "warnings.filterwarnings('ignore')\\n",
    "\\n",
    "# Set plotting style\\n",
    "plt.style.use('seaborn-v0_8')\\n",
    "sns.set_palette(\"husl\")"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Import our custom modules\\n",
    "import sys\\n",
    "sys.path.append('../src')\\n",
    "\\n",
    "from config import Config\\n",
    "from data_loader import DataLoader\\n",
    "from data_preprocessor import DataPreprocessor\\n",
    "from analytics import DataAnalytics"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Initialize configuration and components\\n",
    "config = Config()\\n",
    "data_loader = DataLoader(config)\\n",
    "preprocessor = DataPreprocessor(config)\\n",
    "analytics = DataAnalytics(config)"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Load the dataset\\n",
    "df = data_loader.load_data()\\n",
    "print(f\"Dataset shape: {df.shape}\")\\n",
    "print(f\"Dataset info:\")\\n",
    "df.info()"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Display first few rows\\n",
    "df.head()"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Basic statistics\\n",
    "df.describe()"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Check for missing values\\n",
    "missing_data = df.isnull().sum()\\n",
    "missing_percent = (missing_data / len(df)) * 100\\n",
    "missing_df = pd.DataFrame({\\n",
    "    'Column': missing_data.index,\\n",
    "    'Missing Count': missing_data.values,\\n",
    "    'Missing %': missing_percent.values\\n",
    "})\\n",
    "missing_df = missing_df[missing_df['Missing Count'] > 0].sort_values('Missing %', ascending=False)\\n",
    "missing_df"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Comprehensive data analysis\\n",
    "analysis_results = analytics.analyze_data(df)\\n",
    "analysis_results.keys()"
   ]
  },
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "## Data Quality Summary\\n",
    "\\n",
    f\"\"\"**Total Records**: {analysis_results['basic_info']['total_records']:,}\\n",
    "**Total Features**: {analysis_results['basic_info']['total_features']}\\n",
    "**Data Completeness**: {analysis_results['data_quality']['completeness']['percentage']:.1f}%\\n",
    "**Missing Values**: {analysis_results['data_quality']['missing_values']['total_missing']:,}\"\"\""
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {},
   "outputs": [],
   "source": [
    "# Save the analysis results\\n",
    "import json\\n",
    "import os\\n",
    "\\n",
    "# Create reports directory if it doesn't exist\\n",
    "os.makedirs(config.REPORTS_DIR, exist_ok=True)\\n",
    "\\n",
    "# Save analysis results\\n",
    "with open(config.REPORTS_DIR / 'analysis_results.json', 'w') as f:\\n",
    "    json.dump(analysis_results, f, indent=2, default=str)\\n",
    "\\n",
    "print(\"Analysis results saved to:\", config.REPORTS_DIR / 'analysis_results.json')"
   ]
  },
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "## Next Steps\\n",
    "\\n",
    "1. **Data Preprocessing**: Clean and prepare the data\\n",
    "2. **Feature Engineering**: Create new features if needed\\n",
    "3. **Exploratory Data Analysis**: Deep dive into patterns\\n",
    "4. **Model Building**: Train machine learning models\\n",
    "5. **Model Evaluation**: Assess model performance"
   ]
  }
 ],
 "metadata": {
  "kernelspec": {
   "display_name": "Python 3",
   "language": "python",
   "name": "python3"
  },
  "language_info": {
   "codemirror_mode": {
    "name": "ipython",
    "version": 3
   },
   "file_extension": ".py",
   "mimetype": "text/x-python",
   "name": "python",
   "nbconvert_exporter": "python",
   "pygments_lexer": "ipython3",
   "version": "3.8.0"
  }
 },
 "nbformat": 4,
 "nbformat_minor": 4
}
                """.trimIndent()
            ),
            
            <!-- TESTS:START -->
            <!-- TESTS:DISABLED -->
            // Test files
            TemplateFile(
                targetPath = "tests/__init__.py",
                content = "# Tests package"
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "tests/test_config.py",
                content = """
import pytest
from pathlib import Path
import os
import sys

# Add src to path
sys.path.append(str(Path(__file__).parent.parent / "src"))

from config import Config

def test_config_creation():
    \"\"\"Test configuration creation.\"\"\"
    config = Config()
    assert config.PROJECT_ROOT is not None
    assert config.DATA_DIR.exists()
    assert config.MODELS_DIR.exists()

def test_directories_created():
    \"\"\"Test that required directories are created.\"\"\"
    config = Config()
    assert config.RAW_DATA_DIR.exists()
    assert config.PROCESSED_DATA_DIR.exists()
    assert config.REPORTS_DIR.exists()
    assert config.LOGS_DIR.exists()

def test_model_config():
    \"\"\"Test model configuration retrieval.\"\"\"
    config = Config()
    
    # Test RandomForest config
    rf_config = config.get_model_config("RandomForest")
    assert "n_estimators" in rf_config
    assert rf_config["n_estimators"] == 100
    
    # Test unknown model config
    unknown_config = config.get_model_config("UnknownModel")
    assert unknown_config == {}

def test_config_from_env():
    \"\"\"Test configuration from environment variables.\"\"\"
    # Set environment variables
    os.environ["TEST_SIZE"] = "0.3"
    os.environ["RANDOM_STATE"] = "123"
    os.environ["TARGET_COLUMN"] = "test_target"
    
    config = Config()
    assert config.TEST_SIZE == 0.3
    assert config.RANDOM_STATE == 123
    assert config.TARGET_COLUMN == "test_target"
    
    # Clean up
    del os.environ["TEST_SIZE"]
    del os.environ["RANDOM_STATE"]
    del os.environ["TARGET_COLUMN"]
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "tests/test_data_loader.py",
                content = """
import pytest
import pandas as pd
import numpy as np
import sys
from pathlib import Path

# Add src to path
sys.path.append(str(Path(__file__).parent.parent / "src"))

from config import Config
from data_loader import DataLoader

@pytest.fixture
def config():
    return Config()

@pytest.fixture
def data_loader(config):
    return DataLoader(config)

def test_data_loader_creation(data_loader):
    \"\"\"Test data loader creation.\"\"\"
    assert data_loader.config is not None
    assert isinstance(data_loader.data_cache, dict)

def test_synthetic_data_creation(data_loader):
    \"\"\"Test synthetic data creation.\"\"\"
    df = data_loader.create_synthetic_data(n_samples=100, n_features=3)
    
    assert df.shape[0] == 100
    assert df.shape[1] == 4  # 3 features + 1 target
    assert 'target' in df.columns
    assert 'feature_0' in df.columns
    assert 'feature_1' in df.columns
    assert 'feature_2' in df.columns

def test_synthetic_data_has_missing_values(data_loader):
    \"\"\"Test that synthetic data includes some missing values.\"\"\"
    df = data_loader.create_synthetic_data(n_samples=1000)
    
    # Should have some missing values in feature_0
    missing_count = df['feature_0'].isnull().sum()
    assert missing_count > 0
    assert missing_count < len(df) * 0.1  # Less than 10% missing

def test_data_split(data_loader):
    \"\"\"Test data splitting functionality.\"\"\"
    df = data_loader.create_synthetic_data(n_samples=100)
    splits = data_loader.split_data(df)
    
    assert 'train' in splits
    assert 'validation' in splits
    assert 'test' in splits
    
    # Check split sizes
    train_X, train_y = splits['train']
    val_X, val_y = splits['validation']
    test_X, test_y = splits['test']
    
    total_samples = len(df)
    expected_train = int(total_samples * 0.7)
    expected_val = int(total_samples * 0.1)
    expected_test = int(total_samples * 0.2)
    
    assert len(train_X) == expected_train
    assert len(val_X) == expected_val
    assert len(test_X) == expected_test

def test_sklearn_dataset_loading(data_loader):
    \"\"\"Test loading sklearn datasets.\"\"\"
    # Test iris dataset
    df = data_loader.load_data("sklearn:iris")
    
    assert df.shape[0] == 150  # Iris has 150 samples
    assert 'target' in df.columns
    assert 'target_names' in df.attrs
    
    # Check that target has correct values
    assert set(df['target'].unique()) == {0, 1, 2}
    assert len(df.attrs['target_names']) == 3

def test_caching_functionality(data_loader):
    \"\"\"Test data caching.\"\"\"
    # Load data twice
    df1 = data_loader.load_data("sklearn:iris")
    df2 = data_loader.load_data("sklearn:iris")
    
    # Should be same object from cache
    assert df1 is df2
    assert "sklearn:iris" in data_loader.data_cache

def test_data_loading_error_handling(data_loader):
    \"\"\"Test error handling for invalid data sources.\"\"\"
    # Test with non-existent file
    with pytest.raises(FileNotFoundError):
        data_loader.load_data("non_existent_file.csv")
    
    # Test with unknown sklearn dataset
    with pytest.raises(ValueError):
        data_loader.load_data("sklearn:unknown_dataset")
                """.trimIndent()
            ),
            <!-- TESTS:END -->
            
            // Environment configuration
            TemplateFile(
                targetPath = ".env.example",
                content = """
# Data Science Project Configuration

# Data Configuration
DATA_SOURCE=sklearn:iris
TARGET_COLUMN=target
TEST_SIZE=0.2
VALIDATION_SIZE=0.1
RANDOM_STATE=42

# Preprocessing Configuration
HANDLE_MISSING=mean
SCALE_FEATURES=true
ENCODE_CATEGORICAL=true

# Model Configuration
CV_FOLDS=5

# Visualization Configuration
PLOT_STYLE=seaborn
FIGURE_SIZE=(12, 8)
FIGURE_DPI=150

# API Configuration
<!-- API:START -->
<!-- API:DISABLED -->
ENABLE_API=true
API_HOST=0.0.0.0
API_PORT=8000
<!-- API:END -->

# Logging Configuration
LOG_LEVEL=INFO
                """.trimIndent()
            ),
            
            // README file
            TemplateFile(
                targetPath = "README.md",
                content = """
# {{ PROJECT_NAME }}

A comprehensive data science and machine learning project built with the Python IDE template.

## Features

- **Data Processing**: Automated data loading, cleaning, and preprocessing
- **Statistical Analysis**: Comprehensive descriptive statistics and data quality assessment
- **Machine Learning**: Multiple ML algorithms with automatic training and evaluation
- **Visualization**: Automated chart generation for data exploration
- **Jupyter Notebooks**: Interactive analysis and model development
- **API Endpoints**: RESTful API for model predictions

<!-- API:START -->
<!-- API:DISABLED -->
- **FastAPI Integration**: Modern web framework for model serving
- **Batch Predictions**: Support for single and batch predictions
- **Model Management**: Automatic model loading and versioning
<!-- API:END -->

## Project Structure

```
{{ PROJECT_NAME }}/
├── src/                    # Source code
│   ├── config.py          # Configuration settings
│   ├── data_loader.py     # Data loading utilities
│   ├── data_preprocessor.py  # Data preprocessing
│   ├── analytics.py       # Statistical analysis
│   ├── visualization.py   # Plotting utilities
│   ├── models.py          # ML model training
│   └── evaluation.py      # Model evaluation
├── data/                  # Data directory
│   ├── raw/              # Raw data files
│   └── processed/        # Processed data files
├── models/                # Trained models
├── reports/               # Analysis reports
│   └── plots/           # Generated visualizations
├── notebooks/             # Jupyter notebooks
├── logs/                  # Application logs
├── tests/                 # Test files
├── api/                   # FastAPI server
│   └── api_server.py    # API endpoints
├── main.py               # Main application entry point
├── requirements.txt      # Python dependencies
└── .env.example         # Environment configuration
```

## Quick Start

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Environment Setup
```bash
cp .env.example .env
# Edit .env file with your configuration
```

### 3. Run Analysis
```bash
python main.py
```

### 4. Jupyter Notebook
```bash
jupyter notebook
# Open notebooks/01_data_exploration.ipynb
```

## Data Sources

### Built-in Datasets
- `sklearn:iris` - Iris flower dataset
- `sklearn:wine` - Wine recognition dataset
- `sklearn:breast_cancer` - Breast cancer dataset

### File Formats
- CSV (`.csv`)
- Excel (`.xlsx`, `.xls`)
- JSON (`.json`)
- Parquet (`.parquet`)

### URLs
- HTTP/HTTPS URLs to CSV or JSON files

## Configuration

### Environment Variables

```bash
# Data Configuration
DATA_SOURCE=sklearn:iris
TARGET_COLUMN=target
TEST_SIZE=0.2
VALIDATION_SIZE=0.1
RANDOM_STATE=42

# Preprocessing
HANDLE_MISSING=mean      # drop, mean, median, mode, knn
SCALE_FEATURES=true      # true, false
ENCODE_CATEGORICAL=true  # true, false

# Models
CV_FOLDS=5              # Cross-validation folds

<!-- API:START -->
<!-- API:DISABLED -->
# API
ENABLE_API=true
API_HOST=0.0.0.0
API_PORT=8000
<!-- API:END -->
```

## API Documentation

<!-- API:START -->
<!-- API:DISABLED -->
### Starting the API Server

```bash
python api/api_server.py
```

Or run with uvicorn:
```bash
uvicorn api.api_server:app --host 0.0.0.0 --port 8000 --reload
```

### API Endpoints

#### Health Check
- `GET /health` - Check API status

#### Predictions
- `POST /api/v1/predict` - Single prediction
- `POST /api/v1/predict/batch` - Batch predictions

#### Model Information
- `GET /api/v1/models` - List available models
- `GET /api/v1/models/{model_name}/info` - Model details

### API Usage Example

```python
import requests

# Make a prediction
response = requests.post('http://localhost:8000/api/v1/predict', json={
    'data': {
        'feature1': 1.0,
        'feature2': 2.0,
        'feature3': 3.0
    }
})

result = response.json()
print(result['prediction'])
print(result['confidence'])
```
<!-- API:END -->

## Data Processing Pipeline

### 1. Data Loading
- Automatic format detection
- Built-in dataset support
- URL and file loading
- Data caching for performance

### 2. Data Preprocessing
- Missing value handling (drop, mean, median, mode, KNN)
- Categorical encoding (one-hot, label)
- Feature scaling (StandardScaler)
- Outlier detection

### 3. Statistical Analysis
- Descriptive statistics
- Data quality assessment
- Correlation analysis
- Distribution analysis
- Feature importance ranking

### 4. Visualization
- Distribution plots
- Correlation heatmaps
- Feature importance charts
- Outlier detection plots

## Machine Learning

### Supported Algorithms
- **Random Forest**: Ensemble method for classification/regression
- **Gradient Boosting**: Sequential ensemble method
- **Logistic Regression**: Linear classification
- **Support Vector Machine**: Kernel-based method
- **XGBoost**: Gradient boosting library

### Model Training Process
1. **Data Splitting**: Train/validation/test split
2. **Cross-Validation**: K-fold cross-validation
3. **Hyperparameter Tuning**: Grid search optimization
4. **Model Evaluation**: Multiple metrics assessment
5. **Model Persistence**: Automatic model saving

### Model Evaluation Metrics
- **Classification**: Accuracy, Precision, Recall, F1-Score, ROC-AUC
- **Regression**: MSE, MAE, R², RMSE

## Jupyter Notebooks

### Notebook Structure
1. **01_data_exploration.ipynb** - Data loading and basic analysis
2. **02_preprocessing.ipynb** - Data cleaning and preprocessing
3. **03_eda.ipynb** - Exploratory data analysis
4. **04_modeling.ipynb** - Model training and evaluation
5. **05_deployment.ipynb** - Model deployment and API usage

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src

# Run specific test file
pytest tests/test_config.py
```

## Generated Files

After running the analysis, the following files are generated:

### Data Files
- `data/raw/` - Original data files
- `data/processed/` - Cleaned and preprocessed data

### Model Files
- `models/` - Trained machine learning models (joblib format)

### Reports
- `reports/analysis_report.md` - Comprehensive analysis report
- `reports/analysis_results.json` - Detailed analysis results
- `reports/plots/` - Generated visualizations

### Logs
- `logs/analysis.log` - Application logs

## Customization

### Adding Custom Datasets
```python
from src.data_loader import DataLoader

loader = DataLoader(config)
df = loader.load_data("path/to/your/data.csv")
```

### Custom Models
```python
from src.models import ModelTrainer

trainer = ModelTrainer(config)
# Add your custom model configuration
```

### Custom Preprocessing
```python
from src.data_preprocessor import DataPreprocessor

preprocessor = DataPreprocessor(config)
# Add your custom preprocessing steps
```

## Best Practices

1. **Version Control**: Use git for tracking changes
2. **Documentation**: Document your analysis and findings
3. **Reproducibility**: Set random seeds for consistent results
4. **Testing**: Write tests for critical functions
5. **Logging**: Use appropriate log levels
6. **Configuration**: Use environment variables for settings

## Performance Optimization

- **Memory Usage**: Process large datasets in chunks
- **Parallel Processing**: Use multiprocessing for data operations
- **Caching**: Cache intermediate results
- **Data Types**: Use appropriate data types to reduce memory

## Deployment

### Production Considerations
1. **Environment Variables**: Secure configuration management
2. **Model Versioning**: Track model versions and performance
3. **Monitoring**: Track model performance in production
4. **A/B Testing**: Test model improvements safely

Built with ❤️ using the Python IDE
                """.trimIndent()
            )
        )
    }
    
    override fun createProjectStructure(projectDir: File) {
        // Create data science project structure
        val directories = listOf(
            "src",
            "data/raw",
            "data/processed",
            "models",
            "reports",
            "reports/plots",
            "notebooks",
            "logs",
            "tests",
            ".pythonide"
        )
        
        directories.forEach { dir ->
            File(projectDir, dir).mkdirs()
        }
        
        <!-- API:START -->
        <!-- API:DISABLED -->
        // Create API directory
        File(projectDir, "api").mkdirs()
        <!-- API:END -->
        
        // Create __init__.py files for Python packages
        val initFiles = listOf(
            "src/__init__.py",
            "tests/__init__.py",
            "api/__init__.py"
        )
        
        initFiles.forEach { file ->
            File(projectDir, file).createNewFile()
        }
        
        // Create sample data directory
        File(projectDir, "data/sample").mkdirs()
    }
    
    override fun getCustomizationOptions(): TemplateCustomizationOptions {
        return TemplateCustomizationOptions(
            enableDatabase = false,
            enableAuthentication = false,
            enableApi = true,
            enableTests = true,
            enableDocumentation = true,
            databaseType = "none",
            additionalDependencies = listOf(
                "jupyter==1.0.0",
                "plotly==5.17.0",
                "seaborn==0.13.0",
                "xgboost==2.0.3",
                "lightgbm==4.1.0"
            )
        )
    }
}