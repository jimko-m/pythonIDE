package com.pythonide.templates.templates

import com.pythonide.templates.*
import java.io.File

/**
 * FastAPI web framework template
 */
class FastAPITemplate : ProjectTemplate {
    
    override fun getTemplateInfo(): ProjectTemplate {
        return ProjectTemplate(
            id = "fastapi",
            name = "FastAPI Modern API",
            description = "High-performance web framework for building APIs with Python 3.6+",
            category = "Web Development",
            icon = "fastapi",
            version = "1.0.0",
            author = "Python IDE",
            tags = listOf("api", "fastapi", "python", "async", "pydantic", "openapi")
        )
    }
    
    override fun getTemplateFiles(): List<TemplateFile> {
        return listOf(
            // Main FastAPI application
            TemplateFile(
                targetPath = "main.py",
                content = """
#!/usr/bin/env python3
\"\"\"
{{ PROJECT_NAME }} - FastAPI Application
\"\"\"

import uvicorn
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from contextlib import asynccontextmanager
from typing import List, Optional
import asyncio

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
from database.session import engine
from database import models
from database.database import SessionLocal, engine
<!-- DATABASE:END -->

from routers import (
    health,
    <!-- AUTH:START -->
    <!-- AUTH:DISABLED -->
    auth,
    users,
    <!-- AUTH:END -->
    <!-- DATABASE:START -->
    <!-- DATABASE:DISABLED -->
    projects,
    items,
    <!-- DATABASE:END -->
    api
)
from auth.auth_handler import get_current_user
from auth.schemas import User

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
models.Base.metadata.create_all(bind=engine)
<!-- DATABASE:END -->

@asynccontextmanager
async def lifespan(app: FastAPI):
    \"\"\"Application lifespan manager.\"\"\"
    # Startup
    print("Starting up {{ PROJECT_NAME }}...")
    yield
    # Shutdown
    print("Shutting down {{ PROJECT_NAME }}...")

# Create FastAPI application
app = FastAPI(
    title="{{ PROJECT_NAME }}",
    description="A FastAPI application built with Python IDE template",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Add middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.add_middleware(GZipMiddleware, minimum_size=1000)

# Include routers
app.include_router(health.router, prefix="/health", tags=["Health"])

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(users.router, prefix="/users", tags=["Users"])
<!-- AUTH:END -->

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
app.include_router(projects.router, prefix="/projects", tags=["Projects"])
app.include_router(items.router, prefix="/items", tags=["Items"])
<!-- DATABASE:END -->

app.include_router(api.router, prefix="/api/v1", tags=["API"])

@app.get("/", summary="Root endpoint")
async def root():
    \"\"\"Root endpoint with basic app information.\"\"\"
    return {
        "app": "{{ PROJECT_NAME }}",
        "version": "1.0.0",
        "docs": "/docs",
        "redoc": "/redoc",
        "health": "/health"
    }

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
# Protected route example
@app.get("/protected", response_model=dict)
async def protected_route(current_user: User = Depends(get_current_user)):
    \"\"\"Example protected route that requires authentication.\"\"\"
    return {
        "message": f"Hello {current_user.username}!",
        "user": current_user.dict()
    }
<!-- DATABASE:END -->

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
                """.trimIndent()
            ),
            
            // FastAPI requirements
            TemplateFile(
                targetPath = "requirements.txt",
                content = """
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
python-multipart==0.0.6
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-dotenv==1.0.0

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
sqlalchemy==2.0.23
alembic==1.13.0
psycopg2-binary==2.9.9
<!-- DATABASE:END -->

<!-- API:START -->
<!-- API:DISABLED -->
fastapi-utils==0.2.1
<!-- API:END -->
                """.trimIndent()
            ),
            
            <!-- AUTH:START -->
            <!-- AUTH:DISABLED -->
            // Authentication schemas
            TemplateFile(
                targetPath = "auth/schemas.py",
                content = """
from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime

class UserBase(BaseModel):
    username: str
    email: EmailStr
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    is_active: bool = True

class UserCreate(UserBase):
    password: str

class UserUpdate(BaseModel):
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    is_active: Optional[bool] = None

class User(UserBase):
    id: int
    created_at: datetime
    
    class Config:
        from_attributes = True

class UserLogin(BaseModel):
    username: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str
    expires_in: int

class TokenData(BaseModel):
    username: Optional[str] = None
                """.trimIndent()
            ),
            
            // Authentication handler
            TemplateFile(
                targetPath = "auth/auth_handler.py",
                content = """
from datetime import datetime, timedelta
from typing import Optional
from jose import JWTError, jwt
from passlib.context import CryptContext
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from database.database import get_db
from database import models
from auth.schemas import TokenData

# Security configuration
SECRET_KEY = "your-secret-key-change-in-production"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# HTTP Bearer token security
security = HTTPBearer()

def verify_password(plain_password: str, hashed_password: str) -> bool:
    \"\"\"Verify a password against its hash.\"\"\"
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    \"\"\"Hash a password.\"\"\"
    return pwd_context.hash(password)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    \"\"\"Create a JWT access token.\"\"\"
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def verify_token(token: str) -> Optional[TokenData]:
    \"\"\"Verify and decode JWT token.\"\"\"
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            return None
        return TokenData(username=username)
    except JWTError:
        return None

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> models.User:
    \"\"\"Get current authenticated user.\"\"\"
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    
    token_data = verify_token(credentials.credentials)
    if token_data is None:
        raise credentials_exception
    
    user = db.query(models.User).filter(models.User.username == token_data.username).first()
    if user is None:
        raise credentials_exception
    
    return user

def get_current_active_user(current_user: models.User = Depends(get_current_user)):
    \"\"\"Get current active user.\"\"\"
    if not current_user.is_active:
        raise HTTPException(status_code=400, detail="Inactive user")
    return current_user
                """.trimIndent()
            ),
            <!-- AUTH:END -->
            
            <!-- DATABASE:START -->
            <!-- DATABASE:DISABLED -->
            // Database models
            TemplateFile(
                targetPath = "database/models.py",
                content = """
from sqlalchemy import Column, Integer, String, Text, DateTime, Boolean, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from datetime import datetime

Base = declarative_base()

class User(Base):
    \"\"\"User model for authentication.\"\"\"
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    first_name = Column(String(50))
    last_name = Column(String(50))
    is_active = Column(Boolean, default=True)
    is_superuser = Column(Boolean, default=False)
    created_at = Column(DateTime, default=func.now())
    
    # Relationships
    projects = relationship("Project", back_populates="owner")
    
    def __repr__(self):
        return f"<User(id={self.id}, username='{self.username}')>"

class Project(Base):
    \"\"\"Main project model.\"\"\"
    __tablename__ = "projects"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False, index=True)
    description = Column(Text)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    is_public = Column(Boolean, default=False)
    status = Column(String(20), default="active")  # active, archived, completed
    created_at = Column(DateTime, default=func.now())
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now())
    
    # Relationships
    owner = relationship("User", back_populates="projects")
    items = relationship("ProjectItem", back_populates="project", cascade="all, delete-orphan")
    
    def __repr__(self):
        return f"<Project(id={self.id}, name='{self.name}')>"

class ProjectItem(Base):
    \"\"\"Items associated with projects.\"\"\"
    __tablename__ = "project_items"
    
    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    title = Column(String(200), nullable=False, index=True)
    content = Column(Text)
    category = Column(String(50))  # task, note, resource, etc.
    priority = Column(String(10), default="medium")  # low, medium, high
    status = Column(String(20), default="pending")  # pending, in_progress, completed
    due_date = Column(DateTime)
    created_at = Column(DateTime, default=func.now())
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now())
    
    # Relationships
    project = relationship("Project", back_populates="items")
    
    def __repr__(self):
        return f"<ProjectItem(id={self.id}, title='{self.title}')>"
                """.trimIndent()
            ),
            
            // Database session
            TemplateFile(
                targetPath = "database/session.py",
                content = """
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os

# Database URL configuration
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "sqlite:///./{{ PROJECT_NAME.lower() }}.db"
)

# Create engine
if "postgresql" in DATABASE_URL:
    engine = create_engine(
        DATABASE_URL,
        pool_pre_ping=True,
        echo=False
    )
else:
    # SQLite specific configuration
    engine = create_engine(
        DATABASE_URL,
        connect_args={"check_same_thread": False}
    )

# Create SessionLocal class
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Create Base class
Base = declarative_base()
                """.trimIndent()
            ),
            
            // Database configuration
            TemplateFile(
                targetPath = "database/database.py",
                content = """
from sqlalchemy.orm import Session
from typing import Generator

from .session import SessionLocal, engine

def get_db() -> Generator[Session, None, None]:
    \"\"\"Database dependency for FastAPI endpoints.\"\"\"
    try:
        db = SessionLocal()
        yield db
    finally:
        db.close()
                """.trimIndent()
            ),
            <!-- DATABASE:END -->
            
            // Health router
            TemplateFile(
                targetPath = "routers/health.py",
                content = """
from fastapi import APIRouter
from datetime import datetime
import sys

router = APIRouter()

@router.get("/", summary="Health check")
async def health_check():
    \"\"\"Health check endpoint.\"\"\"
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "version": "1.0.0",
        "python_version": sys.version,
        "service": "{{ PROJECT_NAME }}"
    }

@router.get("/ready", summary="Readiness check")
async def readiness_check():
    \"\"\"Readiness check for load balancers.\"\"\"
    return {
        "status": "ready",
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/live", summary="Liveness check")
async def liveness_check():
    \"\"\"Liveness check for Kubernetes.\"\"\"
    return {
        "status": "alive",
        "timestamp": datetime.utcnow().isoformat()
    }
                """.trimIndent()
            ),
            
            <!-- AUTH:START -->
            <!-- AUTH:DISABLED -->
            // Authentication router
            TemplateFile(
                targetPath = "routers/auth.py",
                content = """
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPBasicCredentials
from sqlalchemy.orm import Session
from datetime import timedelta

from database.database import get_db
from database import models
from auth.auth_handler import (
    verify_password, get_password_hash, create_access_token,
    ACCESS_TOKEN_EXPIRE_MINUTES
)
from auth.schemas import UserCreate, User, Token, UserLogin

router = APIRouter()

@router.post("/register", response_model=User, status_code=status.HTTP_201_CREATED)
async def register(user: UserCreate, db: Session = Depends(get_db)):
    \"\"\"Register a new user.\"\"\"
    # Check if user already exists
    if db.query(models.User).filter(
        (models.User.username == user.username) | 
        (models.User.email == user.email)
    ).first():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username or email already registered"
        )
    
    # Create new user
    hashed_password = get_password_hash(user.password)
    db_user = models.User(
        username=user.username,
        email=user.email,
        first_name=user.first_name,
        last_name=user.last_name,
        hashed_password=hashed_password
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    
    return db_user

@router.post("/login", response_model=Token)
async def login(user_credentials: UserLogin, db: Session = Depends(get_db)):
    \"\"\"Login user and return access token.\"\"\"
    user = db.query(models.User).filter(
        models.User.username == user_credentials.username
    ).first()
    
    if not user or not verify_password(user_credentials.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Inactive user"
        )
    
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "expires_in": ACCESS_TOKEN_EXPIRE_MINUTES * 60
    }

@router.get("/me", response_model=User)
async def read_users_me(current_user: models.User = Depends(get_current_active_user)):
    \"\"\"Get current user information.\"\"\"
    return current_user

@router.post("/logout")
async def logout(current_user: models.User = Depends(get_current_active_user)):
    \"\"\"Logout user (client-side token removal).\"\"\"
    return {"message": "Successfully logged out"}
                """.trimIndent()
            ),
            
            // Users router
            TemplateFile(
                targetPath = "routers/users.py",
                content = """
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database.database import get_db
from database import models
from auth.schemas import User, UserUpdate
from auth.auth_handler import get_current_active_user

router = APIRouter()

@router.get("/", response_model=List[User])
async def list_users(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_active_user)
):
    \"\"\"List all users (admin only).\"\"\"
    if not current_user.is_superuser:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    users = db.query(models.User).offset(skip).limit(limit).all()
    return users

@router.get("/{user_id}", response_model=User)
async def get_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_active_user)
):
    \"\"\"Get user by ID.\"\"\"
    if user_id != current_user.id and not current_user.is_superuser:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    return user

@router.put("/{user_id}", response_model=User)
async def update_user(
    user_id: int,
    user_update: UserUpdate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_active_user)
):
    \"\"\"Update user information.\"\"\"
    if user_id != current_user.id and not current_user.is_superuser:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    # Update user fields
    update_data = user_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(user, field, value)
    
    db.commit()
    db.refresh(user)
    
    return user

@router.delete("/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_active_user)
):
    \"\"\"Delete user (admin only).\"\"\"
    if not current_user.is_superuser:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    db.delete(user)
    db.commit()
    
    return
                """.trimIndent()
            ),
            <!-- AUTH:END -->
            
            <!-- DATABASE:START -->
            <!-- DATABASE:DISABLED -->
            // Projects router
            TemplateFile(
                targetPath = "routers/projects.py",
                content = """
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database.database import get_db
from database import models
from auth.schemas import User
from routers.schemas import ProjectCreate, Project, ProjectUpdate

router = APIRouter()

@router.get("/", response_model=List[Project])
async def list_projects(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"List user's projects.\"\"\"
    projects = db.query(models.Project).filter(
        (models.Project.owner_id == current_user.id) | 
        (models.Project.is_public == True)
    ).offset(skip).limit(limit).all()
    
    return projects

@router.post("/", response_model=Project, status_code=status.HTTP_201_CREATED)
async def create_project(
    project: ProjectCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Create a new project.\"\"\"
    db_project = models.Project(
        **project.dict(),
        owner_id=current_user.id
    )
    db.add(db_project)
    db.commit()
    db.refresh(db_project)
    
    return db_project

@router.get("/{project_id}", response_model=Project)
async def get_project(
    project_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Get project by ID.\"\"\"
    project = db.query(models.Project).filter(
        models.Project.id == project_id
    ).first()
    
    if not project:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found"
        )
    
    # Check permissions
    if project.owner_id != current_user.id and not project.is_public:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    return project

@router.put("/{project_id}", response_model=Project)
async def update_project(
    project_id: int,
    project_update: ProjectUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Update project.\"\"\"
    project = db.query(models.Project).filter(
        models.Project.id == project_id
    ).first()
    
    if not project:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found"
        )
    
    if project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    # Update project fields
    update_data = project_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(project, field, value)
    
    db.commit()
    db.refresh(project)
    
    return project

@router.delete("/{project_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_project(
    project_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Delete project.\"\"\"
    project = db.query(models.Project).filter(
        models.Project.id == project_id
    ).first()
    
    if not project:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found"
        )
    
    if project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    db.delete(project)
    db.commit()
    
    return
                """.trimIndent()
            ),
            
            // Items router
            TemplateFile(
                targetPath = "routers/items.py",
                content = """
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from database.database import get_db
from database import models
from auth.schemas import User
from routers.schemas import ItemCreate, Item, ItemUpdate

router = APIRouter()

@router.get("/", response_model=List[Item])
async def list_items(
    project_id: int = None,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"List items.\"\"\"
    query = db.query(models.ProjectItem)
    
    if project_id:
        # Verify user has access to the project
        project = db.query(models.Project).filter(
            models.Project.id == project_id
        ).first()
        
        if not project:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Project not found"
            )
        
        if project.owner_id != current_user.id and not project.is_public:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Not enough permissions"
            )
        
        query = query.filter(models.ProjectItem.project_id == project_id)
    
    items = query.offset(skip).limit(limit).all()
    
    # Filter items based on project permissions
    if project_id:
        items = [item for item in items if item.project.owner_id == current_user.id or item.project.is_public]
    
    return items

@router.post("/", response_model=Item, status_code=status.HTTP_201_CREATED)
async def create_item(
    item: ItemCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Create a new item.\"\"\"
    # Verify user has access to the project
    project = db.query(models.Project).filter(
        models.Project.id == item.project_id
    ).first()
    
    if not project:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Project not found"
        )
    
    if project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    db_item = models.ProjectItem(**item.dict())
    db.add(db_item)
    db.commit()
    db.refresh(db_item)
    
    return db_item

@router.get("/{item_id}", response_model=Item)
async def get_item(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Get item by ID.\"\"\"
    item = db.query(models.ProjectItem).filter(
        models.ProjectItem.id == item_id
    ).first()
    
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Item not found"
        )
    
    # Check permissions
    if item.project.owner_id != current_user.id and not item.project.is_public:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    return item

@router.put("/{item_id}", response_model=Item)
async def update_item(
    item_id: int,
    item_update: ItemUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Update item.\"\"\"
    item = db.query(models.ProjectItem).filter(
        models.ProjectItem.id == item_id
    ).first()
    
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Item not found"
        )
    
    if item.project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    # Update item fields
    update_data = item_update.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(item, field, value)
    
    db.commit()
    db.refresh(item)
    
    return item

@router.delete("/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_item(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_active_user)
):
    \"\"\"Delete item.\"\"\"
    item = db.query(models.ProjectItem).filter(
        models.ProjectItem.id == item_id
    ).first()
    
    if not item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Item not found"
        )
    
    if item.project.owner_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not enough permissions"
        )
    
    db.delete(item)
    db.commit()
    
    return
                """.trimIndent()
            ),
            <!-- DATABASE:END -->
            
            // API router
            TemplateFile(
                targetPath = "routers/api.py",
                content = """
from fastapi import APIRouter
from typing import Dict, Any
from datetime import datetime

router = APIRouter()

@router.get("/", summary="API information")
async def api_info():
    \"\"\"Get API information and available endpoints.\"\"\"
    return {
        "name": "{{ PROJECT_NAME }} API",
        "version": "1.0.0",
        "description": "REST API built with FastAPI",
        "documentation": {
            "swagger": "/docs",
            "redoc": "/redoc"
        },
        "endpoints": {
            "health": "/health",
            "auth": "/auth",
            "users": "/users",
            "projects": "/projects",
            "items": "/items"
        },
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/endpoints", summary="List all endpoints")
async def list_endpoints():
    \"\"\"List all available API endpoints.\"\"\"
    endpoints = {
        "health": {
            "GET /health": "Health check",
            "GET /health/ready": "Readiness check",
            "GET /health/live": "Liveness check"
        },
        "authentication": {
            "POST /auth/register": "Register new user",
            "POST /auth/login": "Login user",
            "GET /auth/me": "Get current user",
            "POST /auth/logout": "Logout user"
        },
        "users": {
            "GET /users": "List users (admin only)",
            "GET /users/{user_id}": "Get user by ID",
            "PUT /users/{user_id}": "Update user",
            "DELETE /users/{user_id}": "Delete user (admin only)"
        },
        "projects": {
            "GET /projects": "List projects",
            "POST /projects": "Create project",
            "GET /projects/{project_id}": "Get project",
            "PUT /projects/{project_id}": "Update project",
            "DELETE /projects/{project_id}": "Delete project"
        },
        "items": {
            "GET /items": "List items",
            "POST /items": "Create item",
            "GET /items/{item_id}": "Get item",
            "PUT /items/{item_id}": "Update item",
            "DELETE /items/{item_id}": "Delete item"
        }
    }
    
    return {
        "endpoints": endpoints,
        "total_endpoints": sum(len(group) for group in endpoints.values()),
        "timestamp": datetime.utcnow().isoformat()
    }

@router.get("/stats", summary="API statistics")
async def api_stats():
    \"\"\"Get API usage statistics.\"\"\"
    return {
        "requests_total": 0,  # Implement with metrics
        "active_users": 0,
        "total_projects": 0,
        "uptime_seconds": 0,
        "average_response_time": 0,
        "timestamp": datetime.utcnow().isoformat()
    }
                """.trimIndent()
            ),
            
            // Router schemas
            TemplateFile(
                targetPath = "routers/schemas.py",
                content = """
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
class ProjectBase(BaseModel):
    name: str = Field(..., max_length=100, description="Project name")
    description: Optional[str] = Field(None, description="Project description")
    is_public: bool = Field(default=False, description="Whether project is public")
    status: str = Field(default="active", description="Project status")

class ProjectCreate(ProjectBase):
    pass

class ProjectUpdate(BaseModel):
    name: Optional[str] = Field(None, max_length=100)
    description: Optional[str] = None
    is_public: Optional[bool] = None
    status: Optional[str] = None

class Project(ProjectBase):
    id: int
    owner_id: int
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True

class ItemBase(BaseModel):
    project_id: int = Field(..., description="Associated project ID")
    title: str = Field(..., max_length=200, description="Item title")
    content: Optional[str] = Field(None, description="Item content")
    category: Optional[str] = Field(None, description="Item category")
    priority: str = Field(default="medium", description="Item priority")
    status: str = Field(default="pending", description="Item status")
    due_date: Optional[datetime] = Field(None, description="Due date")

class ItemCreate(ItemBase):
    pass

class ItemUpdate(BaseModel):
    title: Optional[str] = Field(None, max_length=200)
    content: Optional[str] = None
    category: Optional[str] = None
    priority: Optional[str] = None
    status: Optional[str] = None
    due_date: Optional[datetime] = None

class Item(ItemBase):
    id: int
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True
<!-- DATABASE:END -->

class MessageResponse(BaseModel):
    message: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)
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
                target_path = "tests/test_main.py",
                content = """
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_read_root():
    \"\"\"Test the root endpoint.\"\"\"
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "app" in data
    assert "version" in data
    assert "docs" in data

def test_health_check():
    \"\"\"Test the health check endpoint.\"\"\"
    response = client.get("/health/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "timestamp" in data

def test_health_ready():
    \"\"\"Test the readiness check endpoint.\"\"\"
    response = client.get("/health/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ready"

def test_health_live():
    \"\"\"Test the liveness check endpoint.\"\"\"
    response = client.get("/health/live")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "alive"

def test_api_info():
    \"\"\"Test the API information endpoint.\"\"\"
    response = client.get("/api/v1/")
    assert response.status_code == 200
    data = response.json()
    assert "name" in data
    assert "version" in data
    assert "endpoints" in data

def test_api_endpoints():
    \"\"\"Test the endpoints listing.\"\"\"
    response = client.get("/api/v1/endpoints")
    assert response.status_code == 200
    data = response.json()
    assert "endpoints" in data
    assert "health" in data["endpoints"]
                """.trimIndent()
            ),
            
            TemplateFile(
                target_path = "tests/test_auth.py",
                content = """
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from database.models import Base
from database.database import get_db
from main import app
from auth.auth_handler import get_password_hash

# Create test database
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def override_get_db():
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

client = TestClient(app)

@pytest.fixture(scope="module")
def test_db():
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)

def test_register_user(test_db):
    \"\"\"Test user registration.\"\"\"
    user_data = {
        "username": "testuser",
        "email": "test@example.com",
        "password": "testpassword123"
    }
    
    response = client.post("/auth/register", json=user_data)
    assert response.status_code == 201
    
    data = response.json()
    assert data["username"] == "testuser"
    assert data["email"] == "test@example.com"
    assert "hashed_password" not in data
    assert "id" in data

def test_login_user(test_db):
    \"\"\"Test user login.\"\"\"
    # First register a user
    user_data = {
        "username": "testuser2",
        "email": "test2@example.com",
        "password": "testpassword123"
    }
    
    client.post("/auth/register", json=user_data)
    
    # Then login
    login_data = {
        "username": "testuser2",
        "password": "testpassword123"
    }
    
    response = client.post("/auth/login", json=login_data)
    assert response.status_code == 200
    
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert "expires_in" in data

def test_login_invalid_credentials(test_db):
    \"\"\"Test login with invalid credentials.\"\"\"
    login_data = {
        "username": "nonexistent",
        "password": "wrongpassword"
    }
    
    response = client.post("/auth/login", json=login_data)
    assert response.status_code == 401

def test_register_duplicate_user(test_db):
    \"\"\"Test registering a user with duplicate username/email.\"\"\"
    user_data = {
        "username": "duplicate",
        "email": "duplicate@example.com",
        "password": "testpassword123"
    }
    
    # Register first user
    client.post("/auth/register", json=user_data)
    
    # Try to register duplicate
    response = client.post("/auth/register", json=user_data)
    assert response.status_code == 400
                """.trimIndent()
            ),
            <!-- TESTS:END -->
            
            // Environment configuration
            TemplateFile(
                targetPath = ".env.example",
                content = """
# Application Configuration
APP_NAME="{{ PROJECT_NAME }}"
DEBUG=True
SECRET_KEY=your-super-secret-key-change-in-production

# Database Configuration
DATABASE_URL=sqlite:///./{{ PROJECT_NAME.lower() }}.db

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
# PostgreSQL Configuration
DATABASE_URL=postgresql://user:password@localhost:5432/{{ PROJECT_NAME.lower() }}
<!-- DATABASE:END -->

# CORS Configuration
ALLOWED_ORIGINS=["http://localhost:3000", "http://localhost:8080"]

# Security Configuration
ACCESS_TOKEN_EXPIRE_MINUTES=30
ALGORITHM=HS256

# Logging
LOG_LEVEL=INFO
                """.trimIndent()
            ),
            
            // Docker configuration
            TemplateFile(
                targetPath = "Dockerfile",
                content = """
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \\
    gcc \\
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create non-root user
RUN useradd --create-home --shell /bin/bash app \\
    && chown -R app:app /app
USER app

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \\
    CMD curl -f http://localhost:8000/health/ || exit 1

# Run the application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
                """.trimIndent()
            ),
            
            // Docker Compose
            TemplateFile(
                targetPath = "docker-compose.yml",
                content = """
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DEBUG=False
      - DATABASE_URL=postgresql://postgres:password@database:5432/{{ PROJECT_NAME.lower() }}
    volumes:
      - .:/app
    depends_on:
      - database
    restart: unless-stopped

  <!-- DATABASE:START -->
  <!-- DATABASE:DISABLED -->
  database:
    image: postgres:15
    environment:
      POSTGRES_DB: {{ PROJECT_NAME.lower() }}
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    ports:
      - "5432:5432"
    restart: unless-stopped
  <!-- DATABASE:END -->

volumes:
<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
  postgres_data:
<!-- DATABASE:END -->
  app_data:
                """.trimIndent()
            ),
            
            // README file
            TemplateFile(
                targetPath = "README.md",
                content = """
# {{ PROJECT_NAME }}

A modern FastAPI application built with the Python IDE template.

## Features

- **High Performance**: Built with FastAPI and Uvicorn for optimal performance
- **Async Support**: Full asynchronous programming support
- **Auto Documentation**: Automatic OpenAPI/Swagger documentation
- **Type Safety**: Full Pydantic validation and type checking
- **Database Integration**: SQLAlchemy ORM with async support
- **Authentication**: JWT-based authentication system
- **Modern API**: RESTful API design with proper HTTP status codes

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
- **CRUD Operations**: Complete Create, Read, Update, Delete functionality
- **Database Migrations**: Alembic migration support
- **Data Validation**: Pydantic models with comprehensive validation
- **Database Relationships**: Proper foreign key relationships
<!-- DATABASE:END -->

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
- **User Management**: Complete user registration and authentication
- **JWT Tokens**: Secure token-based authentication
- **Password Hashing**: Secure password storage with bcrypt
- **User Profiles**: User profile management and updates
- **Authorization**: Role-based access control
<!-- AUTH:END -->

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

### 3. Run the Application
```bash
python main.py
```

Or with Uvicorn directly:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Visit the following URLs:
- **Application**: `http://localhost:8000`
- **API Documentation**: `http://localhost:8000/docs`
- **ReDoc Documentation**: `http://localhost:8000/redoc`

## API Documentation

FastAPI automatically generates interactive API documentation:

### Swagger UI
- **URL**: `/docs`
- **Features**: Interactive API testing, request/response schemas

### ReDoc
- **URL**: `/redoc`
- **Features**: Clean, printable API documentation

## API Endpoints

### Health Check
- `GET /health/` - Basic health check
- `GET /health/ready` - Readiness check for load balancers
- `GET /health/live` - Liveness check for Kubernetes

### Authentication

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login user (returns JWT token)
- `GET /auth/me` - Get current user information
- `POST /auth/logout` - Logout user

### User Management
- `GET /users/` - List all users (admin only)
- `GET /users/{user_id}` - Get user by ID
- `PUT /users/{user_id}` - Update user
- `DELETE /users/{user_id}` - Delete user (admin only)
<!-- AUTH:END -->

### Projects

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
- `GET /projects/` - List user's projects
- `POST /projects/` - Create new project
- `GET /projects/{project_id}` - Get project details
- `PUT /projects/{project_id}` - Update project
- `DELETE /projects/{project_id}` - Delete project

### Items
- `GET /items/` - List items (optional project_id filter)
- `POST /items/` - Create new item
- `GET /items/{item_id}` - Get item details
- `PUT /items/{item_id}` - Update item
- `DELETE /items/{item_id}` - Delete item
<!-- DATABASE:END -->

### API Information
- `GET /api/v1/` - API information
- `GET /api/v1/endpoints` - List all endpoints
- `GET /api/v1/stats` - API statistics

## Project Structure

```
{{ PROJECT_NAME }}/
├── auth/                   # Authentication module
│   ├── auth_handler.py     # JWT and password handling
│   └── schemas.py         # Pydantic schemas for auth
├── database/              # Database module
│   ├── models.py          # SQLAlchemy models
│   ├── database.py        # Database connection
│   └── session.py         # Database session management
├── routers/               # API routes
│   ├── health.py          # Health check endpoints
│   ├── auth.py            # Authentication routes
│   ├── users.py           # User management routes
│   ├── projects.py        # Project CRUD routes
│   ├── items.py           # Item CRUD routes
│   ├── api.py             # General API routes
│   └── schemas.py         # Pydantic schemas
├── tests/                 # Test files
├── main.py                # FastAPI application entry point
├── requirements.txt       # Python dependencies
├── .env.example          # Environment configuration template
├── Dockerfile            # Docker configuration
└── docker-compose.yml    # Docker Compose setup
```

## Database Models

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
### User
- `id`: Primary key
- `username`: Unique username
- `email`: Unique email address
- `hashed_password`: Securely hashed password
- `first_name`: User's first name
- `last_name`: User's last name
- `is_active`: Active status
- `is_superuser`: Admin status
- `created_at`: Account creation timestamp

### Project
- `id`: Primary key
- `name`: Project name
- `description`: Project description
- `owner_id`: Foreign key to User
- `is_public`: Public project flag
- `status`: Project status
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

### ProjectItem
- `id`: Primary key
- `project_id`: Foreign key to Project
- `title`: Item title
- `content`: Item content
- `category`: Item category
- `priority`: Item priority level
- `status`: Item completion status
- `due_date`: Due date (optional)
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp
<!-- DATABASE:END -->

## Configuration

### Environment Variables

Create a `.env` file based on `.env.example`:

```bash
# Required
SECRET_KEY=your-super-secret-key
DATABASE_URL=sqlite:///./app.db

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
# For PostgreSQL
DATABASE_URL=postgresql://user:password@localhost:5432/dbname
<!-- DATABASE:END -->

# Optional
DEBUG=True
ALLOWED_ORIGINS=["http://localhost:3000"]
ACCESS_TOKEN_EXPIRE_MINUTES=30
```

## Running with Docker

### Build and Run
```bash
docker-compose up --build
```

### Development
```bash
# Start services
docker-compose up

# Run migrations (if needed)
docker-compose exec api alembic upgrade head
```

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=.

# Run specific test file
pytest tests/test_auth.py
```

## Development

### Adding New Endpoints

1. Create Pydantic schemas in `routers/schemas.py`
2. Add database models in `database/models.py` (if needed)
3. Implement router functions in appropriate router file
4. Add tests in `tests/`

### Database Migrations

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
```bash
# Create migration
alembic revision --autogenerate -m "Description"

# Run migrations
alembic upgrade head

# Rollback migration
alembic downgrade -1
```
<!-- DATABASE:END -->

## Deployment

### Production Considerations

1. **Environment Variables**: Set secure `SECRET_KEY` and `DATABASE_URL`
2. **HTTPS**: Use HTTPS in production (Let's Encrypt recommended)
3. **Database**: Use PostgreSQL or other production database
4. **CORS**: Configure allowed origins properly
5. **Logging**: Set appropriate log levels
6. **Monitoring**: Add application monitoring

### Production Deployment

1. Build Docker image
2. Set production environment variables
3. Deploy with reverse proxy (nginx/Apache)
4. Use process manager (systemd, supervisor)
5. Set up database backups

Built with ❤️ using the Python IDE
                """.trimIndent()
            )
        )
    }
    
    override fun createProjectStructure(projectDir: File) {
        // Create FastAPI project structure
        val directories = listOf(
            "auth",
            "database",
            "routers",
            "tests",
            ".pythonide"
        )
        
        directories.forEach { dir ->
            File(projectDir, dir).mkdirs()
        }
        
        // Create __init__.py files for Python packages
        val initFiles = listOf(
            "auth/__init__.py",
            "database/__init__.py",
            "routers/__init__.py",
            "tests/__init__.py"
        )
        
        initFiles.forEach { file ->
            File(projectDir, file).createNewFile()
        }
        
        <!-- DATABASE:START -->
        <!-- DATABASE:DISABLED -->
        // Create database migrations directory
        File(projectDir, "alembic").mkdirs()
        File(projectDir, "alembic/versions").mkdirs()
        File(projectDir, "alembic.ini").createNewFile()
        <!-- DATABASE:END -->
    }
    
    override fun getCustomizationOptions(): TemplateCustomizationOptions {
        return TemplateCustomizationOptions(
            enableDatabase = true,
            enableAuthentication = true,
            enableApi = true,
            enableTests = true,
            enableDocumentation = true,
            databaseType = "postgresql",
            additionalDependencies = listOf(
                "fastapi-utils==0.2.1",
                "httpx==0.25.2",
                "aiosqlite==0.19.0"
            )
        )
    }
}