package krasa.toolwindow;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import krasa.actions.DialogUtils;
import krasa.actions.RunAutotestInIntelliJ;
import krasa.model.AutotestState;
import krasa.model.TestFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBList;

public class AutotestPanel implements Disposable {

	private final Runnable settingsChangedListener;
	private JBList list;
	private JPanel root;
	private DefaultListModel model;
	private Project project;

	public JPanel getRoot() {
		return root;
	}

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
							return new AnAction[] { new RunAction(), new RunOnAction(), new DeleteAction() };
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

	private class DeleteListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent evt) {
			final int keyCode = evt.getKeyCode();
			if (keyCode == KeyEvent.VK_DELETE) {
				delete();
			}
		}

	}

	private void delete() {
		for (Object o : list.getSelectedValues()) {
			TestFile o1 = (TestFile) o;
			AutotestState.getInstance().removeTestFileHistory(o1);
		}
	}

	private class EnterListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent evt) {
			final int keyCode = evt.getKeyCode();
			if (keyCode == KeyEvent.VK_ENTER) {
				run();
			}
		}
	}

	private void run() {
		for (Object o : list.getSelectedValues()) {
			TestFile o1 = (TestFile) o;
			new RunAutotestInIntelliJ().runInIDEA(project, o1);
		}
	}

	private class DoubleClickListener extends MouseAdapter {

		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2) {
				TestFile o = getTestFileAtPoint(evt.getPoint());
				new RunAutotestInIntelliJ().runInIDEA(project, o);
			}
		}
	}

	private TestFile getTestFileAtPoint(Point point) {
		int index = list.locationToIndex(point);
		return (TestFile) model.get(index);
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
					TestFile element = new TestFile(environment, o1.getName(), o1.getPath());
					if (!testFileList.contains(element)) {
						testFileList.add(element);
						AutotestState.getInstance().addTestFile(element);
						new RunAutotestInIntelliJ().runInIDEA(project, element);
					}
				}

			}
		}
	}
}
