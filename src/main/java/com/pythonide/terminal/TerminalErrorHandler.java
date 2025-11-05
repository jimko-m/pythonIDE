package com.pythonide.terminal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TerminalErrorHandler {
    // (الحقول وبنّاء الكلاس الأصلي يبقون كما هم إن وُجدوا)

    public String getErrorStatistics() {
        if (errorHistory.isEmpty()) {
            return "لا توجد أخطاء مسجلة";
        }

        java.util.Map<String, Integer> categoryStats = new java.util.HashMap<>();
        for (ErrorEntry error : errorHistory) {
            categoryStats.put(error.category,
                categoryStats.getOrDefault(error.category, 0) + 1);
        }

        StringBuilder stats = new StringBuilder();
        stats.append("إحصائيات الأخطاء:\n");
        stats.append("إجمالي الأخطاء: ").append(errorHistory.size()).append("\n");

        for (java.util.Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
            stats.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        ErrorEntry lastError = errorHistory.get(errorHistory.size() - 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String lastErrorTime = sdf.format(new Date(lastError.timestamp));
        stats.append("آخر خطأ: ").append(lastErrorTime).append("\n");

        return stats.toString();
    }

    public static class ErrorEntry {
        public long timestamp;
        public String category;
        public String message;
        public String stackTrace;

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static class ErrorAnalyzer {

        public static String analyzeError(String errorMessage) {
            if (errorMessage == null || errorMessage.isEmpty()) {
                return "لا توجد رسالة خطأ لتحليلها.";
            }

            StringBuilder analysis = new StringBuilder();
            analysis.append("تحليل رسالة الخطأ:\n");

            if (errorMessage.contains("No module named") || errorMessage.contains("ModuleNotFoundError")) {
                analysis.append("- الوحدة غير متوفرة، تحقق من تثبيت الحزمة\n");
            }
            if (errorMessage.contains("NameError")) {
                analysis.append("- متغير غير محدد أو خطأ في اسم المتغير\n");
            }
            if (errorMessage.contains("SyntaxError")) {
                analysis.append("- خطأ في تركيب الكود، تحقق من الصيغة والنحو\n");
            }
            if (errorMessage.contains("IndentationError")) {
                analysis.append("- خطأ في المسافات البادئة (Indentation). تأكد من ثبات المسافات/التابات\n");
            }
            if (errorMessage.contains("ImportError")) {
                analysis.append("- مشكلة في استيراد وحدة\n");
            }
            if (errorMessage.contains("FileNotFoundError")) {
                analysis.append("- ملف غير موجود\n");
            }
            if (errorMessage.contains("PermissionError") || errorMessage.contains("Permission denied")) {
                analysis.append("- مشكلة في الصلاحيات\n");
            }
            if (errorMessage.contains("UnicodeError")) {
                analysis.append("- مشكلة في ترميز النصوص\n");
            }
            if (errorMessage.contains("MemoryError")) {
                analysis.append("- نفاد الذاكرة\n");
            }
            if (errorMessage.contains("SystemError")) {
                analysis.append("- خطأ في النظام\n");
            }

            if (errorMessage.contains("SecurityException")) {
                analysis.append("- مشكلة أمنية، تحقق من الصلاحيات\n");
            }
            if (errorMessage.contains("NetworkOnMainThreadException")) {
                analysis.append("- استخدم Thread منفصل للشبكة\n");
            }

            analysis.append("\nاقتراحات:\n");
            analysis.append("1. تحقق من صحة الكود ومواقع السطور المذكورة في الخطأ\n");
            analysis.append("2. تأكد من تثبيت الحزم المطلوبة أو توفر المكتبات\n");
            analysis.append("3. راجع السجلات التفصيلية لتحديد السياق\n");

            return analysis.toString();
        }

        public static List<String> suggestSolutions(String errorMessage) {
            List<String> solutions = new ArrayList<>();
            if (errorMessage == null || errorMessage.isEmpty()) {
                solutions.add("لا توجد معلومات خطأ كافية لاقتراح حلول.");
                return solutions;
            }

            if (errorMessage.contains("No module named") || errorMessage.contains("ModuleNotFoundError")) {
                solutions.add("قم بتثبيت الحزمة المطلوبة: pip install <package>");
                solutions.add("تحقق من اسم الحزمة ومسار البيئة (virtualenv/venv).");
            } else if (errorMessage.contains("NameError")) {
                solutions.add("تأكد من تعريف المتغير قبل استخدامه أو تأكد من تهجئة الاسم بشكل صحيح.");
            } else if (errorMessage.contains("SyntaxError")) {
                solutions.add("افحص السطر المذكور وأضف عناصر التركيب المفقودة (مثل النقطتين أو الأقواس أو علامات الاقتباس).");
            } else if (errorMessage.contains("IndentationError")) {
                solutions.add("راجع المسافات البادئة. استخدم مسافات ثابتة (مثلاً 4) أو التابات بشكل متسق.");
            } else if (errorMessage.contains("Connection refused")) {
                solutions.add("تأكد من تشغيل الخدمة المستهدفة وأن رقم المنفذ صحيح.");
                solutions.add("تحقق من جدار الحماية وصلاحيات الوصول.");
            } else if (errorMessage.contains("Permission denied")) {
                solutions.add("تأكد من صلاحيات الكتابة/القراءة للمجلد أو الملف.");
            } else {
                solutions.add("راجع رسائل السجل التفصيلية لمعرفة أسباب إضافية.");
                solutions.add("حاول تشغيل الأمر في وضع تفصيلي (verbose) للحصول على مزيد من المعلومات.");
            }

            return solutions;
        }
    }
}