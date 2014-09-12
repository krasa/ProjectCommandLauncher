package krasa;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.*;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.*;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.tabs.PinToolwindowTabAction;

/**
 * Runs a process and prints the output in a content tab within the Run toolwindow.
 * 
 * @author yole
 */
public class AutotestContentExecutor implements Disposable {

	private final Project myProject;
	private final ProcessHandler myProcess;
	private final List<Filter> myFilterList = new ArrayList<Filter>();
	private Runnable myRerunAction;
	private Runnable myStopAction;
	private Runnable myAfterCompletion;
	private Computable<Boolean> myStopEnabled;
	private String myTitle = "Output";
	private String myHelpId = null;
	private boolean myActivateToolWindow = true;

	public AutotestContentExecutor(@NotNull Project project, @NotNull ProcessHandler process) {
		myProject = project;
		myProcess = process;
	}

	public AutotestContentExecutor withFilter(Filter filter) {
		myFilterList.add(filter);
		return this;
	}

	public AutotestContentExecutor withTitle(String title) {
		myTitle = title;
		return this;
	}

	public AutotestContentExecutor withRerun(Runnable rerun) {
		myRerunAction = rerun;
		return this;
	}

	public AutotestContentExecutor withStop(@NotNull Runnable stop, @NotNull Computable<Boolean> stopEnabled) {
		myStopAction = stop;
		myStopEnabled = stopEnabled;
		return this;
	}

	public AutotestContentExecutor withAfterCompletion(Runnable afterCompletion) {
		myAfterCompletion = afterCompletion;
		return this;
	}

	public AutotestContentExecutor withHelpId(String helpId) {
		myHelpId = helpId;
		return this;
	}

	public AutotestContentExecutor withActivateToolWindow(boolean activateToolWindow) {
		myActivateToolWindow = activateToolWindow;
		return this;
	}

	private ConsoleView createConsole(@NotNull Project project, @NotNull ProcessHandler processHandler) {
		TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
		consoleBuilder.filters(myFilterList);
		ConsoleView console = consoleBuilder.getConsole();
		console.attachToProcess(processHandler);
		return console;
	}

	public void run() {
		FileDocumentManager.getInstance().saveAllDocuments();

		ConsoleView view = createConsole(myProject, myProcess);

		if (myHelpId != null) {
			view.setHelpId(myHelpId);
		}
		Executor executor = DefaultRunExecutor.getRunExecutorInstance();
		DefaultActionGroup actions = new DefaultActionGroup();

		final JComponent consolePanel = createConsolePanel(view, actions);
		RunContentDescriptor descriptor = new RunContentDescriptor(view, myProcess, consolePanel, myTitle);

		Disposer.register(this, descriptor);

		actions.add(new RerunAction(consolePanel));
		actions.add(new StopAction());
		for (AnAction action : view.createConsoleActions()) {
			actions.add(action);
		}
		actions.add(PinToolwindowTabAction.getPinAction());
		actions.add(new CloseAction(executor, descriptor, myProject));

		ExecutionManager.getInstance(myProject).getContentManager().showRunContent(executor, descriptor);

		if (myActivateToolWindow) {
			activateToolWindow();
		}

		if (myAfterCompletion != null) {
			myProcess.addProcessListener(new ProcessAdapter() {

				@Override
				public void processTerminated(ProcessEvent event) {
					SwingUtilities.invokeLater(myAfterCompletion);
				}
			});
		}

		myProcess.startNotify();
	}

	public void activateToolWindow() {
		ApplicationManager.getApplication().invokeLater(new Runnable() {

			@Override
			public void run() {
				ToolWindowManager.getInstance(myProject).getToolWindow(DefaultRunExecutor.EXECUTOR_ID).activate(null);
			}
		});
	}

	private static JComponent createConsolePanel(ConsoleView view, ActionGroup actions) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(view.getComponent(), BorderLayout.CENTER);
		panel.add(createToolbar(actions), BorderLayout.WEST);
		return panel;
	}

	private static JComponent createToolbar(ActionGroup actions) {
		ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actions,
				false);
		return actionToolbar.getComponent();
	}

	@Override
	public void dispose() {
		Disposer.dispose(this);
	}

	private class RerunAction extends AnAction implements DumbAware {

		public RerunAction(JComponent consolePanel) {
			super("Rerun", "Rerun", AllIcons.Actions.Restart);
			registerCustomShortcutSet(CommonShortcuts.getRerun(), consolePanel);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			myRerunAction.run();
		}

		@Override
		public void update(AnActionEvent e) {
			e.getPresentation().setVisible(myRerunAction != null);
		}
	}

	private class StopAction extends AnAction implements DumbAware {

		public StopAction() {
			super("Stop", "Stop", AllIcons.Actions.Suspend);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			myStopAction.run();
		}

		@Override
		public void update(AnActionEvent e) {
			e.getPresentation().setVisible(myStopAction != null);
			e.getPresentation().setEnabled(myStopEnabled != null && myStopEnabled.compute());
		}
	}
}
