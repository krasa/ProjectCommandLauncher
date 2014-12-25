package krasa.console;

import org.apache.commons.lang.ClassUtils;

import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

public class FormatUtils {

	private static final Logger LOG = Logger.getInstance(FormatUtils.class);

	static String reformat(final Project project, final String actual) {
		PsiFile actualPsiFile = null;
		if (isJson(actual)) {
			Language instance = null;
			try {
				instance = (Language) ClassUtils.getClass("com.intellij.json.JsonLanguage").getField("INSTANCE").get(
						null);

				actualPsiFile = reformat(project, actual, instance);
			} catch (Throwable e) {
				LOG.info(e);
			}
		} else {
			actualPsiFile = reformat(project, actual, XMLLanguage.INSTANCE);
		}

		if (actualPsiFile == null) {
			return actual;
		}
		return actualPsiFile.getText();
	}

	private static PsiFile reformat(final Project project, final String actual, Language instance) {
		final PsiFile actualPsiFile;
		actualPsiFile = PsiFileFactory.getInstance(project).createFileFromText("temp", instance, actual);
		WriteCommandAction.runWriteCommandAction(project, new Runnable() {

			@Override
			public void run() {
				try {
					CodeStyleManager.getInstance(project).reformatRange(actualPsiFile, 0, actual.length());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		return actualPsiFile;
	}

	private static boolean isJson(String actual) {
		return actual.startsWith("{");
	}
}
