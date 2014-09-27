package krasa.console;

import krasa.actions.AutotestUtils;

import org.jetbrains.annotations.*;

import com.intellij.execution.filters.*;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diff.*;
import com.intellij.openapi.diff.ex.DiffContentFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;

public class RequestComparatorFilter implements Filter {

	public static final String REQUEST_DOES_NOT_MATCH_EXPECTED_CONTENT_FOR = "Request does not match expected content for";

	Data data;

	@Nullable
	@Override
	public Result applyFilter(String line, int entireLength) {
		if (line == null) {
			return null;
		}
		if (line.substring(0, Math.min(line.length(), 100)).contains("RequestComparator")) {
			int path = line.indexOf(REQUEST_DOES_NOT_MATCH_EXPECTED_CONTENT_FOR);
			int actual = actualIndex(line);
			if (path > 0) {
				data = new Data();
				data.path = line.substring(
						path
								+ REQUEST_DOES_NOT_MATCH_EXPECTED_CONTENT_FOR.length()).trim();
				data.hyperlinkInfoBase = new MyHyperlinkInfoBase(data);
			} else if (actual > 0) {
				String substring = line.substring(actual + "Actual: ".length());
				data.actual = substring.trim();
				Result result = new Result(entireLength - line.length(), entireLength, data.hyperlinkInfoBase);
				data = null;
				return result;
			}
			if (data != null && data.hyperlinkInfoBase != null) {
				return new Result(entireLength - line.length(), entireLength, data.hyperlinkInfoBase);
			}
		}
		return null;
	}

	private int actualIndex(String line) {
		int i = line.indexOf("Actual: ");
		if (i <= 0) {
			i = line.indexOf("actual: ");
		}
		return i;
	}

	private static class MyHyperlinkInfoBase extends HyperlinkInfoBase {

		private Data data;

		public MyHyperlinkInfoBase(Data data) {
			this.data = data;
		}

		@Override
		public void navigate(@NotNull final Project project,
				@Nullable RelativePoint hyperlinkLocationPoint) {
			VirtualFile expected = findFile(project);

			if (expected == null) {
				ConsoleFilterRequestComparatorProvider.notificationGroup.createNotification(
						"File not found " + data.path, MessageType.ERROR).notify(project);
				return;
			}

			final PsiFile actualPsiFile = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE,
					data.actual);
			WriteCommandAction.runWriteCommandAction(project, new Runnable() {

				@Override
				public void run() {
					try {
						CodeStyleManager.getInstance(project).reformatRange(actualPsiFile, 0, data.actual.length());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			String text = actualPsiFile.getText();
			VirtualFile actualVF = new LightVirtualFile("Actual", XMLLanguage.INSTANCE, text);

			SimpleDiffRequest diffData = DiffContentFactory.compareVirtualFiles(project, actualVF, expected, data.path);
			// noinspection ConstantConditions
			diffData.setContentTitles("Actual", expected.getPath());
			final DiffContent[] contents = diffData.getContents();
			ApplicationManager.getApplication().runWriteAction(new Runnable() {

				public void run() {
					for (DiffContent content : contents) {
						Document document = content.getDocument();
						if (document != null) {
							FileDocumentManager.getInstance().saveDocument(document);
						}
					}
				}
			});
			DiffManager.getInstance().getDiffTool().show(diffData);
		}

		private VirtualFile findFile(Project project) {
			VirtualFile file = null;
			PsiShortNamesCache instance = PsiShortNamesCache.getInstance(project);
			String fileName = AutotestUtils.getFileName(data.path);
			PsiFile[] filesByName = getFilesByName(instance, fileName);
			for (PsiFile psiFile : filesByName) {
				VirtualFile virtualFile = psiFile.getVirtualFile();
				String path = virtualFile.getPath();
				if (path.contains(data.path) && path.contains("dummy-requests")) {
					file = virtualFile;
				}
			}
			return file;
		}

		private PsiFile[] getFilesByName(PsiShortNamesCache instance, String fileName) {
			PsiFile[] filesByName = instance.getFilesByName(fileName);
			if (filesByName.length == 0) {
				filesByName = instance.getFilesByName(fileName + ".xml");
			}
			if (filesByName.length == 0) {
				filesByName = instance.getFilesByName(fileName + ".json");
			}
			return filesByName;
		}
	}

	private static class Data {

		String path;
		String actual;
		HyperlinkInfoBase hyperlinkInfoBase;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Data{");
			sb.append("path='").append(path).append('\'');
			sb.append(", actual='").append(actual).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}

}
