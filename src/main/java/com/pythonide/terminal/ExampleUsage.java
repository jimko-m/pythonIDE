package com.pythonide.terminal;

import android.app.Activity;
import android.widget.Toast;
import java.util.List;

public class ExampleUsage extends Activity {
    // ... other members if needed

    private void demonstrateErrorHandling() {
        TerminalErrorHandler errorHandler = new TerminalErrorHandler(this);

        // تسجيل خطأ عادي
        try {
            // محاكاة خطأ
            throw new RuntimeException("خطأ اختبار في النهائية");
        } catch (Exception e) {
            errorHandler.logError("EXECUTION", "خطأ في تنفيذ أمر Python", e);
        }

        // تسجيل تحذير
        errorHandler.logWarning("استهلاك عالي للذاكرة");

        // تسجيل معلومات
        errorHandler.logInfo("تم بدء Terminal بنجاح");

        // الحصول على آخر الأخطاء
        TerminalErrorHandler.ErrorEntry[] recentErrors =
            errorHandler.getRecentErrors(5).toArray(new TerminalErrorHandler.ErrorEntry[0]);

        // تحليل الخطأ
        String analysis = TerminalErrorHandler.ErrorAnalyzer.analyzeError(
            "NameError: name 'variable' is not defined"
        );
        showToast("تحليل الخطأ: " + analysis);

        // اقتراح حلول
        List<String> solutions = TerminalErrorHandler.ErrorAnalyzer.suggestSolutions(
            "NameError: name 'variable' is not defined"
        );

        // عرض الحلول (كمثال)
        for (String s : solutions) {
            showToast("اقتراح: " + s);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}