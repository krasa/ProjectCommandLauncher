package krasa;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Vojtech Krasa
 */
public class PortalShortcutStartupActivity implements StartupActivity {

	public static final String CLEAN_UTIL_PORTAL = "clean, util, portal";
	public static final Icon ICON = IconLoader.getIcon("/actions/uninstall.png");
	private volatile boolean registered = false;

	@Override
	public void runActivity(@NotNull Project project) {
		if (!registered) {
			registered = true;
			if (MatyasUtils.isMatyas()) {
				return;
			}

			initializeActions();
		}
	}

	private void initializeActions() {
		DefaultActionGroup mainToolBar = (DefaultActionGroup) ActionManager.getInstance().getAction("BuildMenu");
		mainToolBar.add(cleanUtilPortal());
		mainToolBar.add(payment());
		mainToolBar.add(portal());
		mainToolBar.add(all());
	}

	private DumbAwareAction all() {
		return new DumbAwareAction("all", "all", ICON) {

			public void actionPerformed(final AnActionEvent e) {
				final Process process = getProcess();
				RunContentExecutor executor = getRunContentExecutor(e.getProject(), process);
				executor.withAfterCompletion(new Runnable() {

					@Override
					public void run() {
						payment().actionPerformed(e);
						portal().actionPerformed(e);
					}
				});
				executor.run();
				run(e, process, "util.bat clean", "util.bat", "exit");
			}
		};
	}

	private DumbAwareAction portal() {
		return new DumbAwareAction("portal", "portal", IconLoader.getIcon("/actions/uninstall.png")) {

			public void actionPerformed(AnActionEvent e) {
				run(e, "portal-platform.bat", "exit ");
			}
		};
	}

	private DumbAwareAction payment() {
		return new DumbAwareAction("payment", "payment", IconLoader.getIcon("/actions/uninstall.png")) {

			public void actionPerformed(AnActionEvent e) {
				run(e, "payment.bat", "exit");
			}
		};
	}

	private DumbAwareAction cleanUtilPortal() {
		return new DumbAwareAction(CLEAN_UTIL_PORTAL, CLEAN_UTIL_PORTAL, IconLoader.getIcon("/actions/uninstall.png")) {

			public void actionPerformed(AnActionEvent e) {
				run(e, "util.bat clean", "util.bat", "portal-platform.bat", "exit");
			}
		};
	}

	private void run(AnActionEvent e, String... strings) {
		final Process process = createCmd(e.getProject());
		run(e, process, strings);
	}

	private void run(AnActionEvent e, Process process, String... strings) {
		String baseDir = getBaseDir(e);
		List<String> commands = new ArrayList<String>();
		commands.add(getDrive(baseDir));
		commands.add(getScriptsDir(baseDir));
		commands.addAll(Arrays.asList(strings));
		writeCommands(process, commands);
	}

	@NotNull
	private String getBaseDir(AnActionEvent e) {
		Project project = e.getProject();
		Module[] modules = ModuleManager.getInstance(project).getModules();
		for (Module module : modules) {
			if (module.getName().toUpperCase().startsWith("PORTAL")) {
				VirtualFile moduleFile = module.getModuleFile();
				VirtualFile baseDir = moduleFile.getParent();
				return baseDir.getPath();
			}
		}
		throw new RuntimeException("portal module not found");
	}

	private String getDrive(String path) {
		return path.substring(0, 1) + ":";
	}

	private Process createCmd(Project project) {
		final Process process = getProcess();
		RunContentExecutor executor = getRunContentExecutor(project, process);
		executor.run();
		return process;
	}

	private Process getProcess() {
		try {
			GeneralCommandLine generalCommandLine = new GeneralCommandLine("cmd");
			return generalCommandLine.createProcess();
		} catch (ExecutionException e1) {
			throw new RuntimeException(e1);
		}
	}

	private RunContentExecutor getRunContentExecutor(Project project, Process process) {
		OSProcessHandler osProcessHandler = new OSProcessHandler(process, "");
		return new RunContentExecutor(project, osProcessHandler);
	}

	private void writeCommands(Process process, List<String> strings) {
		PrintWriter printWriter = new PrintWriter(process.getOutputStream());

		for (String string : strings) {
			printWriter.println(string);

		}
		printWriter.flush();
	}

	private String getScriptsDir(String path) {
		return "cd " + path.replaceAll("[/]", "\\\\") + "\\sdp\\scripts";
	}

}
