package com.pythonide.templates.templates

import com.pythonide.templates.*
import java.io.File

/**
 * Flask web application template
 */
class FlaskTemplate : ProjectTemplate {
    
    override fun getTemplateInfo(): ProjectTemplate {
        return ProjectTemplate(
            id = "flask",
            name = "Flask Web Application",
            description = "A lightweight WSGI web application framework",
            category = "Web Development",
            icon = "flask",
            version = "1.0.0",
            author = "Python IDE",
            tags = listOf("web", "flask", "python", "backend", "api")
        )
    }
    
    override fun getTemplateFiles(): List<TemplateFile> {
        return listOf(
            // Main application file
            TemplateFile(
                targetPath = "app.py",
                content = """
#!/usr/bin/env python3
"""
                .trimIndent() + """

from flask import Flask, render_template, jsonify, request
import os
import sys

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY') or 'dev-secret-key'

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/health')
def health_check():
    return jsonify({
        'status': 'healthy',
        'message': 'Flask app is running!'
    })

@app.route('/api/hello/<name>')
def hello(name):
    return jsonify({
        'message': f'Hello, {name}!'
    })

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(debug=True, host='0.0.0.0', port=port)
                """.trimIndent()
            ),
            
            // Requirements file
            TemplateFile(
                targetPath = "requirements.txt",
                content = """
Flask==2.3.3
Werkzeug==2.3.7
Jinja2==3.1.2
MarkupSafe==2.1.3
itsdangerous==2.1.2
click==8.1.7
blinker==1.6.3
gunicorn==21.2.0

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
Flask-SQLAlchemy==3.0.5
Flask-Migrate==4.0.5
<!-- DATABASE:END -->

<!-- API:START -->
<!-- API:DISABLED -->
Flask-RESTful==0.3.10
Flask-CORS==4.0.0
<!-- API:END -->

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
Flask-Login==0.6.3
Flask-WTF==1.1.1
Flask-Bcrypt==1.0.1
<!-- AUTH:END -->
                """.trimIndent()
            ),
            
            // HTML template
            TemplateFile(
                targetPath = "templates/base.html",
                content = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{% block title %}{{ title or "Flask App" }}{% endblock %}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="/">{{ PROJECT_NAME }}</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav ms-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="/">Home</a>
                    </li>
                    <!-- API:START -->
                    <!-- API:DISABLED -->
                    <li class="nav-item">
                        <a class="nav-link" href="/api/health">API</a>
                    </li>
                    <!-- API:END -->
                </ul>
            </div>
        </div>
    </nav>

    <main class="container mt-4">
        {% block content %}{% endblock %}
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    {% block scripts %}{% endblock %}
</body>
</html>
                """.trimIndent()
            ),
            
            // Home page template
            TemplateFile(
                targetPath = "templates/index.html",
                content = """
{% extends "base.html" %}

{% block title %}{{ PROJECT_NAME }} - Home{% endblock %}

{% block content %}
<div class="row">
    <div class="col-12">
        <div class="jumbotron">
            <h1 class="display-4">Welcome to {{ PROJECT_NAME }}!</h1>
            <p class="lead">This is a Flask web application created from the Python IDE template.</p>
            <hr class="my-4">
            <p>Your Flask app is now running successfully.</p>
            <a class="btn btn-primary btn-lg" href="/api/health" role="button">Check API Health</a>
        </div>
    </div>
</div>

<div class="row mt-4">
    <div class="col-md-4">
        <div class="card">
            <div class="card-body">
                <h5 class="card-title">Getting Started</h5>
                <p class="card-text">Modify the app.py file to add your own routes and functionality.</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card">
            <div class="card-body">
                <h5 class="card-title">Templates</h5>
                <p class="card-text">Add your HTML templates in the templates/ directory.</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card">
            <div class="card-body">
                <h5 class="card-title">Static Files</h5>
                <p class="card-text">Add CSS, JavaScript, and images in the static/ directory.</p>
            </div>
        </div>
    </div>
</div>
{% endblock %}
                """.trimIndent()
            ),
            
            // Static files structure
            TemplateFile(
                targetPath = "static/css/style.css",
                content = """
/* Custom styles for {{ PROJECT_NAME }} */

.jumbotron {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 3rem 2rem;
    margin-bottom: 2rem;
    border-radius: 0.5rem;
}

.card {
    border: none;
    box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
    transition: transform 0.2s;
}

.card:hover {
    transform: translateY(-5px);
}
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "static/js/main.js",
                content = """
// Main JavaScript file for {{ PROJECT_NAME }}

document.addEventListener('DOMContentLoaded', function() {
    console.log('{{ PROJECT_NAME }} loaded successfully!');
    
    // Add any initialization code here
    initializeApp();
});

function initializeApp() {
    // Initialize components
    console.log('App initialized');
}
                """.trimIndent()
            ),
            
            <!-- DATABASE:START -->
            <!-- DATABASE:DISABLED -->
            // Database models (if database is enabled)
            TemplateFile(
                targetPath = "models.py",
                content = """
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from app import app

db = SQLAlchemy(app)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<User {self.username}>'
    
    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'email': self.email,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

class {{ PROJECT_NAME.title() }}Model(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    def __repr__(self):
        return f'<{{ PROJECT_NAME.title() }}Model {self.name}>'
    
    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'description': self.description,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }
                """.trimIndent()
            ),
            
            // Database routes
            TemplateFile(
                targetPath = "database/routes.py",
                content = """
from flask import Blueprint, request, jsonify
from models import db, User, {{ PROJECT_NAME.title() }}Model

database_bp = Blueprint('database', __name__)

@database_bp.route('/api/users', methods=['GET'])
def get_users():
    users = User.query.all()
    return jsonify([user.to_dict() for user in users])

@database_bp.route('/api/users', methods=['POST'])
def create_user():
    data = request.get_json()
    user = User(username=data['username'], email=data['email'])
    db.session.add(user)
    db.session.commit()
    return jsonify(user.to_dict()), 201

@database_bp.route('/api/entities', methods=['GET'])
def get_entities():
    entities = {{ PROJECT_NAME.title() }}Model.query.all()
    return jsonify([entity.to_dict() for entity in entities])

@database_bp.route('/api/entities', methods=['POST'])
def create_entity():
    data = request.get_json()
    entity = {{ PROJECT_NAME.title() }}Model(name=data['name'], description=data.get('description'))
    db.session.add(entity)
    db.session.commit()
    return jsonify(entity.to_dict()), 201
                """.trimIndent()
            ),
            <!-- DATABASE:END -->
            
            <!-- API:START -->
            <!-- API:DISABLED -->
            // API routes
            TemplateFile(
                targetPath = "api/routes.py",
                content = """
from flask import Blueprint, request, jsonify
from flask_restful import Resource, Api

api_bp = Blueprint('api', __name__)
api = Api(api_bp)

class HealthResource(Resource):
    def get(self):
        return {
            'status': 'healthy',
            'message': 'API is running',
            'version': '1.0.0'
        }

class HelloResource(Resource):
    def get(self, name):
        return {
            'message': f'Hello, {name}!',
            'name': name
        }

class ProjectResource(Resource):
    def get(self):
        return {
            'name': '{{ PROJECT_NAME }}',
            'description': '{{ TEMPLATE_NAME }} application',
            'version': '{{ TEMPLATE_VERSION }}'
        }

# Register API resources
api.add_resource(HealthResource, '/api/health')
api.add_resource(HelloResource, '/api/hello/<string:name>')
api.add_resource(ProjectResource, '/api/project')
                """.trimIndent()
            ),
            <!-- API:END -->
            
            <!-- AUTH:START -->
            <!-- AUTH:DISABLED -->
            // Authentication routes
            TemplateFile(
                targetPath = "auth/routes.py",
                content = """
from flask import Blueprint, request, jsonify, current_app
from flask_login import LoginManager, login_user, logout_user, login_required, current_user
from werkzeug.security import generate_password_hash, check_password_hash
from models import db, User

auth_bp = Blueprint('auth', __name__)
login_manager = LoginManager()

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

@auth_bp.route('/api/register', methods=['POST'])
def register():
    data = request.get_json()
    
    # Check if user already exists
    if User.query.filter_by(username=data['username']).first():
        return jsonify({'error': 'Username already exists'}), 400
    
    if User.query.filter_by(email=data['email']).first():
        return jsonify({'error': 'Email already exists'}), 400
    
    # Create new user
    user = User(
        username=data['username'],
        email=data['email']
    )
    user.set_password(data['password'])
    
    db.session.add(user)
    db.session.commit()
    
    return jsonify({'message': 'User created successfully'}), 201

@auth_bp.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    user = User.query.filter_by(username=data['username']).first()
    
    if user and user.check_password(data['password']):
        login_user(user)
        return jsonify({'message': 'Logged in successfully'}), 200
    
    return jsonify({'error': 'Invalid credentials'}), 401

@auth_bp.route('/api/logout', methods=['POST'])
@login_required
def logout():
    logout_user()
    return jsonify({'message': 'Logged out successfully'}), 200

@auth_bp.route('/api/user')
@login_required
def get_current_user():
    return jsonify(current_user.to_dict())
                """.trimIndent()
            ),
            <!-- AUTH:END -->
            
            // Main app configuration with blueprints
            TemplateFile(
                targetPath = "config.py",
                content = """
import os
from datetime import timedelta

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key'
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or 'sqlite:///app.db'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    
    <!-- DATABASE:START -->
    <!-- DATABASE:DISABLED -->
    # Database configuration
    POSTGRES_USER = os.environ.get('POSTGRES_USER')
    POSTGRES_PASSWORD = os.environ.get('POSTGRES_PASSWORD')
    POSTGRES_DB = os.environ.get('POSTGRES_DB')
    POSTGRES_HOST = os.environ.get('POSTGRES_HOST')
    POSTGRES_PORT = os.environ.get('POSTGRES_PORT')
    
    if POSTGRES_USER:
        SQLALCHEMY_DATABASE_URI = f'postgresql://{POSTGRES_USER}:{POSTGRES_PASSWORD}@{POSTGRES_HOST}:{POSTGRES_PORT}/{POSTGRES_DB}'
    <!-- DATABASE:END -->
    
    <!-- AUTH:START -->
    <!-- AUTH:DISABLED -->
    # Session configuration
    PERMANENT_SESSION_LIFETIME = timedelta(hours=24)
    SESSION_COOKIE_SECURE = False  # Set to True in production with HTTPS
    SESSION_COOKIE_HTTPONLY = True
    SESSION_COOKIE_SAMESITE = 'Lax'
    <!-- AUTH:END -->

class DevelopmentConfig(Config):
    DEBUG = True

class ProductionConfig(Config):
    DEBUG = False

class TestingConfig(Config):
    TESTING = True
    SQLALCHEMY_DATABASE_URI = 'sqlite:///:memory:'

config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'testing': TestingConfig,
    'default': DevelopmentConfig
}
                """.trimIndent()
            ),
            
            // README file
            TemplateFile(
                targetPath = "README.md",
                content = """
# {{ PROJECT_NAME }}

A Flask web application created from the Python IDE template.

## Features

- Flask web framework
- Bootstrap frontend
- RESTful API endpoints
- Template inheritance with Jinja2
- Static file management

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
- SQLAlchemy database integration
- User management models
- Database migration support
<!-- DATABASE:END -->

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
- User authentication
- Login/logout functionality
- Session management
<!-- AUTH:END -->

<!-- API:START -->
<!-- API:DISABLED -->
- Flask-RESTful API
- CORS support
- API versioning
<!-- API:END -->

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Initialize the database:
```bash
flask db init
flask db migrate
flask db upgrade
```

3. Run the application:
```bash
python app.py
```

The application will be available at `http://localhost:5000`

## Project Structure

```
{{ PROJECT_NAME }}/
├── app.py              # Main application file
├── config.py           # Application configuration
├── models.py           # Database models
├── requirements.txt    # Python dependencies
├── templates/          # HTML templates
│   ├── base.html
│   └── index.html
├── static/             # Static files
│   ├── css/
│   └── js/
├── database/           # Database routes (if enabled)
├── auth/               # Authentication routes (if enabled)
├── api/                # API routes (if enabled)
└── .pythonide/         # IDE project configuration
```

## Environment Variables

- `FLASK_ENV`: Environment (development/production)
- `SECRET_KEY`: Application secret key
- `DATABASE_URL`: Database connection string
- `PORT`: Application port (default: 5000)

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
## Database

The application uses SQLAlchemy ORM with the following models:

- `User`: User accounts and authentication
- `{{ PROJECT_NAME.title() }}Model`: Main application entities

Run migrations to create database tables:
```bash
flask db migrate -m "Initial migration"
flask db upgrade
```
<!-- DATABASE:END -->

## API Endpoints

- `GET /`: Home page
- `GET /api/health`: Health check
- `GET /api/hello/<name>`: Hello endpoint

<!-- API:START -->
<!-- API:DISABLED -->
- `GET /api/project`: Project information
- `GET /api/users`: Get all users
- `POST /api/users`: Create new user
<!-- API:END -->

## Development

To modify the application:

1. Edit `app.py` to add new routes
2. Modify templates in the `templates/` directory
3. Add static files to the `static/` directory
4. Update `requirements.txt` for new dependencies

## Deployment

For production deployment:

1. Set `FLASK_ENV=production`
2. Use a production WSGI server like Gunicorn
3. Configure proper database settings
4. Set secure environment variables

Built with ❤️ using the Python IDE
                """.trimIndent()
            ),
            
            <!-- TESTS:START -->
            <!-- TESTS:DISABLED -->
            // Test files
            TemplateFile(
                targetPath = "tests/test_app.py",
                content = """
import pytest
import tempfile
import os
from app import app, db
from models import User

@pytest.fixture
def client():
    app.config['TESTING'] = True
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
    
    with app.test_client() as client:
        with app.app_context():
            db.create_all()
            yield client

def test_index_page(client):
    """Test the index page loads successfully"""
    response = client.get('/')
    assert response.status_code == 200
    assert b'Welcome to' in response.data

def test_health_check(client):
    """Test the health check endpoint"""
    response = client.get('/api/health')
    assert response.status_code == 200
    data = response.get_json()
    assert data['status'] == 'healthy'

def test_hello_endpoint(client):
    """Test the hello endpoint with a name"""
    response = client.get('/api/hello/World')
    assert response.status_code == 200
    data = response.get_json()
    assert data['message'] == 'Hello, World!'

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
def test_create_user(client):
    """Test creating a new user"""
    response = client.post('/api/users', 
        json={'username': 'testuser', 'email': 'test@example.com'})
    assert response.status_code == 201
    data = response.get_json()
    assert data['username'] == 'testuser'
    assert data['email'] == 'test@example.com'

def test_get_users(client):
    """Test getting all users"""
    # Create a test user first
    with client.application.app_context():
        user = User(username='testuser', email='test@example.com')
        db.session.add(user)
        db.session.commit()
    
    response = client.get('/api/users')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data) == 1
    assert data[0]['username'] == 'testuser'
<!-- DATABASE:END -->
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "tests/conftest.py",
                content = """
import pytest
import os
import tempfile
from app import create_app, db

@pytest.fixture
def app():
    app = create_app('testing')
    
    with app.app_context():
        db.create_all()
        yield app
        db.drop_all()

@pytest.fixture
def client(app):
    return app.test_client()
                """.trimIndent()
            ),
            <!-- TESTS:END -->
            
            // Docker support
            TemplateFile(
                targetPath = "Dockerfile",
                content = """
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \\
    gcc \\
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create non-root user
RUN useradd --create-home --shell /bin/bash app \\
    && chown -R app:app /app
USER app

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \\
    CMD curl -f http://localhost:5000/api/health || exit 1

# Run the application
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "4", "app:app"]
                """.trimIndent()
            ),
            
            // Docker Compose for development
            TemplateFile(
                targetPath = "docker-compose.yml",
                content = """
version: '3.8'

services:
  app:
    build: .
    ports:
      - "5000:5000"
    environment:
      - FLASK_ENV=development
      - SECRET_KEY=dev-secret-key
    volumes:
      - .:/app
    depends_on:
      database

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
  database:
    image: postgres:15
    environment:
      POSTGRES_DB: {{ PROJECT_NAME.lower() }}
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
<!-- DATABASE:END -->

volumes:
<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
  postgres_data:
<!-- DATABASE:END -->
  app_data:
                """.trimIndent()
            )
        )
    }
    
    override fun createProjectStructure(projectDir: File) {
        // Create basic Flask directory structure
        val directories = listOf(
            "templates",
            "static/css",
            "static/js",
            "static/images",
            "instance",
            ".pythonide"
        )
        
        directories.forEach { dir ->
            File(projectDir, dir).mkdirs()
        }
        
        <!-- AUTH:START -->
        <!-- AUTH:DISABLED -->
        // Create auth directory if authentication is enabled
        File(projectDir, "auth").mkdirs()
        <!-- AUTH:END -->
        
        <!-- DATABASE:START -->
        <!-- DATABASE:DISABLED -->
        // Create database directory
        File(projectDir, "database").mkdirs()
        <!-- DATABASE:END -->
        
        <!-- API:START -->
        <!-- API:DISABLED -->
        // Create API directory
        File(projectDir, "api").mkdirs()
        <!-- API:END -->
        
        <!-- TESTS:START -->
        <!-- TESTS:DISABLED -->
        // Create tests directory
        File(projectDir, "tests").mkdirs()
        File(projectDir, "tests/__pycache__").mkdirs()
        <!-- TESTS:END -->
    }
    
    override fun getCustomizationOptions(): TemplateCustomizationOptions {
        return TemplateCustomizationOptions(
            enableDatabase = false,
            enableAuthentication = false,
            enableApi = false,
            enableTests = true,
            enableDocumentation = false,
            databaseType = "sqlite",
            additionalDependencies = listOf(
                "python-dotenv==1.0.0",
                "flask-debugtoolbar==4.0.0"
            )
        )
    }
}