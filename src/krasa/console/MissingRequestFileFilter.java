package krasa.console;

import java.io.IOException;

import krasa.actions.AutotestUtils;

import org.jetbrains.annotations.*;

import com.intellij.execution.filters.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import com.intellij.psi.*;
import com.intellij.ui.awt.RelativePoint;

public class MissingRequestFileFilter implements Filter {

	public static final String PATTERN = "No request defined for [";
	Data data;

	@Nullable
	@Override
	public Result applyFilter(final String line, int entireLength) {
		if (line == null) {
			return null;
		}
		if (line.substring(0, Math.min(line.length(), 120)).contains("DummyRequestService")) {
			int i = line.indexOf("Actual: ");
			if (i > 0) {
				String substring = line.substring(i + "Actual: ".length());
				final Data data1 = new Data(substring.trim());
				data = data1;
				data.hyperlinkInfoBase = getHyperlinkInfoBase(data1);
				return new Result(entireLength - line.length(), entireLength, data.hyperlinkInfoBase);
			}
			if (line.substring(0, Math.min(line.length(), 160)).contains(PATTERN)) {
				final int i2 = line.indexOf(PATTERN);
				if (i2 > 0) {
					String substring = line.substring(i2 + PATTERN.length());
					final String path = substring.substring(0, substring.indexOf("]"));
					final String baseFolderName = path.substring(0, path.indexOf("/"));
					final String relativeFilePath = path.substring(path.indexOf("/") + 1);
					final String subdirectoryName = relativeFilePath.substring(0, relativeFilePath.lastIndexOf("/"));
					if (data == null || data.baseFolderName != null) {
						data = new Data(null);
						data.hyperlinkInfoBase = getHyperlinkInfoBase(data);
					}
					data.baseFolderName = baseFolderName;
					data.relativeFilePath = relativeFilePath;
					data.subdirectoryName = subdirectoryName;

					return new Result(entireLength - line.length(), entireLength, data.hyperlinkInfoBase);
				}
			}

		}
		return null;
	}

	private HyperlinkInfoBase getHyperlinkInfoBase(final Data data1) {
		return new HyperlinkInfoBase() {

			@Override
			public void navigate(@NotNull final Project project,
					@Nullable RelativePoint hyperlinkLocationPoint) {
				createFile(project, data1);
			}

		};
	}

	private void createFile(final Project project, final Data data) {
		WriteCommandAction.runWriteCommandAction(project, new Runnable() {

			@Override
			public void run() {

				VirtualFile baseFolder = findDirectoryByName(project.getBaseDir(), data.baseFolderName);
				if (baseFolder != null) {
					PsiDirectory file = PsiManager.getInstance(project).findDirectory(baseFolder);
					assert file != null;

					PsiDirectory subdirectory = file.findSubdirectory(data.subdirectoryName);
					if (subdirectory == null) {
						subdirectory = file.createSubdirectory(data.subdirectoryName);
					}
					String fileName = AutotestUtils.getFileName(data.relativeFilePath) + ".xml";
					PsiFile createdFile = subdirectory.findFile(fileName);
					if (createdFile == null) {
						createdFile = subdirectory.createFile(fileName);
					}
					VirtualFile createdFileVF = VirtualFileManagerEx.getInstance().refreshAndFindFileByUrl(
							createdFile.getVirtualFile().getUrl());
					try {
						String content = data.content;
						if (content != null) {
							assert createdFileVF != null;
							String reformat = FormatUtils.reformat(project, content);
							createdFileVF.setBinaryContent(reformat.getBytes("UTF-8"));
						}
					} catch (IOException e) {
						throw new RuntimeException(e.getMessage(), e);
					}

					new OpenFileDescriptor(project, createdFileVF).navigate(true);
				}
			}
		});
	}

	private VirtualFile findDirectoryByName(VirtualFile virtualFile, final String directoryName) {
		VirtualFileVisitor visitor = new VirtualFileVisitor() {

			@NotNull
			@Override
			public Result visitFileEx(@NotNull VirtualFile file) {
				if (file.isDirectory() && file.getName().equals(directoryName)) {
					throw new FolderFound(file);
				}
				return CONTINUE;
			}
		};
		try {
			VfsUtilCore.visitChildrenRecursively(virtualFile, visitor);
		} catch (FolderFound e) {
			return e.file;
		}
		return null;
	}

	private class FolderFound extends RuntimeException {

		private VirtualFile file;

		public FolderFound(VirtualFile file) {
			this.file = file;
		}
	}

	private static class Data {

		String path;
		HyperlinkInfoBase hyperlinkInfoBase;
		String baseFolderName;
		String subdirectoryName;
		String content;
		String relativeFilePath;

		Data(String content) {

			this.content = content;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Data{");
			sb.append("path='").append(path).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}
}
