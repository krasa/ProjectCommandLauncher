package krasa.console;

import krasa.actions.AutotestUtils;

import org.jetbrains.annotations.*;

import com.intellij.execution.filters.*;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.*;
import com.intellij.openapi.diff.ex.DiffContentFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;

public class ResponseComparatorFilter implements Filter {

	public static final String prefix = "Response does not match file: ";

	Data data;

	@Nullable
	@Override
	public Result applyFilter(String line, int entireLength) {
		if (line == null) {
			return null;
		}
		if (line.substring(0, Math.min(line.length(), 100)).contains("AssertEqualResponseBuilder")) {
			int path = line.indexOf(prefix);
			int actual = actualIndex(line);
			if (path > 0) {
				data = new Data();
				data.path = line.substring(path + prefix.length()).trim();
				return new Result(entireLength - line.length() + path, entireLength, new GoToFile(data.path));
			} else if (actual > 0 && data != null) {
				data.actual = line.substring(actual + "Actual: ".length()).trim();
				Result result = new Result(entireLength - line.length(), entireLength, new DiffFile(data));
				data = null;
				return result;
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

	private class GoToFile implements HyperlinkInfo {

		private String path;

		public GoToFile(String path) {
			this.path = path;
		}

		@Override
		public void navigate(Project project) {
			new OpenFileDescriptor(project, findFile(project, path)).navigate(true);
		}
	}

	private class DiffFile extends HyperlinkInfoBase {

		private Data data;

		public DiffFile(Data data) {
			this.data = data;
		}

		@Override
		public void navigate(@NotNull final Project project,
				@Nullable RelativePoint hyperlinkLocationPoint) {
			VirtualFile expected = findFile(project, data.path);

			if (expected == null) {
				ConsoleFilterProvider.notificationGroup.createNotification(
						"File not found " + data.path, MessageType.ERROR).notify(project);
				return;
			}

			String reformat = FormatUtils.reformat(project, data.actual);

			VirtualFile actualVF = new LightVirtualFile("Actual", XMLLanguage.INSTANCE, reformat);

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

	}

	private VirtualFile findFile(Project project, String path1) {
		VirtualFile file = null;
		PsiShortNamesCache instance = PsiShortNamesCache.getInstance(project);
		String fileName = AutotestUtils.getFileName(path1);
		PsiFile[] filesByName = getFilesByName(instance, fileName);
		if (!path1.startsWith("/")) {
			path1 = "/" + path1;
		}
		for (PsiFile psiFile : filesByName) {
			VirtualFile virtualFile = psiFile.getVirtualFile();
			String path = virtualFile.getPath();
			if (path.contains(path1)) {
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

	private static class Data {

		String path;
		String actual;

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
