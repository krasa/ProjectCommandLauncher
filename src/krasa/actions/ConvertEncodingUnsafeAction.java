package krasa.actions;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.GlobalUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.ChooseFileEncodingAction;
import com.intellij.openapi.vfs.encoding.EncodingUtil;
import com.intellij.util.Function;

public class ConvertEncodingUnsafeAction extends DumbAwareAction {

	boolean allowDirectories = false;

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		final VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		if (virtualFile == null)
			return;
		final Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
		FileDocumentManager documentManager = FileDocumentManager.getInstance();
		final Document document = documentManager.getDocument(virtualFile);
		if (!allowDirectories && virtualFile.isDirectory() || document == null && !virtualFile.isDirectory())
			return;

		final byte[] bytes;
		try {
			bytes = virtualFile.isDirectory() ? null : virtualFile.contentsToByteArray();
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}

		DefaultActionGroup aNull = new ChooseFileEncodingAction(virtualFile) {

			@Override
			public void update(AnActionEvent e) {

			}

			@NotNull
			@Override
			protected DefaultActionGroup createPopupActionGroup(JComponent button) {
				return createCharsetsActionGroup(null, null, new Function<Charset, String>() {

					@Override
					public String fun(Charset charset) {
						return "Change encoding to '" + charset.displayName() + "'";
					}
				});
			}

			@Override
			protected void chosen(@Nullable VirtualFile virtualFile, @NotNull Charset charset) {
				ConvertEncodingUnsafeAction.chosen(document, editor, virtualFile, bytes, charset);

			}
		}.createPopupActionGroup(null);

		ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(getTemplatePresentation().getText(),
				aNull, e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
		if (popup != null) {
			popup.showInBestPositionFor(e.getDataContext());
		}

	}

	// returns true if charset was changed, false if failed
	protected static boolean chosen(final Document document, final Editor editor,
			@NotNull final VirtualFile virtualFile, byte[] bytes, @NotNull final Charset charset) {

		final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
		final Charset oldCharset = virtualFile.getCharset();
		final Runnable undo;
		final Runnable redo;
		// change and forget
		undo = new Runnable() {

			@Override
			public void run() {
				EncodingUtil.saveIn(document, editor, virtualFile, oldCharset);
			}
		};
		redo = new Runnable() {

			@Override
			public void run() {
				EncodingUtil.saveIn(document, editor, virtualFile, charset);
			}
		};
		final UndoableAction action = new GlobalUndoableAction(virtualFile) {

			@Override
			public void undo() {
				// invoke later because changing document inside undo/redo is not allowed
				Application application = ApplicationManager.getApplication();
				application.invokeLater(undo, ModalityState.NON_MODAL,
						(project == null ? application : project).getDisposed());
			}

			@Override
			public void redo() {
				// invoke later because changing document inside undo/redo is not allowed
				Application application = ApplicationManager.getApplication();
				application.invokeLater(redo, ModalityState.NON_MODAL,
						(project == null ? application : project).getDisposed());
			}
		};

		redo.run();
		CommandProcessor.getInstance().executeCommand(project, new Runnable() {

			@Override
			public void run() {
				UndoManager undoManager = project == null ? UndoManager.getGlobalInstance()
						: UndoManager.getInstance(project);
				undoManager.undoableActionPerformed(action);
			}
		}, "Change encoding for '" + virtualFile.getName() + "'", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);

		return true;
	}

}
