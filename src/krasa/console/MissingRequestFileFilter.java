package krasa.console;

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

	public static final String PATTERN = "java.lang.IllegalArgumentException: No request defined for [";

	@Nullable
	@Override
	public Result applyFilter(final String line, int entireLength) {
		if (line == null) {
			return null;
		}
		if (line.substring(0, Math.min(line.length(), 150)).contains(PATTERN)) {
			final int path = line.indexOf(PATTERN);
			if (path > 0) {
				return new Result(entireLength - line.length(), entireLength, new HyperlinkInfoBase() {

					@Override
					public void navigate(@NotNull final Project project, @Nullable RelativePoint hyperlinkLocationPoint) {
						String substring = line.substring(path + PATTERN.length());
						final String path = substring.substring(0, substring.indexOf("]"));
						WriteCommandAction.runWriteCommandAction(project, new Runnable() {

							@Override
							public void run() {
								String baseFolderName = path.substring(0, path.indexOf("/"));
								String relativeFilePath = path.substring(path.indexOf("/") + 1);
								VirtualFile baseFolder = findDirectoryByName(project.getBaseDir(), baseFolderName);
								if (baseFolder != null) {
									PsiDirectory file = PsiManager.getInstance(project).findDirectory(baseFolder);
									assert file != null;
									String subdirectoryName = relativeFilePath.substring(0,
											relativeFilePath.lastIndexOf("/"));
									PsiDirectory subdirectory = file.findSubdirectory(subdirectoryName);
									if (subdirectory == null) {
										subdirectory = file.createSubdirectory(subdirectoryName);
									}
									String fileName = AutotestUtils.getFileName(relativeFilePath) + ".xml";
									PsiFile createdFile = subdirectory.findFile(fileName);
									if (createdFile == null) {
										createdFile = subdirectory.createFile(fileName);
									}
									VirtualFile createdFileVF = VirtualFileManagerEx.getInstance().refreshAndFindFileByUrl(
											createdFile.getVirtualFile().getUrl());
									new OpenFileDescriptor(project, createdFileVF).navigate(true);
								}
							}
						});
					}

				});
			}
		}
		return null;
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

}
