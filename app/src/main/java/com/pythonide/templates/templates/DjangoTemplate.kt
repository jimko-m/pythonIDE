package com.pythonide.templates.templates

import com.pythonide.templates.*
import java.io.File

/**
 * Django web application template
 */
class DjangoTemplate : ProjectTemplate {
    
    override fun getTemplateInfo(): ProjectTemplate {
        return ProjectTemplate(
            id = "django",
            name = "Django Web Framework",
            description = "High-level Python web framework for rapid development",
            category = "Web Development",
            icon = "django",
            version = "1.0.0",
            author = "Python IDE",
            tags = listOf("web", "django", "python", "backend", "framework", "orm")
        )
    }
    
    override fun getTemplateFiles(): List<TemplateFile> {
        return listOf(
            // Django settings
            TemplateFile(
                targetPath = "config/settings.py",
                content = """
\"\"\"
Django settings for {{ PROJECT_NAME }} project.
\"\"\"

import os
from pathlib import Path

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = os.environ.get('SECRET_KEY', 'django-insecure-default-key-change-in-production')

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = os.environ.get('DEBUG', 'True').lower() == 'true'

ALLOWED_HOSTS = os.environ.get('ALLOWED_HOSTS', 'localhost,127.0.0.1').split(',')

# Application definition
DJANGO_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
]

THIRD_PARTY_APPS = [
    <!-- API:START -->
    <!-- API:DISABLED -->
    'rest_framework',
    'rest_framework_simplejwt',
    'corsheaders',
    <!-- API:END -->
]

LOCAL_APPS = [
    <!-- DATABASE:START -->
    <!-- DATABASE:DISABLED -->
    'core',
    <!-- DATABASE:END -->
    'accounts',
]

INSTALLED_APPS = DJANGO_APPS + THIRD_PARTY_APPS + LOCAL_APPS

MIDDLEWARE = [
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]

ROOT_URLCONF = 'config.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates'],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'config.wsgi.application'

# Database
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
# PostgreSQL database configuration
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': os.environ.get('POSTGRES_DB', '{{ PROJECT_NAME.lower() }}'),
        'USER': os.environ.get('POSTGRES_USER', 'postgres'),
        'PASSWORD': os.environ.get('POSTGRES_PASSWORD', ''),
        'HOST': os.environ.get('POSTGRES_HOST', 'localhost'),
        'PORT': os.environ.get('POSTGRES_PORT', '5432'),
    }
}
<!-- DATABASE:END -->

# Password validation
AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]

# Internationalization
LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_TZ = True

# Static files (CSS, JavaScript, Images)
STATIC_URL = '/static/'
STATICFILES_DIRS = [BASE_DIR / 'static']
STATIC_ROOT = BASE_DIR / 'staticfiles'

MEDIA_URL = '/media/'
MEDIA_ROOT = BASE_DIR / 'media'

# Default primary key field type
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
# Custom User Model
AUTH_USER_MODEL = 'accounts.User'

# Authentication settings
LOGIN_REDIRECT_URL = '/'
LOGOUT_REDIRECT_URL = '/'
LOGIN_URL = '/login/'

# Session settings
SESSION_COOKIE_AGE = 86400  # 1 day
SESSION_COOKIE_SECURE = not DEBUG
SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_SAMESITE = 'Lax'
<!-- AUTH:END -->

<!-- API:START -->
<!-- API:DISABLED -->
# REST Framework settings
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework_simplejwt.authentication.JWTAuthentication',
        'rest_framework.authentication.SessionAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 20,
    'DEFAULT_FILTER_BACKENDS': [
        'django_filters.rest_framework.DjangoFilterBackend',
        'rest_framework.filters.SearchFilter',
        'rest_framework.filters.OrderingFilter',
    ],
}

# CORS settings
CORS_ALLOWED_ORIGINS = [
    "http://localhost:3000",
    "http://127.0.0.1:3000",
]
CORS_ALLOW_CREDENTIALS = True
<!-- API:END -->
                """.trimIndent()
            ),
            
            // Django URLs configuration
            TemplateFile(
                targetPath = "config/urls.py",
                content = """
\"\"\"
{{ PROJECT_NAME }} URL Configuration
\"\"\"

from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from django.views.generic import TemplateView

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', TemplateView.as_view(template_name='home.html'), name='home'),
    
    <!-- AUTH:START -->
    <!-- AUTH:DISABLED -->
    path('accounts/', include('accounts.urls')),
    path('accounts/', include('django.contrib.auth.urls')),
    <!-- AUTH:END -->
    
    <!-- API:START -->
    <!-- API:DISABLED -->
    path('api/v1/', include('core.api_urls')),
    <!-- API:END -->
]

# Serve media and static files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
                """.trimIndent()
            ),
            
            // Django requirements
            TemplateFile(
                targetPath = "requirements.txt",
                content = """
Django==4.2.7
psycopg2-binary==2.9.9
Pillow==10.0.1
python-decouple==3.8

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
Django==4.2.7
psycopg2-binary==2.9.9
django-crispy-forms==2.1
crispy-bootstrap5==0.7
django-crispy-forms==2.1
django-widget-tweaks==1.5.0
<!-- DATABASE:END -->

<!-- API:START -->
<!-- API:DISABLED -->
djangorestframework==3.14.0
django-cors-headers==4.3.1
django-filter==23.5
djangorestframework-simplejwt==5.3.0
drf-spectacular==0.26.5
<!-- API:END -->

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
django-allauth==0.57.0
django-extensions==3.2.3
<!-- AUTH:END -->

<!-- TESTS:START -->
<!-- TESTS:DISABLED -->
django-extensions==3.2.3
factory-boy==3.3.0
pytest-django==4.7.0
pytest-cov==4.1.0
<!-- TESTS:END -->
                """.trimIndent()
            ),
            
            <!-- AUTH:START -->
            <!-- AUTH:DISABLED -->
            // Custom User Model
            TemplateFile(
                targetPath = "accounts/models.py",
                content = """
from django.contrib.auth.models import AbstractUser
from django.db import models
from django.urls import reverse

class User(AbstractUser):
    \"\"\"Custom user model with additional fields.\"\"\"
    email = models.EmailField(unique=True)
    first_name = models.CharField(max_length=30, blank=True)
    last_name = models.CharField(max_length=150, blank=True)
    date_joined = models.DateTimeField(auto_now_add=True)
    is_verified = models.BooleanField(default=False)
    
    class Meta:
        db_table = 'auth_user'
        verbose_name = 'User'
        verbose_name_plural = 'Users'
    
    def __str__(self):
        return self.username
    
    def get_absolute_url(self):
        return reverse('user_detail', kwargs={'username': self.username})
    
    def get_full_name(self):
        return f"{self.first_name} {self.last_name}".strip() or self.username
                """.trimIndent()
            ),
            
            // User URLs
            TemplateFile(
                targetPath = "accounts/urls.py",
                content = """
from django.urls import path
from django.contrib.auth import views as auth_views
from . import views

app_name = 'accounts'

urlpatterns = [
    path('signup/', views.signup, name='signup'),
    path('login/', auth_views.LoginView.as_view(template_name='registration/login.html'), name='login'),
    path('logout/', auth_views.LogoutView.as_view(), name='logout'),
    path('profile/<str:username>/', views.user_profile, name='user_profile'),
    path('profile/', views.my_profile, name='my_profile'),
    path('settings/', views.settings, name='settings'),
]
                """.trimIndent()
            ),
            
            // User Views
            TemplateFile(
                targetPath = "accounts/views.py",
                content = """
from django.shortcuts import render, redirect, get_object_or_404
from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth.decorators import login_required
from django.http import HttpResponseForbidden
from .models import User
from .forms import UserCreationForm, UserChangeForm

def signup(request):
    if request.user.is_authenticated:
        return redirect('home')
    
    if request.method == 'POST':
        form = UserCreationForm(request.POST)
        if form.is_valid():
            user = form.save()
            login(request, user)
            messages.success(request, f'Account created for {user.username}!')
            return redirect('home')
    else:
        form = UserCreationForm()
    return render(request, 'registration/signup.html', {'form': form})

@login_required
def user_profile(request, username):
    user = get_object_or_404(User, username=username)
    if user != request.user:
        return HttpResponseForbidden("You can't view this profile.")
    
    return render(request, 'accounts/profile.html', {'profile_user': user})

@login_required
def my_profile(request):
    return redirect('accounts:user_profile', username=request.user.username)

@login_required
def settings(request):
    if request.method == 'POST':
        form = UserChangeForm(request.POST, instance=request.user)
        if form.is_valid():
            form.save()
            messages.success(request, 'Profile updated successfully!')
            return redirect('accounts:settings')
    else:
        form = UserChangeForm(instance=request.user)
    return render(request, 'accounts/settings.html', {'form': form})
                """.trimIndent()
            ),
            
            // User Forms
            TemplateFile(
                targetPath = "accounts/forms.py",
                content = """
from django import forms
from django.contrib.auth.forms import UserCreationForm, UserChangeForm
from .models import User

class UserCreationForm(UserCreationForm):
    email = forms.EmailField(required=True)
    
    class Meta:
        model = User
        fields = ('username', 'email', 'first_name', 'last_name')

class UserChangeForm(UserChangeForm):
    class Meta:
        model = User
        fields = ('username', 'email', 'first_name', 'last_name')
                """.trimIndent()
            ),
            
            // Admin configuration
            TemplateFile(
                targetPath = "accounts/admin.py",
                content = """
from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from .models import User

@admin.register(User)
class UserAdmin(BaseUserAdmin):
    fieldsets = BaseUserAdmin.fieldsets + (
        ('Profile Information', {
            'fields': ('first_name', 'last_name', 'email', 'is_verified')
        }),
        ('Important dates', {
            'fields': ('date_joined',)
        }),
    )
    add_fieldsets = BaseUserAdmin.add_fieldsets + (
        ('Profile Information', {
            'fields': ('first_name', 'last_name', 'email')
        }),
    )
    list_display = ('username', 'email', 'first_name', 'last_name', 'is_staff', 'is_verified')
    list_filter = ('is_staff', 'is_superuser', 'is_active', 'is_verified')
    search_fields = ('username', 'first_name', 'last_name', 'email')
    ordering = ('username',)
                """.trimIndent()
            ),
            <!-- AUTH:END -->
            
            <!-- DATABASE:START -->
            <!-- DATABASE:DISABLED -->
            // Core models
            TemplateFile(
                targetPath = "core/models.py",
                content = """
from django.db import models
from django.contrib.auth import get_user_model
from django.urls import reverse

User = get_user_model()

class {{ PROJECT_NAME.title() }}Model(models.Model):
    \"\"\"Main model for {{ PROJECT_NAME }}.\"\"\"
    name = models.CharField(max_length=200)
    description = models.TextField(blank=True)
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='{{ PROJECT_NAME.lower() }}_models')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        ordering = ['-created_at']
        verbose_name = '{{ PROJECT_NAME.title() }} Model'
        verbose_name_plural = '{{ PROJECT_NAME.title() }} Models'
    
    def __str__(self):
        return self.name
    
    def get_absolute_url(self):
        return reverse('{{ PROJECT_NAME.lower() }}_detail', kwargs={'pk': self.pk})

class {{ PROJECT_NAME.title() }}Item(models.Model):
    \"\"\"Items related to the main model.\"\"\"
    {{ PROJECT_NAME.lower() }}_model = models.ForeignKey({{ PROJECT_NAME.title() }}Model, on_delete=models.CASCADE, related_name='items')
    title = models.CharField(max_length=200)
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        ordering = ['-created_at']
    
    def __str__(self):
        return self.title
                """.trimIndent()
            ),
            
            // Core URLs
            TemplateFile(
                targetPath = "core/urls.py",
                content = """
from django.urls import path
from . import views

app_name = '{{ PROJECT_NAME.lower() }}'

urlpatterns = [
    path('', views.model_list, name='model_list'),
    path('<int:pk>/', views.model_detail, name='model_detail'),
    path('create/', views.model_create, name='model_create'),
    path('<int:pk>/update/', views.model_update, name='model_update'),
    path('<int:pk>/delete/', views.model_delete, name='model_delete'),
    path('<int:model_pk>/items/', views.item_list, name='item_list'),
    path('<int:model_pk>/items/create/', views.item_create, name='item_create'),
    path('<int:model_pk>/items/<int:item_pk>/', views.item_detail, name='item_detail'),
    path('<int:model_pk>/items/<int:item_pk>/update/', views.item_update, name='item_update'),
    path('<int:model_pk>/items/<int:item_pk>/delete/', views.item_delete, name='item_delete'),
]
                """.trimIndent()
            ),
            
            // Core views
            TemplateFile(
                targetPath = "core/views.py",
                content = """
from django.shortcuts import render, get_object_or_404, redirect
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.http import HttpResponseForbidden
from django.core.paginator import Paginator
from .models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item
from .forms import {{ PROJECT_NAME.title() }}ModelForm, {{ PROJECT_NAME.title() }}ItemForm

@login_required
def model_list(request):
    models = {{ PROJECT_NAME.title() }}Model.objects.filter(owner=request.user)
    paginator = Paginator(models, 10)
    page_number = request.GET.get('page')
    page_obj = paginator.get_page(page_number)
    return render(request, 'core/model_list.html', {'page_obj': page_obj})

@login_required
def model_detail(request, pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=pk, owner=request.user)
    items = model.items.all()
    return render(request, 'core/model_detail.html', {'model': model, 'items': items})

@login_required
def model_create(request):
    if request.method == 'POST':
        form = {{ PROJECT_NAME.title() }}ModelForm(request.POST)
        if form.is_valid():
            model = form.save(commit=False)
            model.owner = request.user
            model.save()
            messages.success(request, '{{ PROJECT_NAME.title() }} model created successfully!')
            return redirect('{{ PROJECT_NAME.lower() }}:model_detail', pk=model.pk)
    else:
        form = {{ PROJECT_NAME.title() }}ModelForm()
    return render(request, 'core/model_form.html', {'form': form})

@login_required
def model_update(request, pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=pk, owner=request.user)
    if request.method == 'POST':
        form = {{ PROJECT_NAME.title() }}ModelForm(request.POST, instance=model)
        if form.is_valid():
            form.save()
            messages.success(request, '{{ PROJECT_NAME.title() }} model updated successfully!')
            return redirect('{{ PROJECT_NAME.lower() }}:model_detail', pk=model.pk)
    else:
        form = {{ PROJECT_NAME.title() }}ModelForm(instance=model)
    return render(request, 'core/model_form.html', {'form': form})

@login_required
def model_delete(request, pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=pk, owner=request.user)
    if request.method == 'POST':
        model.delete()
        messages.success(request, '{{ PROJECT_NAME.title() }} model deleted successfully!')
        return redirect('{{ PROJECT_NAME.lower() }}:model_list')
    return render(request, 'core/model_confirm_delete.html', {'model': model})

@login_required
def item_list(request, model_pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=model_pk, owner=request.user)
    items = model.items.all()
    return render(request, 'core/item_list.html', {'model': model, 'items': items})

@login_required
def item_create(request, model_pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=model_pk, owner=request.user)
    if request.method == 'POST':
        form = {{ PROJECT_NAME.title() }}ItemForm(request.POST)
        if form.is_valid():
            item = form.save(commit=False)
            item.{{ PROJECT_NAME.lower() }}_model = model
            item.save()
            messages.success(request, 'Item created successfully!')
            return redirect('{{ PROJECT_NAME.lower() }}:item_detail', model_pk=model.pk, item_pk=item.pk)
    else:
        form = {{ PROJECT_NAME.title() }}ItemForm()
    return render(request, 'core/item_form.html', {'form': form, 'model': model})

@login_required
def item_detail(request, model_pk, item_pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=model_pk, owner=request.user)
    item = get_object_or_404(model.items.all(), pk=item_pk)
    return render(request, 'core/item_detail.html', {'model': model, 'item': item})

@login_required
def item_update(request, model_pk, item_pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=model_pk, owner=request.user)
    item = get_object_or_404(model.items.all(), pk=item_pk)
    if request.method == 'POST':
        form = {{ PROJECT_NAME.title() }}ItemForm(request.POST, instance=item)
        if form.is_valid():
            form.save()
            messages.success(request, 'Item updated successfully!')
            return redirect('{{ PROJECT_NAME.lower() }}:item_detail', model_pk=model.pk, item_pk=item.pk)
    else:
        form = {{ PROJECT_NAME.title() }}ItemForm(instance=item)
    return render(request, 'core/item_form.html', {'form': form, 'model': model})

@login_required
def item_delete(request, model_pk, item_pk):
    model = get_object_or_404({{ PROJECT_NAME.title() }}Model, pk=model_pk, owner=request.user)
    item = get_object_or_404(model.items.all(), pk=item_pk)
    if request.method == 'POST':
        item.delete()
        messages.success(request, 'Item deleted successfully!')
        return redirect('{{ PROJECT_NAME.lower() }}:item_list', model_pk=model.pk)
    return render(request, 'core/item_confirm_delete.html', {'model': model, 'item': item})
                """.trimIndent()
            ),
            
            // Core forms
            TemplateFile(
                targetPath = "core/forms.py",
                content = """
from django import forms
from .models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item

class {{ PROJECT_NAME.title() }}ModelForm(forms.ModelForm):
    class Meta:
        model = {{ PROJECT_NAME.title() }}Model
        fields = ['name', 'description']
        widgets = {
            'name': forms.TextInput(attrs={'class': 'form-control'}),
            'description': forms.Textarea(attrs={'class': 'form-control', 'rows': 4}),
        }

class {{ PROJECT_NAME.title() }}ItemForm(forms.ModelForm):
    class Meta:
        model = {{ PROJECT_NAME.title() }}Item
        fields = ['title', 'content']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control'}),
            'content': forms.Textarea(attrs={'class': 'form-control', 'rows': 4}),
        }
                """.trimIndent()
            ),
            
            // Core admin
            TemplateFile(
                targetPath = "core/admin.py",
                content = """
from django.contrib import admin
from .models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item

@admin.register({{ PROJECT_NAME.title() }}Model)
class {{ PROJECT_NAME.title() }}ModelAdmin(admin.ModelAdmin):
    list_display = ['name', 'owner', 'created_at', 'updated_at']
    list_filter = ['created_at', 'updated_at']
    search_fields = ['name', 'description', 'owner__username']
    readonly_fields = ['created_at', 'updated_at']

@admin.register({{ PROJECT_NAME.title() }}Item)
class {{ PROJECT_NAME.title() }}ItemAdmin(admin.ModelAdmin):
    list_display = ['title', '{{ PROJECT_NAME.lower() }}_model', 'created_at']
    list_filter = ['created_at', '{{ PROJECT_NAME.lower() }}_model']
    search_fields = ['title', 'content']
    readonly_fields = ['created_at', 'updated_at']
                """.trimIndent()
            ),
            <!-- DATABASE:END -->
            
            <!-- API:START -->
            <!-- API:DISABLED -->
            // API URLs
            TemplateFile(
                targetPath = "core/api_urls.py",
                content = """
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .api import views

router = DefaultRouter()
<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
router.register(r'{{ PROJECT_NAME.lower() }}-models', views.{{ PROJECT_NAME.title() }}ModelViewSet)
router.register(r'items', views.{{ PROJECT_NAME.title() }}ItemViewSet)
<!-- DATABASE:END -->

urlpatterns = [
    path('', include(router.urls)),
    path('auth/', include('rest_framework.urls')),  # Login/logout endpoints
    path('token/', views.CustomTokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('token/refresh/', views.CustomTokenRefreshView.as_view(), name='token_refresh'),
]
                """.trimIndent()
            ),
            
            // API Views
            TemplateFile(
                targetPath = "core/api.py",
                content = """
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from django_filters.rest_framework import DjangoFilterBackend
from rest_framework.filters import SearchFilter, OrderingFilter
from .models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item
from .serializers import {{ PROJECT_NAME.title() }}ModelSerializer, {{ PROJECT_NAME.title() }}ItemSerializer

class {{ PROJECT_NAME.title() }}ModelViewSet(viewsets.ModelViewSet):
    serializer_class = {{ PROJECT_NAME.title() }}ModelSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ['name', 'owner']
    search_fields = ['name', 'description']
    ordering_fields = ['name', 'created_at', 'updated_at']
    ordering = ['-created_at']
    
    def get_queryset(self):
        return {{ PROJECT_NAME.title() }}Model.objects.filter(owner=self.request.user)
    
    def perform_create(self, serializer):
        serializer.save(owner=self.request.user)
    
    @action(detail=True, methods=['get'])
    def items(self, request, pk=None):
        model = self.get_object()
        items = model.items.all()
        serializer = {{ PROJECT_NAME.title() }}ItemSerializer(items, many=True)
        return Response(serializer.data)

class {{ PROJECT_NAME.title() }}ItemViewSet(viewsets.ModelViewSet):
    serializer_class = {{ PROJECT_NAME.title() }}ItemSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ['{{ PROJECT_NAME.lower() }}_model']
    search_fields = ['title', 'content']
    ordering_fields = ['title', 'created_at']
    ordering = ['-created_at']
    
    def get_queryset(self):
        return {{ PROJECT_NAME.title() }}Item.objects.filter({{ PROJECT_NAME.lower() }}_model__owner=self.request.user)

class CustomTokenObtainPairView(TokenObtainPairView):
    pass

class CustomTokenRefreshView(TokenRefreshView):
    pass
                """.trimIndent()
            ),
            
            // API Serializers
            TemplateFile(
                targetPath = "core/serializers.py",
                content = """
from rest_framework import serializers
from .models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item

class {{ PROJECT_NAME.title() }}ItemSerializer(serializers.ModelSerializer):
    class Meta:
        model = {{ PROJECT_NAME.title() }}Item
        fields = ['id', 'title', 'content', 'created_at', 'updated_at']
        read_only_fields = ['created_at', 'updated_at']

class {{ PROJECT_NAME.title() }}ModelSerializer(serializers.ModelSerializer):
    items = {{ PROJECT_NAME.title() }}ItemSerializer(many=True, read_only=True)
    item_count = serializers.SerializerMethodField()
    
    class Meta:
        model = {{ PROJECT_NAME.title() }}Model
        fields = ['id', 'name', 'description', 'owner', 'items', 'item_count', 'created_at', 'updated_at']
        read_only_fields = ['owner', 'created_at', 'updated_at']
    
    def get_item_count(self, obj):
        return obj.items.count()
                """.trimIndent()
            ),
            <!-- API:END -->
            
            // HTML Templates
            TemplateFile(
                targetPath = "templates/base.html",
                content = """
{% load static %}
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{% block title %}{{ title or "Django Project" }}{% endblock %}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="{% static 'css/style.css' %}" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="{% url 'home' %}">{{ PROJECT_NAME }}</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'home' %}">Home</a>
                    </li>
                    <!-- AUTH:START -->
                    <!-- AUTH:DISABLED -->
                    {% if user.is_authenticated %}
                    <li class="nav-item">
                        <a class="nav-link" href="{% url '{{ PROJECT_NAME.lower() }}:model_list' %}">Models</a>
                    </li>
                    {% endif %}
                    <!-- AUTH:END -->
                </ul>
                <ul class="navbar-nav">
                    <!-- AUTH:START -->
                    <!-- AUTH:DISABLED -->
                    {% if user.is_authenticated %}
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-bs-toggle="dropdown">
                            {{ user.username }}
                        </a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="{% url 'accounts:my_profile' %}">Profile</a></li>
                            <li><a class="dropdown-item" href="{% url 'accounts:settings' %}">Settings</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item" href="{% url 'accounts:logout' %}">Logout</a></li>
                        </ul>
                    </li>
                    {% else %}
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'accounts:login' %}">Login</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="{% url 'accounts:signup' %}">Sign Up</a>
                    </li>
                    {% endif %}
                    <!-- AUTH:END -->
                </ul>
            </div>
        </div>
    </nav>

    <main class="container my-4">
        {% if messages %}
        <div class="messages">
            {% for message in messages %}
            <div class="alert alert-{{ message.tags }} alert-dismissible fade show" role="alert">
                {{ message }}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
            {% endfor %}
        </div>
        {% endif %}
        
        {% block content %}{% endblock %}
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="{% static 'js/main.js' %}"></script>
    {% block scripts %}{% endblock %}
</body>
</html>
                """.trimIndent()
            ),
            
            // Home template
            TemplateFile(
                targetPath = "templates/home.html",
                content = """
{% extends "base.html" %}

{% block title %}{{ PROJECT_NAME }} - Home{% endblock %}

{% block content %}
<div class="row">
    <div class="col-12">
        <div class="jumbotron bg-light p-5 rounded mb-4">
            <h1 class="display-4">Welcome to {{ PROJECT_NAME }}!</h1>
            <p class="lead">A Django web application built with the Python IDE template.</p>
            <hr class="my-4">
            <p>Get started by creating your first model or exploring the features.</p>
            <!-- AUTH:START -->
            <!-- AUTH:DISABLED -->
            {% if user.is_authenticated %}
            <a class="btn btn-primary btn-lg" href="{% url '{{ PROJECT_NAME.lower() }}:model_list' %}" role="button">View Models</a>
            {% else %}
            <a class="btn btn-primary btn-lg" href="{% url 'accounts:signup' %}" role="button">Get Started</a>
            {% endif %}
            <!-- AUTH:END -->
        </div>
    </div>
</div>

<div class="row">
    <div class="col-md-4">
        <div class="card mb-4">
            <div class="card-body">
                <h5 class="card-title">Django Framework</h5>
                <p class="card-text">Built with Django's powerful web framework for rapid development.</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card mb-4">
            <div class="card-body">
                <h5 class="card-title">RESTful API</h5>
                <p class="card-text">Complete REST API with authentication and serialization.</p>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card mb-4">
            <div class="card-body">
                <h5 class="card-title">Modern UI</h5>
                <p class="card-text">Responsive design with Bootstrap for great user experience.</p>
            </div>
        </div>
    </div>
</div>
{% endblock %}
                """.trimIndent()
            ),
            
            // Static files
            TemplateFile(
                targetPath = "static/css/style.css",
                content = """
/* Custom styles for {{ PROJECT_NAME }} */

.jumbotron {
    background: linear-gradient(135deg, #6B73FF 0%, #000DFF 100%);
    color: white;
}

.card {
    border: none;
    box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
    transition: transform 0.2s;
}

.card:hover {
    transform: translateY(-5px);
}

.navbar-brand {
    font-weight: bold;
}

.form-control:focus {
    border-color: #6B73FF;
    box-shadow: 0 0 0 0.2rem rgba(107, 115, 255, 0.25);
}
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "static/js/main.js",
                content = """
// Main JavaScript for {{ PROJECT_NAME }}

document.addEventListener('DOMContentLoaded', function() {
    console.log('{{ PROJECT_NAME }} Django app loaded!');
    initializeComponents();
});

function initializeComponents() {
    // Add any initialization code here
    setupForms();
    setupTooltips();
}

function setupForms() {
    // Enhanced form handling
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Submitting...';
            }
        });
    });
}

