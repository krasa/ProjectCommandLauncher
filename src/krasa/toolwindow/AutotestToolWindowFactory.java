package krasa.toolwindow;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.module.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;

public class AutotestToolWindowFactory implements ToolWindowFactoryEx, DumbAware {

	@Override
	public void init(ToolWindow window) {
	}

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		Module[] modules = ModuleManager.getInstance(project).getModules();
		for (Module module : modules) {
			if (StringUtils.containsIgnoreCase(module.getName(), "autotest")) {
				AutotestExplorer explorer = new AutotestExplorer(project);
				final ContentManager contentManager = toolWindow.getContentManager();
				final Content content = contentManager.getFactory().createContent(explorer, null, false);
				contentManager.addContent(content);
				Disposer.register(project, explorer);
			}
		}
	}
}
