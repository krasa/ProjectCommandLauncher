package krasa.toolwindow;

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;

import javax.swing.*;

import krasa.actions.RunAutotestInIntelliJ;
import krasa.model.*;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;

public class AutotestPanel implements Disposable {

	private final Runnable settingsChangedListener;
	private JList list;
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
				for (Object o : list.getSelectedValues()) {
					TestFile o1 = (TestFile) o;
					AutotestState.getInstance().removeTestFileHistory(o1);
				}
			}

		}
	}

	private class EnterListener extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent evt) {
			final int keyCode = evt.getKeyCode();
			if (keyCode == KeyEvent.VK_ENTER) {
				for (Object o : list.getSelectedValues()) {
					TestFile o1 = (TestFile) o;
					new RunAutotestInIntelliJ().runInIDEA(project, o1);
				}
			}
		}
	}

	private class DoubleClickListener extends MouseAdapter {

		public void mouseClicked(MouseEvent evt) {
			JList list = (JList) evt.getSource();
			if (evt.getClickCount() == 2) {
				int index = list.locationToIndex(evt.getPoint());
				TestFile o = (TestFile) model.get(index);
				new RunAutotestInIntelliJ().runInIDEA(project, o);
			}
		}
	}
}