function setupTooltips() {
    // Initialize Bootstrap tooltips if available
    if (typeof bootstrap !== 'undefined') {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
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
                targetPath = "tests/test_models.py",
                content = """
import pytest
from django.contrib.auth import get_user_model
from django.test import TestCase
from core.models import {{ PROJECT_NAME.title() }}Model, {{ PROJECT_NAME.title() }}Item

User = get_user_model()

class {{ PROJECT_NAME.title() }}ModelTestCase(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username='testuser',
            email='test@example.com',
            password='testpassword'
        )
        self.model = {{ PROJECT_NAME.title() }}Model.objects.create(
            name='Test Model',
            description='Test description',
            owner=self.user
        )
    
    def test_model_creation(self):
        \"\"\"Test that a model can be created.\"\"\"
        self.assertEqual(self.model.name, 'Test Model')
        self.assertEqual(self.model.owner, self.user)
        self.assertTrue(hasattr(self.model, 'created_at'))
    
    def test_model_str_representation(self):
        \"\"\"Test the string representation of the model.\"\"\"
        self.assertEqual(str(self.model), 'Test Model')
    
    def test_model_get_absolute_url(self):
        \"\"\"Test the get_absolute_url method.\"\"\"
        self.assertEqual(self.model.get_absolute_url(), f'/model/{self.model.pk}/')

class {{ PROJECT_NAME.title() }}ItemTestCase(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username='testuser',
            email='test@example.com',
            password='testpassword'
        )
        self.model = {{ PROJECT_NAME.title() }}Model.objects.create(
            name='Test Model',
            description='Test description',
            owner=self.user
        )
        self.item = {{ PROJECT_NAME.title() }}Item.objects.create(
            {{ PROJECT_NAME.lower() }}_model=self.model,
            title='Test Item',
            content='Test content'
        )
    
    def test_item_creation(self):
        \"\"\"Test that an item can be created.\"\"\"
        self.assertEqual(self.item.title, 'Test Item')
        self.assertEqual(self.item.{{ PROJECT_NAME.lower() }}_model, self.model)
    
    def test_item_str_representation(self):
        \"\"\"Test the string representation of the item.\"\"\"
        self.assertEqual(str(self.item), 'Test Item')

@pytest.mark.django_db
def test_user_creation():
    \"\"\"Test user creation with pytest.\"\"\"
    user = User.objects.create_user(
        username='pytestuser',
        email='pytest@example.com',
        password='testpass'
    )
    assert user.username == 'pytestuser'
    assert user.email == 'pytest@example.com'
    assert user.check_password('testpass')
    assert not user.is_staff
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "tests/test_views.py",
                content = """
from django.test import Client, TestCase
from django.urls import reverse
from django.contrib.auth import get_user_model
from core.models import {{ PROJECT_NAME.title() }}Model

User = get_user_model()

class {{ PROJECT_NAME.title() }}ModelViewTestCase(TestCase):
    def setUp(self):
        self.client = Client()
        self.user = User.objects.create_user(
            username='testuser',
            email='test@example.com',
            password='testpassword'
        )
        self.client.login(username='testuser', password='testpassword')
        self.model = {{ PROJECT_NAME.title() }}Model.objects.create(
            name='Test Model',
            description='Test description',
            owner=self.user
        )
    
    def test_model_list_view(self):
        \"\"\"Test the model list view.\"\"\"
        response = self.client.get(reverse('{{ PROJECT_NAME.lower() }}:model_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Test Model')
    
    def test_model_detail_view(self):
        \"\"\"Test the model detail view.\"\"\"
        response = self.client.get(reverse('{{ PROJECT_NAME.lower() }}:model_detail', kwargs={'pk': self.model.pk}))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'Test Model')
    
    def test_model_create_view(self):
        \"\"\"Test the model create view.\"\"\"
        response = self.client.get(reverse('{{ PROJECT_NAME.lower() }}:model_create'))
        self.assertEqual(response.status_code, 200)
        
        response = self.client.post(reverse('{{ PROJECT_NAME.lower() }}:model_create'), {
            'name': 'New Model',
            'description': 'New description'
        })
        self.assertEqual(response.status_code, 302)  # Redirect after successful creation
        self.assertTrue({{ PROJECT_NAME.title() }}Model.objects.filter(name='New Model').exists())

class {{ PROJECT_NAME.title() }}APITestCase(TestCase):
    def setUp(self):
        self.client = Client()
        self.user = User.objects.create_user(
            username='testuser',
            email='test@example.com',
            password='testpassword'
        )
        self.client.login(username='testuser', password='testpassword')
        self.model = {{ PROJECT_NAME.title() }}Model.objects.create(
            name='Test Model',
            description='Test description',
            owner=self.user
        )
    
    def test_api_model_list(self):
        \"\"\"Test the API model list endpoint.\"\"\"
        response = self.client.get('/api/v1/{{ PROJECT_NAME.lower() }}-models/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(len(data['results']), 1)
        self.assertEqual(data['results'][0]['name'], 'Test Model')
    
    def test_api_model_create(self):
        \"\"\"Test creating a model via API.\"\"\"
        response = self.client.post('/api/v1/{{ PROJECT_NAME.lower() }}-models/', {
            'name': 'API Model',
            'description': 'API description'
        }, content_type='application/json')
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data['name'], 'API Model')
                """.trimIndent()
            ),
            
            TemplateFile(
                targetPath = "pytest.ini",
                content = """
[tool:pytest]
DJANGO_SETTINGS_MODULE = config.settings
python_files = tests/*.py
python_classes = Test*
python_functions = test_*
addopts = 
    --verbose
    --cov=core
    --cov=accounts
    --cov-report=html
    --cov-report=term-missing
    --django-db
                """.trimIndent()
            ),
            <!-- TESTS:END -->
            
            // Django management commands
            TemplateFile(
                targetPath = "manage.py",
                content = """
#!/usr/bin/env python
\"\"\"Django's command-line utility for administrative tasks.\"\"\"
import os
import sys

if __name__ == '__main__':
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
    try:
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        raise ImportError(
            "Couldn't import Django. Are you sure it's installed and "
            "available on your PYTHONPATH environment variable? Did you "
            "forget to activate a virtual environment?"
        ) from exc
    execute_from_command_line(sys.argv)
                """.trimIndent()
            ),
            
            // Environment configuration
            TemplateFile(
                targetPath = ".env.example",
                content = """
# Django Configuration
DEBUG=True
SECRET_KEY=your-secret-key-here
ALLOWED_HOSTS=localhost,127.0.0.1

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
# Database Configuration
POSTGRES_DB={{ PROJECT_NAME.lower() }}
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
<!-- DATABASE:END -->

# Email Configuration (optional)
EMAIL_BACKEND=django.core.mail.backends.console.EmailBackend
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USE_TLS=True
EMAIL_HOST_USER=your-email@gmail.com
EMAIL_HOST_PASSWORD=your-app-password
                """.trimIndent()
            ),
            
            // README file
            TemplateFile(
                targetPath = "README.md",
                content = """
# {{ PROJECT_NAME }}

A Django web application created from the Python IDE template.

## Features

- Django web framework with modern project structure
- User authentication and registration
- REST API with JWT authentication
- Bootstrap-powered responsive UI
- PostgreSQL database support

<!-- DATABASE:START -->
<!-- DATABASE:DISABLED -->
- Complete CRUD operations
- Admin interface
- Database migrations
- Data validation
<!-- DATABASE:END -->

<!-- AUTH:START -->
<!-- AUTH:DISABLED -->
- User management system
- Profile management
- Secure authentication
- Session management
<!-- AUTH:END -->

## Quick Start

### 1. Setup Virtual Environment
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\\Scripts\\activate
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Environment Configuration
```bash
cp .env.example .env
# Edit .env file with your settings
```

### 4. Database Setup
```bash
python manage.py migrate
python manage.py createsuperuser
```

### 5. Run Development Server
```bash
python manage.py runserver
```

Visit `http://127.0.0.1:8000` to see your application!

## Project Structure

```
{{ PROJECT_NAME }}/
├── config/                 # Django settings and configuration
│   ├── settings.py         # Main settings file
│   ├── urls.py            # URL configuration
│   └── wsgi.py            # WSGI application
├── core/                  # Main application
│   ├── models.py          # Data models
│   ├── views.py           # View functions
│   ├── urls.py            # App URLs
│   ├── admin.py           # Admin interface
│   ├── forms.py           # Django forms
│   └── api.py             # REST API views
├── accounts/              # User management
│   ├── models.py          # Custom user model
│   ├── views.py           # User views
│   ├── urls.py            # User URLs
│   ├── forms.py           # User forms
│   └── admin.py           # User admin
├── templates/             # HTML templates
│   ├── base.html          # Base template
│   └── home.html          # Home page
├── static/                # Static files
│   ├── css/               # Stylesheets
│   └── js/                # JavaScript
├── tests/                 # Test files
├── manage.py              # Django management script
├── requirements.txt       # Python dependencies
└── .env.example           # Environment variables template
```

## Key Components

### Models
- **{{ PROJECT_NAME.title() }}Model**: Main application model
- **{{ PROJECT_NAME.title() }}Item**: Related items
- **User**: Custom user model with enhanced fields

### Views
- **Model CRUD**: Create, read, update, delete operations
- **API endpoints**: RESTful API with authentication
- **Authentication**: Login, logout, registration

### API Endpoints

<!-- API:START -->
<!-- API:DISABLED -->
- `GET /api/v1/{{ PROJECT_NAME.lower() }}-models/`: List models
- `POST /api/v1/{{ PROJECT_NAME.lower() }}-models/`: Create model
- `GET /api/v1/{{ PROJECT_NAME.lower() }}-models/{id}/`: Get model details
- `PUT /api/v1/{{ PROJECT_NAME.lower() }}-models/{id}/`: Update model
- `DELETE /api/v1/{{ PROJECT_NAME.lower() }}-models/{id}/`: Delete model
- `GET /api/v1/items/`: List items
- `POST /api/v1/items/`: Create item
- `POST /api/v1/token/`: Get JWT token
- `POST /api/v1/token/refresh/`: Refresh JWT token
<!-- API:END -->

## Development

### Running Tests
```bash
pytest
# or
python manage.py test
```

### Creating Migrations
```bash
python manage.py makemigrations
python manage.py migrate
```

### Accessing Admin Interface
Visit `http://127.0.0.1:8000/admin/` and login with superuser credentials.

### API Documentation
API endpoints follow REST conventions and return JSON responses with proper HTTP status codes.

## Deployment

### Environment Variables
Set the following environment variables in production:
- `DEBUG=False`
- `SECRET_KEY=your-secure-secret-key`
- `ALLOWED_HOSTS=yourdomain.com`
- Database connection details

### Database Migration
```bash
python manage.py migrate
```

### Collect Static Files
```bash
python manage.py collectstatic
```

### Web Server Configuration
Configure your web server (nginx, Apache) to serve static files and proxy to Django.

Built with ❤️ using the Python IDE
                """.trimIndent()
            )
        )
    }
    
    override fun createProjectStructure(projectDir: File) {
        // Create Django project structure
        val directories = listOf(
            "config",
            "core",
            "accounts",
            "templates",
            "templates/registration",
            "templates/accounts",
            "templates/core",
            "static",
            "static/css",
            "static/js",
            "static/images",
            "media",
            "tests",
            ".pythonide"
        )
        
        directories.forEach { dir ->
            File(projectDir, dir).mkdirs()
        }
        
        // Create __init__.py files for Python packages
        val initFiles = listOf(
            "config/__init__.py",
            "core/__init__.py",
            "accounts/__init__.py",
            "tests/__init__.py"
        )
        
        initFiles.forEach { file ->
            File(projectDir, file).createNewFile()
        }
    }
    
    override fun getCustomizationOptions(): TemplateCustomizationOptions {
        return TemplateCustomizationOptions(
            enableDatabase = true,
            enableAuthentication = true,
            enableApi = true,
            enableTests = true,
            enableDocumentation = false,
            databaseType = "postgresql",
            additionalDependencies = listOf(
                "django-environ==0.11.2",
                "django-debug-toolbar==4.2.0"
            )
        )
    }
}