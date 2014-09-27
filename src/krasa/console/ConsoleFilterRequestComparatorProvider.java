package krasa.console;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.filters.*;
import com.intellij.notification.*;
import com.intellij.openapi.project.Project;

public class ConsoleFilterRequestComparatorProvider implements ConsoleFilterProvider {

	final static NotificationGroup notificationGroup = new NotificationGroup("ConsoleFilterRequestComparatorProvider",
			NotificationDisplayType.BALLOON, false);

	@NotNull
	@Override
	public Filter[] getDefaultFilters(@NotNull Project project) {
		return new Filter[] { new RequestComparatorFilter(), new MissingRequestFileFilter() };
	}

}
