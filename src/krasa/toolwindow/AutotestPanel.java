package krasa.toolwindow;

import static krasa.actions.AutotestUtils.getTestFileRelativePath;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import krasa.actions.*;
import krasa.model.*;

import org.jetbrains.annotations.*;

import com.intellij.notification.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBList;

public class AutotestPanel implements Disposable {

	private final Runnable settingsChangedListener;
	private JBList list;
	private JPanel root;
	private DefaultListModel model;
	private Project project;

	public AutotestPanel(Project project) {
		this.project = project;
		initializeModel();
		settingsChangedListener = new Runnable() {

			@Override
			public void run() {
				initializeModel();
			}
		};
		AutotestState.getInstance().addListener(settingsChangedListener);
	}

	public JPanel getRoot() {
		return root;
	}

	private void createUIComponents() {
		model = new DefaultListModel();
		list = createJBList(model);
		list.addMouseListener(new PopupHandler() {

			@Override
			public void invokePopup(Component comp, int x, int y) {
				int index = list.locationToIndex(new Point(x, y));
				if (index >= 0) {
					if (!list.isSelectedIndex(index)) {
						list.setSelectedIndex(index);
					}
					ActionManager.getInstance().createActionPopupMenu("", new ActionGroup() {

						@NotNull
						@Override
						public AnAction[] getChildren(@Nullable AnActionEvent e) {
							return new AnAction[] { new JumpToSourceAction(), new RunAction(), new RunOnAction(),
									new DebugAction(), new DeleteAction() };
						}
					}).getComponent().show(comp, x, y);
				}
			}
		});
	}

	private JBList createJBList(DefaultListModel pluginsModel) {
		JBList jbList = new JBList(pluginsModel);
		jbList.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				TestFile goal = (TestFile) value;
				setText(goal.getEnviroment().replace("prgen", "p") + " - " + goal.getName());
				return comp;
			}
		});

		jbList.addMouseListener(new DoubleClickListener());
		jbList.addKeyListener(new EnterListener());
		jbList.addKeyListener(new DeleteListener());
		jbList.addKeyListener(new JumpToSourceListener());

		return jbList;
	}

	private void initializeModel() {
		model.clear();
		Collection<TestFile> testFileHistory = AutotestState.getInstance().getTestFileHistory();
		for (TestFile testFile : testFileHistory) {
			model.addElement(testFile);
		}
	}

	@Override
	public void dispose() {
		AutotestState.getInstance().removeListener(settingsChangedListener);
	}

	private void jumpToSource() {
		TestFile selectedValue = (TestFile) list.getSelectedValue();
		if (selectedValue != null) {
			String path = selectedValue.getPath();
			PsiFile[] filesByName = PsiShortNamesCache.getInstance(project).getFilesByName(selectedValue.getName());
			for (PsiFile psiFile : filesByName) {
				if (getTestFileRelativePath(psiFile.getVirtualFile()).equals(path)) {
					OpenFileDescriptor n = new OpenFileDescriptor(project, psiFile.getVirtualFile(), 0).setUseCurrentWindow(true);
					if (n.canNavigate()) {
						n.navigate(true);
						return;
					}
				}
			}
			final Notification notification = new Notification("Autotest ERROR", "", "File not found "
					+ selectedValue.getPath(), NotificationType.ERROR);
			ApplicationManager.getApplication().invokeLater(new Runnable() {

				@Override
				public void run() {
					Notifications.Bus.notify(notification, project);
				}
			});
		}
	}

	private void delete() {
		for (Object o : list.getSelectedValues()) {
			TestFile o1 = (TestFile) o;
			AutotestState.getInstance().removeTestFileHistory(o1);
		}
	}

	private void run() {
		for (Object o : list.getSelectedValues()) {
			TestFile o1 = (TestFile) o;
			new RunAutotestInIntelliJ().runInIDEA(project, o1);
		}
	}

	private void debug() {
		for (Object o : list.getSelectedValues()) {
			TestFile o1 = (TestFile) o;
			new DebugAutotestInIntelliJ().debugInIDEA(project, o1);
		}
	}

	private TestFile getTestFileAtPoint(Point point) {
		int index = list.locationToIndex(point);
		return (TestFile) model.get(index);
	}

	private class JumpToSourceListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			final int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_F4) {
				jumpToSource();
			}
		}
	}

	private class DeleteListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			final int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_DELETE) {
				delete();
			}
		}

	}

	private class EnterListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			final int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_ENTER) {
				run();
			}
		}
	}

	private class DoubleClickListener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				TestFile o = getTestFileAtPoint(e.getPoint());
				new RunAutotestInIntelliJ().runInIDEA(project, o);
			}
		}
	}

	private class DeleteAction extends DumbAwareAction {

		public DeleteAction() {
			super("Delete");
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			delete();
		}
	}

	private class RunAction extends DumbAwareAction {

		public RunAction() {
			super("Run");
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			run();
		}
	}

	private class DebugAction extends DumbAwareAction {

		public DebugAction() {
			super("Debug");
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			debug();
		}
	}

	private class RunOnAction extends DumbAwareAction {

		public RunOnAction() {
			super("Run on...");
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			String environment = DialogUtils.chooseEnvironment();
			if (environment != null) {
				Set<TestFile> testFileList = new HashSet<TestFile>();
				for (Object o : list.getSelectedValues()) {
					TestFile o1 = (TestFile) o;
					TestFile element = new TestFile(environment, o1.getName(), o1.getPath(), o1.getFullPath());
					if (!testFileList.contains(element)) {
						testFileList.add(element);
						AutotestState.getInstance().addTestFile(element);
						new RunAutotestInIntelliJ().runInIDEA(project, element);
					}
				}

			}
		}
	}

	private class JumpToSourceAction extends DumbAwareAction {

		public JumpToSourceAction() {
			super("Jump to source (F4)");
		}

		@Override
		public void actionPerformed(@NotNull AnActionEvent e) {
			jumpToSource();
		}

		@Override
		public void update(@NotNull AnActionEvent e) {
			super.update(e);
			e.getPresentation().setVisible(list.getSelectedValues().length == 1);
		}
	}
}
